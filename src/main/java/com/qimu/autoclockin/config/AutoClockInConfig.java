package com.qimu.autoclockin.config;

import com.qimu.autoclockin.model.dto.autoclockin.AutoClockInClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: QiMu
 * @Date: 2023/11/15 08:21:10
 * @Version: 1.0
 * @Description: 自动打卡配置
 */
@Configuration
@ConfigurationProperties(prefix = "auto.clock.config")
@Data
public class AutoClockInConfig {

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


    @Bean
    public AutoClockInClient autoClockInClient() {
        return new AutoClockInClient(os, appVersion, tokenUrl, loginUrl, signUrl);
    }
}
