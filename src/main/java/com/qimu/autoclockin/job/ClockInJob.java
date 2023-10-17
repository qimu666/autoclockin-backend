package com.qimu.autoclockin.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qimu.autoclockin.model.entity.ClockInInfo;
import com.qimu.autoclockin.model.entity.DailyCheckIn;
import com.qimu.autoclockin.model.entity.User;
import com.qimu.autoclockin.model.enums.ClockInStatusEnum;
import com.qimu.autoclockin.service.ClockInInfoService;
import com.qimu.autoclockin.service.DailyCheckInService;
import com.qimu.autoclockin.service.UserService;
import com.qimu.autoclockin.utils.RedissonLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.qimu.autoclockin.constant.ClockInConstant.SIGN_USER_GROUP;

/**
 * @Author: QiMu
 * @Date: 2023年10月07日 08:45
 * @Version: 1.0
 * @Description:
 */
@Component
@Slf4j
public class ClockInJob {
    @Resource
    private ClockInInfoService clockInInfoService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private DailyCheckInService dailyCheckInService;
    @Resource
    private RedissonLockUtil redissonLockUtil;
    @Resource
    private UserService userService;

    /**
     * 每日凌晨自动打卡
     */
    // @Scheduled(cron = "0 0 0 * * *")
    @Scheduled(fixedDelay = 10000)

    public void doAutoClockIn() {
        redissonLockUtil.redissonDistributedLocks("ClockInJob:clearCheckInList", () -> {
            // 每批删除的数据量
            int batchSize = 1000;
            // 是否还有数据需要删除
            boolean hasMoreData = true;

            while (hasMoreData) {
                // 分批查询数据
                List<DailyCheckIn> dataList = dailyCheckInService.list(new QueryWrapper<DailyCheckIn>().last("LIMIT " + batchSize));

                if (dataList.isEmpty()) {
                    // 没有数据了，退出循环
                    hasMoreData = false;
                } else {
                    // 批量删除数据
                    dailyCheckInService.removeByIds(dataList.stream().map(DailyCheckIn::getId).collect(Collectors.toList()));
                }
            }
            // 已开启自动打卡的用户id
            List<Long> clockInStartingUserIdList = clockInInfoService.list()
                    .stream()
                    .filter(clockInInfo ->
                            !clockInInfo.getStatus().equals(ClockInStatusEnum.PAUSED.getValue()) && !clockInInfo.getStatus().equals(ClockInStatusEnum.ERROR.getValue()))
                    .map(ClockInInfo::getUserId)
                    .collect(Collectors.toList());
            log.info("打卡所有用户id:" + clockInStartingUserIdList);
            clockInStartingUserIdList.forEach(id -> {
                String clockInUserId = redisTemplate.opsForValue().get(SIGN_USER_GROUP + id);
                if (StringUtils.isNotBlank(clockInUserId)) {
                    return;
                }
                User user = userService.getById(id);
                LambdaQueryWrapper<ClockInInfo> clockInInfoQueryWrapper = new LambdaQueryWrapper<>();
                clockInInfoQueryWrapper.eq(ClockInInfo::getUserId, user.getId());
                ClockInInfo clockInInfo = clockInInfoService.getOne(clockInInfoQueryWrapper);
                long secondsUntilUserTime = getObtainClockInTime(user.getId(), clockInInfo.getClockInTime());
                if (secondsUntilUserTime > 0) {
                    redisTemplate.opsForValue().set(SIGN_USER_GROUP + user.getId(), String.valueOf(user.getId()), secondsUntilUserTime, TimeUnit.SECONDS);
                    log.info("打卡用户添加成功:{} ,打卡时间：{}", user.getId(), clockInInfo.getClockInTime());
                }
            });
        });
    }

    /**
     * 获取打卡时间
     *
     * @param clockInTime 打卡时间
     * @return long
     */
    public static long getObtainClockInTime(Long userId, String clockInTime) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        if (StringUtils.isBlank(clockInTime)) {
            log.error("打卡时间未设置,用户为：{}", userId);
            return -1;
        }
        LocalTime userTime = LocalTime.parse(clockInTime);
        LocalDateTime userDateTime = currentDateTime.with(userTime);
        Duration duration = Duration.between(currentDateTime, userDateTime);
        return duration.getSeconds();
    }
}
