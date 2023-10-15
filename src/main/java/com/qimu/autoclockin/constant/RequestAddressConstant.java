package com.qimu.autoclockin.constant;

/**
 * @Author: QiMu
 * @Date: 2023/08/14 10:40:48
 * @Version: 1.0
 * @Description: 请求地址常数
 */
public interface RequestAddressConstant {
    /**
     * 经度和纬度
     */
    String LONGITUDE_AND_LATITUDE = "https://apis.map.qq.com/jsapi?qt=geoc&addr=";

    /**
     * 标记url
     */
    String TOKEN_URL = "https://sxbaapp.vae.ha.cn/interface/token.ashx";

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
    /**
     * 登录网址
     */
    String LOGIN_URL = "http://sxbaapp.vae.ha.cn/interface/relog.ashx";


    /**
     * 标志url
     */
    String SIGN_URL = "http://sxbaapp.vae.ha.cn/interface/clockindaily20220827.ashx";

    /**
     * 报告界面
     */
    String REPORTING_INTERFACE = "https://sxbaapp.zcj.jyt.henan.gov.cn/interface/ReportHandler.ashx";
}
