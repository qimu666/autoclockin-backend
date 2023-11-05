package com.qimu.autoclockin.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年11月02日 17:07
 * @Version: 1.0
 * @Description:
 */
@Data
public class MapResult implements Serializable {
    private static final long serialVersionUID = 2078387270342622636L;
    private String status;
    private String message;
    private ResultData result;

    @Data
    public static class ResultData implements Serializable {
        private Location location;
        private String address;
        private static final long serialVersionUID = 2814086012754087281L;
    }

    @Data
    public static class Location implements Serializable {
        private String lat;
        private String lng;
        private static final long serialVersionUID = 2814086012754087281L;
    }
}
