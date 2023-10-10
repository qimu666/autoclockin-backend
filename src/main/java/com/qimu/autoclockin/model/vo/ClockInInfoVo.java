package com.qimu.autoclockin.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年08月13日 17:58
 * @Version: 1.0
 * @Description:
 */
@Data
public class ClockInInfoVo implements Serializable {
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
     * 打卡账号
     */
    private String clockInAccount;
    /**
     * 打卡密码
     */
    private String clockPassword;
    /**
     * 经度
     */
    private String longitude;

    /**
     * 纬度
     */
    private String latitude;

    private static final long serialVersionUID = 5303531927837364177L;
}
