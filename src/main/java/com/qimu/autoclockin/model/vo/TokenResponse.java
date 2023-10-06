package com.qimu.autoclockin.model.vo;

import lombok.Data;

/**
 * @author qimu
 */
@Data
public class TokenResponse {
    private Integer code;
    private String deviceid;
    private TokenData data;
    private String msg;
    private Integer currentpage;
    private Integer pagesize;
    private Integer totalcount;
    private Integer pagecount;
}