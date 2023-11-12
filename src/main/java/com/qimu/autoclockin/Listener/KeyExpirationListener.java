package com.qimu.autoclockin.Listener;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.config.EmailConfig;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.model.dto.IpPool.IpPoolClient;
import com.qimu.autoclockin.model.dto.dingTalk.DingTalkPushClient;
import com.qimu.autoclockin.model.dto.tencentmap.TencentMapClient;
import com.qimu.autoclockin.model.entity.ClockInInfo;
import com.qimu.autoclockin.model.entity.DailyCheckIn;
import com.qimu.autoclockin.model.entity.User;
import com.qimu.autoclockin.model.enums.ClockInStatusEnum;
import com.qimu.autoclockin.model.vo.ClockInInfoVo;
import com.qimu.autoclockin.model.vo.ClockInStatus;
import com.qimu.autoclockin.service.ClockInInfoService;
import com.qimu.autoclockin.service.DailyCheckInService;
import com.qimu.autoclockin.service.UserService;
import com.qimu.autoclockin.utils.AutoSignUtils;
import com.qimu.autoclockin.utils.RedissonLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import static com.qimu.autoclockin.constant.ClockInConstant.SIGN_USER_GROUP;
import static com.qimu.autoclockin.constant.EmailConstant.EMAIL_TITLE;
import static com.qimu.autoclockin.constant.IpConstant.IP_URL;
import static com.qimu.autoclockin.model.enums.IpPoolStatusEnum.STARTING;
import static com.qimu.autoclockin.utils.DingTalkPushUtils.initDingTalkClient;
import static com.qimu.autoclockin.utils.DingTalkPushUtils.sendMessageByMarkdown;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 08:13:15
 * @Version: 1.0
 * @Description: 密钥过期侦听器
 */
@Component
@Slf4j
public class KeyExpirationListener extends KeyExpirationEventMessageListener {
    @Resource
    private TencentMapClient tencentMapClient;
    @Resource
    private DingTalkPushClient dingTalkPushClient;
    @Resource
    private EmailConfig emailConfig;
    @Resource
    private ClockInInfoService clockInInfoService;
    @Resource
    private DailyCheckInService dailyCheckInService;
    @Resource
    private RedissonLockUtil redissonLockUtil;
    @Resource
    private JavaMailSender mailSender;
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private IpPoolClient ipPoolClient;

    public KeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        if (StringUtils.isNotBlank(expiredKey) && expiredKey.startsWith(SIGN_USER_GROUP)) {
            String signId = expiredKey.replace(SIGN_USER_GROUP, StringUtils.EMPTY);
            redissonLockUtil.redissonDistributedLocks("user_sign_lock:" + signId, () -> {
                LambdaQueryWrapper<ClockInInfo> clockInInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
                clockInInfoLambdaQueryWrapper.eq(ClockInInfo::getClockInAccount, signId);
                ClockInInfo oldClockInInfo = clockInInfoService.getOne(clockInInfoLambdaQueryWrapper);
                if (oldClockInInfo == null) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "打卡信息不存在");
                }
                User user = userService.getById(oldClockInInfo.getUserId());
                LambdaQueryWrapper<DailyCheckIn> checkInLambdaQueryWrapper = new LambdaQueryWrapper<>();
                checkInLambdaQueryWrapper.eq(DailyCheckIn::getClockInAccount, oldClockInInfo.getClockInAccount());
                checkInLambdaQueryWrapper.eq(DailyCheckIn::getStatus, 1);
                long count = dailyCheckInService.count(checkInLambdaQueryWrapper);
                if (count > 0) {
                    LambdaUpdateWrapper<ClockInInfo> clockInInfoQueryWrapper = new LambdaUpdateWrapper<>();
                    clockInInfoQueryWrapper.eq(ClockInInfo::getId, oldClockInInfo.getId());
                    clockInInfoQueryWrapper.set(ClockInInfo::getStatus, ClockInStatusEnum.SUCCESS.getValue());
                    clockInInfoService.update(clockInInfoQueryWrapper);
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日已签到");
                }
                ClockInInfoVo clockInInfoVo = getClockInInfoVo(oldClockInInfo);
                ClockInInfo clockInInfo = new ClockInInfo();
                clockInInfo.setId(clockInInfoVo.getId());
                try {
                    boolean isEnable = ipPoolClient.isEnableTrue() && clockInInfoVo.getIsEnable().equals(STARTING.getValue());
                    ClockInStatus sign = AutoSignUtils.sign(isEnable, clockInInfoVo, buildUrl(), redisTemplate, tencentMapClient);
                    if (sign.getStatus()) {
                        clockInInfo.setStatus(ClockInStatusEnum.SUCCESS.getValue());
                        saveDailyCheckInInfo(clockInInfo, clockInInfoVo, sign.getMessage());
                    } else {
                        if (sign.getMessage().contains("密码错误")) {
                            LambdaUpdateWrapper<ClockInInfo> clockInInfoQueryWrapper = new LambdaUpdateWrapper<>();
                            clockInInfoQueryWrapper.eq(ClockInInfo::getId, oldClockInInfo.getId());
                            clockInInfoQueryWrapper.set(ClockInInfo::getStatus, ClockInStatusEnum.PAUSED.getValue());
                            clockInInfoService.update(clockInInfoQueryWrapper);
                            redisTemplate.delete(SIGN_USER_GROUP + clockInInfoVo.getClockInAccount());
                            return;
                        }
                        LambdaQueryWrapper<DailyCheckIn> dailyCheckInLambdaQueryWrapper = new LambdaQueryWrapper<>();
                        dailyCheckInLambdaQueryWrapper.eq(DailyCheckIn::getClockInAccount, clockInInfoVo.getClockInAccount());
                        DailyCheckIn checkInServiceOne = dailyCheckInService.getOne(dailyCheckInLambdaQueryWrapper);
                        clockInInfo.setStatus(ClockInStatusEnum.ERROR.getValue());
                        DailyCheckIn dailyCheckIn = new DailyCheckIn();
                        if (StringUtils.isNotBlank(sign.getMessage()) && sign.getMessage().contains("已打卡")) {
                            clockInInfo.setStatus(ClockInStatusEnum.SUCCESS.getValue());
                            dailyCheckIn.setStatus(1);
                        }
                        clockInInfoService.updateById(clockInInfo);
                        dailyCheckIn.setDescription(sign.getMessage());
                        dailyCheckIn.setUserId(user.getId());
                        dailyCheckIn.setClockInAccount(clockInInfoVo.getClockInAccount());
                        if (checkInServiceOne == null) {
                            dailyCheckInService.save(dailyCheckIn);
                        } else {
                            dailyCheckIn.setId(checkInServiceOne.getId());
                            dailyCheckInService.updateById(dailyCheckIn);
                        }
                        if (StringUtils.isNotBlank(user.getEmail())) {
                            sendEmail(user, "签到失败", sign.getMessage());
                        }
                        // 已经打卡就不用重试了
                        if (!sign.getMessage().contains("已打卡")) {
                            delayed(clockInInfoVo.getClockInAccount());
                        }
                        sendDingTalkMessage("签到失败", clockInInfoVo.getClockInAccount(), sign.getMessage(), clockInInfoVo.getAddress());
                    }
                } catch (Exception e) {
                    try {
                        LambdaQueryWrapper<DailyCheckIn> dailyCheckInLambdaQueryWrapper = new LambdaQueryWrapper<>();
                        dailyCheckInLambdaQueryWrapper.eq(DailyCheckIn::getClockInAccount, clockInInfoVo.getClockInAccount());
                        DailyCheckIn checkInServiceOne = dailyCheckInService.getOne(dailyCheckInLambdaQueryWrapper);
                        if (StringUtils.isNotBlank(user.getEmail())) {
                            sendEmail(user, "签到失败", e.getMessage());
                        }
                        clockInInfo.setStatus(ClockInStatusEnum.ERROR.getValue());
                        clockInInfoService.updateById(clockInInfo);
                        DailyCheckIn dailyCheckIn = new DailyCheckIn();
                        dailyCheckIn.setDescription(StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "签到失败,将在15分钟后重试");
                        dailyCheckIn.setUserId(user.getId());
                        dailyCheckIn.setStatus(0);
                        dailyCheckIn.setClockInAccount(clockInInfoVo.getClockInAccount());
                        if (checkInServiceOne == null) {
                            dailyCheckInService.save(dailyCheckIn);
                        } else {
                            dailyCheckIn.setId(checkInServiceOne.getId());
                            dailyCheckInService.updateById(dailyCheckIn);
                        }
                        sendDingTalkMessage("签到失败", clockInInfoVo.getClockInAccount(), e.getMessage(), clockInInfoVo.getAddress());
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    } finally {
                        delayed(clockInInfoVo.getClockInAccount());
                    }
                }
            });
        }
    }

    /**
     * 保存每日入住信息
     *
     * @param clockInInfo 打卡信息
     * @param message     消息
     * @throws MessagingException 消息传递异常
     */
    private void saveDailyCheckInInfo(ClockInInfo clockInInfo, ClockInInfoVo clockInInfoVo, String message) throws Exception {
        boolean update = clockInInfoService.updateById(clockInInfo);
        LambdaQueryWrapper<DailyCheckIn> checkInLambdaQueryWrapper = new LambdaQueryWrapper<>();
        checkInLambdaQueryWrapper.eq(DailyCheckIn::getClockInAccount, clockInInfoVo.getClockInAccount());
        DailyCheckIn checkInServiceOne = dailyCheckInService.getOne(checkInLambdaQueryWrapper);
        DailyCheckIn dailyCheckIn = new DailyCheckIn();
        dailyCheckIn.setDescription(message);
        User user = userService.getById(clockInInfoVo.getUserId());
        dailyCheckIn.setUserId(user.getId());
        dailyCheckIn.setClockInAccount(clockInInfoVo.getClockInAccount());
        if (StringUtils.isNotBlank(user.getEmail())) {
            if (update) {
                // 打卡成功
                dailyCheckIn.setStatus(1);
                sendEmail(user, "签到成功", message);
                redisTemplate.delete(SIGN_USER_GROUP + clockInInfoVo.getClockInAccount());
            } else {
                // 打卡失败
                dailyCheckIn.setStatus(0);
                clockInInfo.setStatus(ClockInStatusEnum.ERROR.getValue());
                clockInInfoService.updateById(clockInInfo);
                sendEmail(user, "签到失败", message);
                delayed(clockInInfoVo.getClockInAccount());
            }
        }
        if (checkInServiceOne == null) {
            dailyCheckIn.setStatus(1);
            dailyCheckInService.save(dailyCheckIn);
        } else {
            dailyCheckIn.setId(checkInServiceOne.getId());
            dailyCheckInService.updateById(dailyCheckIn);
        }
        sendDingTalkMessage("签到成功", clockInInfoVo.getClockInAccount(), message, clockInInfoVo.getAddress());
    }

    /**
     * 打卡失败15分钟后重试
     *
     * @param clockInAccount 打卡账号
     */
    private void delayed(String clockInAccount) {
        redisTemplate.opsForValue().set(SIGN_USER_GROUP + clockInAccount, clockInAccount, 15, TimeUnit.MINUTES);
    }

    private ClockInInfoVo getClockInInfoVo(ClockInInfo clockInInfo) {
        ClockInInfoVo clockInInfoVo = new ClockInInfoVo();
        BeanUtils.copyProperties(clockInInfo, clockInInfoVo);
        return clockInInfoVo;
    }

    /**
     * 发送电子邮件
     *
     * @param user    使用者
     * @param subject 主题
     * @param text    文本
     * @throws MessagingException 消息传递异常
     */
    private void sendEmail(User user, String subject, String text) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        // 邮箱发送内容组成
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setSubject(subject);
        helper.setText(text);
        helper.setFrom(EMAIL_TITLE + '<' + emailConfig.getEmailFrom() + '>');
        helper.setTo(user.getEmail());
        mailSender.send(mimeMessage);
    }

    /**
     * 生成url
     *
     * @return {@link String}
     */
    private String buildUrl() {
        String url = IP_URL + "?repeat=1" + "&format=" + ipPoolClient.getFormat() + "&protocol=" + ipPoolClient.getProtocol() + "&num=" + ipPoolClient.getExtractQuantity() + "&no=" + ipPoolClient.getPackageNumber() + "&mode=" + ipPoolClient.getAuthorizationMode() + "&secret=" + ipPoolClient.getPackageSecret() + "&minute=" + ipPoolClient.getOccupancyDuration() + "&pool=" + ipPoolClient.getIpPoolType();
        log.info("获取ip的url为：" + url);
        return url;
    }

    /**
     * 发送通话信息
     *
     * @param clockStatus      打卡状态
     * @param clockDescription 打卡描述
     * @param address          打卡住址
     * @param clockInAccount   打卡帐户
     * @throws UnsupportedEncodingException 不支持编码异常
     * @throws NoSuchAlgorithmException     没有这样算法例外
     * @throws InvalidKeyException          无效密钥异常
     */
    public void sendDingTalkMessage(String clockStatus, String clockInAccount, String clockDescription, String address) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        if (dingTalkPushClient.isEnable()) {
            if (StringUtils.isBlank(clockDescription)) {
                clockDescription = "暂无描述";
            }
            String message = "<h1 align=\"center\">\n" +
                    "   " + clockStatus + "\n" +
                    "</h1><br/>\n" +
                    "\n" +
                    "- 打卡账号：**" + clockInAccount + "**\n" +
                    "- 打卡状态：**" + clockStatus + "**\n" +
                    "- 打卡状态描述：**" + clockDescription + "**\n" +
                    "- 打卡时间：**" + DateUtil.now() + "**\n" +
                    "- 打卡地址：**" + address + "**\n";
            sendMessageByMarkdown(initDingTalkClient(dingTalkPushClient), clockStatus, message, null, false);
        }
    }
}