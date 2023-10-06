package com.qimu.autoclockin.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年08月13日 18:13
 * @Version: 1.0
 * @Description:
 */
@Data
public class Detail implements Serializable {
    private ClockInInfoMap detail;
    private static final long serialVersionUID = 5460269501476160853L;
}
