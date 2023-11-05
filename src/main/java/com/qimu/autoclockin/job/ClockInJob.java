package com.qimu.autoclockin.job;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qimu.autoclockin.model.entity.ClockInInfo;
import com.qimu.autoclockin.model.entity.DailyCheckIn;
import com.qimu.autoclockin.model.enums.ClockInStatusEnum;
import com.qimu.autoclockin.service.ClockInInfoService;
import com.qimu.autoclockin.service.DailyCheckInService;
import com.qimu.autoclockin.utils.RedissonLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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


    /**
     * 每日凌晨自动打卡
     */
    @Scheduled(cron = "0 0 0 * * *")
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
            // 每天已经打卡的用户
            List<ClockInInfo> successClockInInfo = new ArrayList<>();

            // 已开启自动打卡的用户id
            List<String> clockInStartingUserIdList = clockInInfoService.list()
                    .stream()
                    .filter(clockInInfo -> {
                        if (!clockInInfo.getStatus().equals(ClockInStatusEnum.PAUSED.getValue()) && !clockInInfo.getStatus().equals(ClockInStatusEnum.ERROR.getValue())) {
                            if (clockInInfo.getStatus().equals(ClockInStatusEnum.SUCCESS.getValue())) {
                                successClockInInfo.add(clockInInfo);
                            }
                            return true;
                        }
                        return false;
                    })
                    .map(ClockInInfo::getClockInAccount)
                    .distinct()
                    .collect(Collectors.toList());
            log.info("所有打卡账号为:" + clockInStartingUserIdList);

            successClockInInfo.forEach(clockInInfo -> {
                ClockInInfo newClock = new ClockInInfo();
                BeanUtils.copyProperties(clockInInfo, newClock);
                newClock.setStatus(ClockInStatusEnum.STARTING.getValue());
                clockInInfoService.updateById(newClock);
            });

            clockInStartingUserIdList.forEach(clockInAccount -> {
                String clockInUserId = redisTemplate.opsForValue().get(SIGN_USER_GROUP + clockInAccount);
                if (StringUtils.isNotBlank(clockInUserId)) {
                    return;
                }
                try {
                    LambdaQueryWrapper<ClockInInfo> clockInInfoQueryWrapper = new LambdaQueryWrapper<>();
                    clockInInfoQueryWrapper.eq(ClockInInfo::getClockInAccount, clockInAccount);
                    ClockInInfo clockInInfo = clockInInfoService.getOne(clockInInfoQueryWrapper);
                    addClockInToRedis(clockInInfo);
                } catch (Exception ignored) {
                    // 如果重复抛异常,就删除其他记录，只保留一条
                    LambdaQueryWrapper<ClockInInfo> clockInInfoQueryWrapper = new LambdaQueryWrapper<>();
                    clockInInfoQueryWrapper.eq(ClockInInfo::getClockInAccount, clockInAccount);
                    List<ClockInInfo> list = clockInInfoService.list(clockInInfoQueryWrapper);
                    // 重复数据id
                    List<Long> idsToDelete = new ArrayList<>();
                    if (list.size() > 1) {
                        // 添加要删除的元素的ID到列表中
                        for (int i = 1; i < list.size(); i++) {
                            idsToDelete.add(list.get(i).getId());
                        }
                    }

                    // 批量删除重复的打卡信息
                    if (!idsToDelete.isEmpty()) {
                        clockInInfoService.removeByIds(idsToDelete);
                    }

                    // 打卡信息补录
                    ClockInInfo clockInInfo = list.get(0);
                    addClockInToRedis(clockInInfo);
                }
            });
        });
    }

    private void addClockInToRedis(ClockInInfo clockInInfo) {
        long secondsUntilUserTime = getObtainClockInTime(clockInInfo);
        if (secondsUntilUserTime > 0) {
            int randomTime = RandomUtil.randomInt(10, 1000);
            redisTemplate.opsForValue().set(SIGN_USER_GROUP + clockInInfo.getClockInAccount(), String.valueOf(clockInInfo.getClockInAccount()), secondsUntilUserTime + randomTime, TimeUnit.SECONDS);
            log.info("打卡账号添加成功:{}", clockInInfo.getClockInAccount());
        }
    }

    /**
     * 获取打卡时间
     *
     * @param clockInInfo 打卡信息
     * @return long
     */
    public static long getObtainClockInTime(ClockInInfo clockInInfo) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        if (StringUtils.isBlank(clockInInfo.getClockInTime())) {
            log.error("打卡时间未设置,打卡账号为：{}", clockInInfo.getClockInAccount());
            return -1;
        }
        LocalTime userTime = LocalTime.parse(clockInInfo.getClockInTime());
        LocalDateTime userDateTime = currentDateTime.with(userTime);
        Duration duration = Duration.between(currentDateTime, userDateTime);
        return duration.getSeconds();
    }
}
