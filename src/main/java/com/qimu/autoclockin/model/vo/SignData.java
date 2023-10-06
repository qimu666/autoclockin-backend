package com.qimu.autoclockin.model.vo;

import lombok.Data;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 09:42:42
 * @Version: 1.0
 * @Description: 符号数据
 */
@Data
public class SignData {
    private int dtype;
    private int probability;
    private String address;
    private String longitude;
    private String latitude;
    private String phonetype;
    private String uid;
}