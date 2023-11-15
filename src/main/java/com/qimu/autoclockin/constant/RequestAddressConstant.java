package com.qimu.autoclockin.constant;

/**
 * @Author: QiMu
 * @Date: 2023/08/14 10:40:48
 * @Version: 1.0
 * @Description: 请求地址常数
 */
public interface RequestAddressConstant {

    /**
     * 内容类型
     */
    String CONTENT_TYPE = "application/json;charset=UTF-8";
    /**
     * 接受编码
     */
    String ACCEPT_ENCODING = "gzip";
    /**
     * 用户代理
     */
    String USER_AGENT = "okhttp/3.14.9";
    /**
     * cl-ip
     */
    String CL_IP = "192.168.190.1";
    /**
     * 经度和纬度
     */
    String LONGITUDE_AND_LATITUDE = "https://apis.map.qq.com/jsapi?qt=geoc&addr=";

    /**
     * 密钥
     */
    String SECRET_KEY = "Anything_2023";
    /**
     * 成功code
     */
    int SUCCESS_CODE = 1001;
    /**
     * 数据类型
     */
    int DTYPE = 6;
}
