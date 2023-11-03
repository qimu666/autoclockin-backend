package com.qimu.autoclockin.model.dto.dingTalk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: QiMu
 * @Date: 2023/11/01 12:44:12
 * @Version: 1.0
 * @Description: ding-talk客户端
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DingTalkPushClient {
    private boolean enable;
    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 密钥
     */
    private String secret;
}
