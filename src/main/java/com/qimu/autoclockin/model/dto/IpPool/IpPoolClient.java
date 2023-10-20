package com.qimu.autoclockin.model.dto.IpPool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: QiMu
 * @Date: 2023年10月20日 14:19
 * @Version: 1.0
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IpPoolClient {
    /**
     * 是否启用ip池
     */
    private boolean isEnableTrue;
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
    private String occupancyDuration;
    /**
     * 提取量
     */
    private String extractQuantity;
    /**
     * 授权模式
     */
    private String authorizationMode;

    /**
     * ip池类型 优质IP: quality 普通IP池: ordinary
     */
    private String ipPoolType;
    /**
     * 返回格式类型
     */
    private String format;
    /**
     * 使用协议
     * http/https: 1
     * socket5: 3
     */
    private String protocol;
}
