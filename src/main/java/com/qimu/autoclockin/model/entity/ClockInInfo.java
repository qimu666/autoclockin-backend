package com.qimu.autoclockin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 09:44:23
 * @Version: 1.0
 * @Description: 打卡信息
 */
@TableName(value = "clock_in_info")
@Data
public class ClockInInfo implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}