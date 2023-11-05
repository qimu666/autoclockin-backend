package com.qimu.autoclockin.config;

import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.model.dto.tencentmap.TencentMapClient;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: QiMu
 * @Date: 2023/11/02 08:06:49
 * @Version: 1.0
 * @Description: 腾讯地图配置
 */
@Configuration
@ConfigurationProperties(prefix = "tencent.map")
@Data
public class TencentMapConfig {
    /**
     * 是否启用腾讯地图
     */
    private boolean enable;
    private String key;

    @Bean
    public TencentMapClient tencentMapClient() {
        if (enable) {
            if (StringUtils.isBlank(key)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "腾讯地图api请求key必须配置：参考地址：https://lbs.qq.com/dev/console/application/mine");
            }
            return new TencentMapClient(true, key);
        }
        return new TencentMapClient();
    }
}
