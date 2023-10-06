package com.qimu.autoclockin.model.vo;

import lombok.Data;

/**
 * @author qimu
 */
@Data
public class LoginData {
    private String password;
    private String phone;
    private Integer dtype;
    private String dToken;
}