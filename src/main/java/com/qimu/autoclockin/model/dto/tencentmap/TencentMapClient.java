package com.qimu.autoclockin.model.dto.tencentmap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: QiMu
 * @Date: 2023年11月02日 18:46
 * @Version: 1.0
 * @Description:
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TencentMapClient {
    private boolean enable;
    private String key;
}
