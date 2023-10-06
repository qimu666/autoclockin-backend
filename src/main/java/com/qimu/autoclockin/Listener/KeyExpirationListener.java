package com.qimu.autoclockin.Listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.config.EmailConfig;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.model.entity.ClockInInfo;
import com.qimu.autoclockin.model.entity.DailyCheckIn;
import com.qimu.autoclockin.model.entity.User;
import com.qimu.autoclockin.model.enums.ClockInStatusEnum;
import com.qimu.autoclockin.model.vo.ClockInInfoVo;
import com.qimu.autoclockin.service.ClockInInfoService;
import com.qimu.autoclockin.service.DailyCheckInService;
import com.qimu.autoclockin.service.UserService;
import com.qimu.autoclockin.utils.AutoSignUtils;
import com.qimu.autoclockin.utils.RedissonLockUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static com.qimu.autoclockin.constant.ClockInConstant.SIGN_USER_GROUP;
import static com.qimu.autoclockin.constant.EmailConstant.EMAIL_TITLE;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 08:13:15
 * @Version: 1.0
 * @Description: 密钥过期侦听器
 */
@Component
public class KeyExpirationListener extends KeyExpirationEventMessageListener {
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

    public KeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        if (StringUtils.isNotBlank(expiredKey) && expiredKey.startsWith(SIGN_USER_GROUP)) {
            String signId = expiredKey.replace(SIGN_USER_GROUP, StringUtils.EMPTY);
            redissonLockUtil.redissonDistributedLocks("user_sign_lock:" + signId, () -> {
                User user = userService.getById(signId);
                if (user == null) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR);
                }
                LambdaQueryWrapper<DailyCheckIn> checkInLambdaQueryWrapper = new LambdaQueryWrapper<>();
                checkInLambdaQueryWrapper.eq(DailyCheckIn::getUserId, user.getId());
                long count = dailyCheckInService.count();
                if (count > 0) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日已签到");
                }
                ClockInInfoVo clockInInfoVo = getClockInInfoVo(user);
                try {
                     boolean sign = AutoSignUtils.sign(clockInInfoVo);
                    if (sign) {
                        ClockInInfo clockInInfo = new ClockInInfo();
                        clockInInfo.setId(clockInInfoVo.getId());
                        clockInInfo.setStatus(ClockInStatusEnum.SUCCESS.getValue());
                        boolean update = clockInInfoService.updateById(clockInInfo);
                        if (StringUtils.isNotBlank(user.getEmail())) {
                            if (update) {
                                DailyCheckIn dailyCheckIn = new DailyCheckIn();
                                dailyCheckIn.setUserId(user.getId());
                                dailyCheckIn.setDescription("签到成功");
                                dailyCheckInService.save(dailyCheckIn);
                                sendEmail(user, "签到成功", "您的职校家园今日已签到成功");
                            } else {
                                sendEmail(user, "签到失败", "您的职校家园签到失败");
                            }
                        }
                    }
                } catch (Exception e) {
                    try {
                        sendEmail(user, "签到失败", "您的职校家园签到失败");
                    } catch (MessagingException ex) {
                        throw new RuntimeException(ex);
                    }
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, e.getMessage());
                }
            });
        }
    }

    private ClockInInfoVo getClockInInfoVo(User user) {
        ClockInInfoVo clockInInfoVo = new ClockInInfoVo();
        clockInInfoVo.setUserAccount(user.getUserAccount());
        clockInInfoVo.setUserPassword(user.getUserPassword());
        LambdaQueryWrapper<ClockInInfo> clockInInfoQueryWrapper = new LambdaQueryWrapper<>();
        clockInInfoQueryWrapper.eq(ClockInInfo::getUserId, user.getId());
        ClockInInfo clockInInfoServiceOne = clockInInfoService.getOne(clockInInfoQueryWrapper);
        clockInInfoVo.setDeviceType(clockInInfoServiceOne.getDeviceType());
        clockInInfoVo.setDeviceId(clockInInfoServiceOne.getDeviceId());
        clockInInfoVo.setLongitude(clockInInfoServiceOne.getLongitude());
        clockInInfoVo.setLatitude(clockInInfoServiceOne.getLatitude());
        clockInInfoVo.setAddress(clockInInfoServiceOne.getAddress());
        clockInInfoVo.setId(clockInInfoServiceOne.getId());
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
}