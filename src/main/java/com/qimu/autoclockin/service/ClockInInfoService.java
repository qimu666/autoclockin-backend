package com.qimu.autoclockin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qimu.autoclockin.model.entity.ClockInInfo;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 09:44:21
 * @Version: 1.0
 * @Description: 打卡信息服务
 */
public interface ClockInInfoService extends IService<ClockInInfo> {
    /**
     * 有效打卡信息
     * 校验
     *
     * @param add         是否为创建校验
     * @param clockInInfo 打卡信息
     */
    void validClockInInfo(ClockInInfo clockInInfo, boolean add);
}
