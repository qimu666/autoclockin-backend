package com.qimu.autoclockin.model.vo;

import lombok.Data;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 09:42:47
 * @Version: 1.0
 * @Description: 登录结果
 */
@Data
public class LoginResult {
    private int code;
    private String deviceId;
    private LoginResultData data;
    private String msg;
}
