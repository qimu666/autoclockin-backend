package com.qimu.autoclockin.model.dto.autoclockin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: QiMu
 * @Date: 2023年11月15日 20:25
 * @Version: 1.0
 * @Description:
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AutoClockInClient {
    /**
     * os 操作系统
     */
    private String os = "android";
    /**
     * 应用程序版本
     */
    String appVersion = "59";
    /**
     * token请求地址
     */
    String tokenUrl = "https://sxbaapp.zcj.jyt.henan.gov.cn/api/getApitoken.ashx";
    /**
     * 登录网址
     */
    String loginUrl = "https://sxbaapp.zcj.jyt.henan.gov.cn/api/relog.ashx";

    /**
     * 打卡地址
     */
    String signUrl = "https://sxbaapp.zcj.jyt.henan.gov.cn/api/clockindaily20220827.ashx";
}
