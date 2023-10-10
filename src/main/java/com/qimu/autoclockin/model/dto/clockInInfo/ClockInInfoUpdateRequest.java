package com.qimu.autoclockin.model.dto.clockInInfo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 09:43:18
 * @Version: 1.0
 * @Description: 更新请求
 */
@Data
public class ClockInInfoUpdateRequest implements Serializable {
    /**
     * 打卡账号
     */
    private String clockInAccount;
    /**
     * 打卡密码
     */
    private String clockPassword;
    /**
     * id
     */
    private Long id;

    /**
     * 打卡地址
     */
    private String address;

    /**
     * 设备型号
     */
    private String deviceType;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 经度
     */
    private String longitude;

    /**
     * 纬度
     */
    private String latitude;
    /**
     * 打卡时间
     */
    private String clockInTime;
    /**
     * 打卡状态( 0-未开始 1-已打卡)
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}