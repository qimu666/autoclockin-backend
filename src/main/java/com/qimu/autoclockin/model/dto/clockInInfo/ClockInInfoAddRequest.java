package com.qimu.autoclockin.model.dto.clockInInfo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 01:44:44
 * @Version: 1.0
 * @Description: 创建请求
 */
@Data
public class ClockInInfoAddRequest implements Serializable {
    /**
     * 用户帐户
     */
    private String userAccount;
    /**
     * 打卡账号
     */
    private String clockInAccount;
    /**
     * 打卡密码
     */
    private String clockPassword;

    /**
     * 打卡地址
     */
    private String address;
    /**
     * 邮箱
     */
    private String email;

    /**
     * 设备型号
     */
    private String deviceType;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 打卡时间
     */
    private String clockInTime;
    private static final long serialVersionUID = 1L;
}