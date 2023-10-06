package com.qimu.autoclockin.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 05:48:25
 * @Version: 1.0
 * @Description: 打卡状态枚举
 */
public enum ClockInStatusEnum {

    /**
     * 已启动
     */
    STARTING("已启动", 1),
    /**
     * 已暂停
     */
    PAUSED("已暂停", 0),
    /**
     * 打卡失败
     */
    ERROR("打卡失败", 3),
    /**
     * 已完成
     */
    SUCCESS("已完成", 2);


    private final String text;

    private final int value;

    ClockInStatusEnum(String text, int value) {
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
