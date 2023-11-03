package com.qimu.autoclockin.config;

import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.model.dto.dingTalk.DingTalkPushClient;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: QiMu
 * @Date: 2023/11/01 12:18:25
 * @Version: 1.0
 * @Description: 钉钉机器人消息推送配置
 */
@Configuration
@ConfigurationProperties(prefix = "dingtalk.message.push")
@Data
public class DingTalkMessagePushConfig {
    /**
     * 是否启用钉钉推送消息
     */
    private boolean enable;
    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 密钥
     */
    private String secret;

    @Bean
    public DingTalkPushClient dingTalkPushClient() {
        if (enable) {
            if (StringUtils.isAnyBlank(accessToken, secret)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "钉钉推送消息机器人配置有误");
            }
            return new DingTalkPushClient(true, accessToken, secret);
        }
        return new DingTalkPushClient();
    }
}
