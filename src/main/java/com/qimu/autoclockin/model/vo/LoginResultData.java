package com.qimu.autoclockin.model.vo;

import lombok.Data;

/**
 * @author qimu
 */
@Data
public class LoginResultData {
    private String uid;
    private int state;
    private int type;
    private String UserToken;
}