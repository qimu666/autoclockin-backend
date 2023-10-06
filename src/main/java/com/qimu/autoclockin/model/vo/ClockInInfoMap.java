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
public class ClockInInfoMap implements Serializable {
    /**
     * 经度
     */
    private String pointx;
    /**
     * 纬度
     */
    private String pointy;

    private static final long serialVersionUID = 5303531927837364177L;
}
