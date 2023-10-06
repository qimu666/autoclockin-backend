package com.qimu.autoclockin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qimu.autoclockin.model.entity.DailyCheckIn;
import com.qimu.autoclockin.service.DailyCheckInService;
import com.qimu.autoclockin.mapper.DailyCheckInMapper;
import org.springframework.stereotype.Service;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 09:44:39
 * @Version: 1.0
 * @Description: 每日入住服务impl
 */
@Service
public class DailyCheckInServiceImpl extends ServiceImpl<DailyCheckInMapper, DailyCheckIn>
    implements DailyCheckInService{

}




