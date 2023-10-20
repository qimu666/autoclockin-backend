package com.qimu.autoclockin.config;

import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.model.dto.IpPool.IpPoolClient;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: QiMu
 * @Date: 2023/10/20 01:45:41
 * @Version: 1.0
 * @Description: ip池配置
 */
@ConfigurationProperties(prefix = "ip.config")
@Configuration
@Data
public class IpPoolConfig {
    /**
     * 是否启用ip池
     */
    private boolean enable;
    /**
     * 套餐编号
     */
    private String packageNumber;
    /**
     * 套餐编号密钥
     */
    private String packageSecret;
    /**
     * 占用持续时间,值为1 3 5 10 15 30
     */
    private String occupancyDuration = "1";
    /**
     * 提取量
     */
    private String extractQuantity = "1";
    /**
     * 授权模式 可选值有 'whitelist' 和 'auth' 分别为白名单授权方式或者账号密码授权
     */
    private String authorizationMode = "auth";

    /**
     * ip池类型 优质IP: quality 普通IP池: ordinary
     */
    private String ipPoolType = "quality";


    @Bean
    public IpPoolClient ipPoolClient() {
        if (enable) {
            if (StringUtils.isBlank(packageNumber)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "ip池套餐编号未配置");
            }
            if (StringUtils.isBlank(packageSecret)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "ip池套餐编号密匙未配置");
            }
            return new IpPoolClient(true, packageNumber, packageSecret, occupancyDuration, extractQuantity, authorizationMode, ipPoolType, "json", "1");
        }
        return new IpPoolClient();
    }
}
