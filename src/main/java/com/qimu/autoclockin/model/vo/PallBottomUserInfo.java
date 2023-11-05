package com.qimu.autoclockin.model.vo;

import lombok.Data;

/**
 * @Author: QiMu
 * @Date: 2023年11月03日 17:09
 * @Version: 1.0
 * @Description:
 */
@Data
public class PallBottomUserInfo {
    private String phone;
    private String password;
    private String deviceType;
    private String deviceId;
    private String address;
    private String longitude;
    private String latitude;
}
