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
import com.qimu.autoclockin.model.vo.ClockInStatus;
import com.qimu.autoclockin.service.ClockInInfoService;
import com.qimu.autoclockin.service.DailyCheckInService;
import com.qimu.autoclockin.service.UserService;
import com.qimu.autoclockin.utils.AutoSignUtils;
import com.qimu.autoclockin.utils.RedissonLockUtil;
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
import java.util.concurrent.TimeUnit;

import static com.qimu.autoclockin.constant.ClockInConstant.SIGN_USER_GROUP;
import static com.qimu.autoclockin.constant.EmailConstant.EMAIL_TITLE;
import static com.qimu.autoclockin.job.ClockInJob.getObtainClockInTime;

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
    @Resource
    private RedisTemplate<String, String> redisTemplate;

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
                User user = userService.getById(signId);
                if (user == null) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR);
                }
                LambdaQueryWrapper<DailyCheckIn> checkInLambdaQueryWrapper = new LambdaQueryWrapper<>();
                checkInLambdaQueryWrapper.eq(DailyCheckIn::getUserId, user.getId());
                checkInLambdaQueryWrapper.eq(DailyCheckIn::getStatus, 1);
                long count = dailyCheckInService.count(checkInLambdaQueryWrapper);
                if (count > 0) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日已签到");
                }
                ClockInInfoVo clockInInfoVo = getClockInInfoVo(user);
                ClockInInfo clockInInfo = new ClockInInfo();
                clockInInfo.setId(clockInInfoVo.getId());
                try {
                    ClockInStatus sign = AutoSignUtils.sign(clockInInfoVo);
                    if (sign.getStatus()) {
                        clockInInfo.setStatus(ClockInStatusEnum.SUCCESS.getValue());
                        saveDailyCheckInInfo(user, clockInInfo, sign.getMessage());
                    } else {
                        LambdaQueryWrapper<DailyCheckIn> dailyCheckInLambdaQueryWrapper = new LambdaQueryWrapper<>();
                        dailyCheckInLambdaQueryWrapper.eq(DailyCheckIn::getUserId, user.getId());
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
                        if (checkInServiceOne == null) {
                            dailyCheckInService.save(dailyCheckIn);
                        } else {
                            dailyCheckIn.setId(checkInServiceOne.getId());
                            dailyCheckInService.updateById(dailyCheckIn);
                        }
                        sendEmail(user, "签到失败", sign.getMessage());
                    }
                } catch (Exception e) {
                    try {
                        LambdaQueryWrapper<DailyCheckIn> dailyCheckInLambdaQueryWrapper = new LambdaQueryWrapper<>();
                        dailyCheckInLambdaQueryWrapper.eq(DailyCheckIn::getUserId, user.getId());
                        DailyCheckIn checkInServiceOne = dailyCheckInService.getOne(dailyCheckInLambdaQueryWrapper);
                        sendEmail(user, "签到失败", e.getMessage());
                        clockInInfo.setStatus(ClockInStatusEnum.ERROR.getValue());
                        clockInInfoService.updateById(clockInInfo);
                        DailyCheckIn dailyCheckIn = new DailyCheckIn();
                        dailyCheckIn.setDescription(e.getMessage());
                        dailyCheckIn.setUserId(user.getId());
                        dailyCheckIn.setStatus(0);
                        if (checkInServiceOne == null) {
                            dailyCheckInService.save(dailyCheckIn);
                        } else {
                            dailyCheckIn.setId(checkInServiceOne.getId());
                            dailyCheckInService.updateById(dailyCheckIn);
                        }
                    } catch (MessagingException ex) {
                        throw new RuntimeException(ex);
                    } finally {
                        delayed(user, clockInInfo);
                    }
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, e.getMessage());
                }
            });
        }
    }

    /**
     * 保存每日入住信息
     *
     * @param user        使用者
     * @param clockInInfo 打卡信息
     * @param message     消息
     * @throws MessagingException 消息传递异常
     */
    private void saveDailyCheckInInfo(User user, ClockInInfo clockInInfo, String message) throws MessagingException {
        boolean update = clockInInfoService.updateById(clockInInfo);
        LambdaQueryWrapper<DailyCheckIn> checkInLambdaQueryWrapper = new LambdaQueryWrapper<>();
        checkInLambdaQueryWrapper.eq(DailyCheckIn::getUserId, user.getId());
        DailyCheckIn checkInServiceOne = dailyCheckInService.getOne(checkInLambdaQueryWrapper);
        DailyCheckIn dailyCheckIn = new DailyCheckIn();
        dailyCheckIn.setDescription(message);
        dailyCheckIn.setUserId(user.getId());
        if (StringUtils.isNotBlank(user.getEmail())) {
            if (update) {
                // 打卡成功
                dailyCheckIn.setStatus(1);
                sendEmail(user, "签到成功", message);
                redisTemplate.delete(SIGN_USER_GROUP + user.getId());
            } else {
                // 打卡失败
                dailyCheckIn.setStatus(0);
                clockInInfo.setStatus(ClockInStatusEnum.ERROR.getValue());
                clockInInfoService.updateById(clockInInfo);
                sendEmail(user, "签到失败", message);
                delayed(user, clockInInfo);
            }
        }
        if (checkInServiceOne == null) {
            dailyCheckInService.save(dailyCheckIn);
        } else {
            dailyCheckIn.setId(checkInServiceOne.getId());
            dailyCheckInService.updateById(dailyCheckIn);
        }
    }

    private void delayed(User user, ClockInInfo clockInInfo) {
        long secondsUntilUserTime = getObtainClockInTime(user.getId(), clockInInfo.getClockInTime());
        if (secondsUntilUserTime > 0) {
            int oneHoursSeconds = 60 * 60;
            redisTemplate.opsForValue().set(SIGN_USER_GROUP + user.getId(), String.valueOf(user.getId()), secondsUntilUserTime + oneHoursSeconds, TimeUnit.SECONDS);
        }
    }

    private ClockInInfoVo getClockInInfoVo(User user) {
        ClockInInfoVo clockInInfoVo = new ClockInInfoVo();
        LambdaQueryWrapper<ClockInInfo> clockInInfoQueryWrapper = new LambdaQueryWrapper<>();
        clockInInfoQueryWrapper.eq(ClockInInfo::getUserId, user.getId());
        ClockInInfo clockInInfoServiceOne = clockInInfoService.getOne(clockInInfoQueryWrapper);
        BeanUtils.copyProperties(clockInInfoServiceOne, clockInInfoVo);
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