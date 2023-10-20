package com.qimu.autoclockin.utils;

import lombok.Data;

import java.util.List;

/**
 * @Author: QiMu
 * @Date: 2023/10/20 06:50:47
 * @Version: 1.0
 * @Description: 响应数据
 */
@Data
public class ResponseData {
    private Data data;
    private int code;
    private String message;
    private int status;

    @lombok.Data
    public static class Data {
        private List<IpInfo> list;
    }

    @lombok.Data
    public static class IpInfo {
        private String ip;
        private String port;
        private long expired;
        private String net;
        private String account;
        private String password;
    }
}