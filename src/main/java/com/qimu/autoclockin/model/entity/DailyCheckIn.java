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
 * @Date: 2023/10/06 09:44:30
 * @Version: 1.0
 * @Description: 每日签到表
 */
@TableName(value = "daily_check_in")
@Data
public class DailyCheckIn implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 打卡状态( 0-打卡失败 1-已打卡)
     */
    private Integer status;
    /**
     * 签到人
     */
    private Long userId;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}