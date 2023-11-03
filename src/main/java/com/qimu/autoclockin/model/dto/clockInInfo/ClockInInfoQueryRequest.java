package com.qimu.autoclockin.model.dto.clockInInfo;

import com.qimu.autoclockin.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 *
 * @author qimu
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ClockInInfoQueryRequest extends PageRequest implements Serializable {

    /**
     * 创建用户
     */
    private Long userId;
    /**
     * 打卡账号
     */
    private String clockInAccount;
    /**
     * 打卡地址
     */
    private String address;

    /**
     * 设备型号
     */
    private String deviceType;

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

    private static final long serialVersionUID = 1L;
}