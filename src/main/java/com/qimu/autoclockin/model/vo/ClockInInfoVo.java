package com.qimu.autoclockin.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

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
     * 是否启用ip池
     */
    private Integer isEnable;
    /**
     * 创建用户
     */
    private Long userId;
    /**
     * 打卡时间
     */
    private String clockInTime;
    /**
     * 打卡地址
     */
    private String address;

    /**
     * 打卡账号
     */
    private String clockInAccount;
    /**
     * 打卡密码
     */
    private String clockPassword;

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
     * 打卡状态( 0-未开始 1-已打卡)
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 描述
     */
    private String description;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 5303531927837364177L;
}
