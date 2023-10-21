package com.qimu.autoclockin.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: QiMu
 * @Date: 2023/10/21 03:02:03
 * @Version: 1.0
 * @Description: ip池状态枚举
 */
public enum IpPoolStatusEnum {

    /**
     * 已启动
     */
    STARTING("已启动", 1),
    /**
     * 已暂停
     */
    PAUSED("未开启", 0);
    private final String text;

    private final int value;

    IpPoolStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return {@link List}<{@link Integer}>
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
