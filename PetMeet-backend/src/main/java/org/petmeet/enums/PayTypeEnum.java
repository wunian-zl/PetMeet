package org.petmeet.enums;

import org.petmeet.common.AppException;

public enum PayTypeEnum {
    ALIPAY(1, "支付宝"),
    WECHAT_MOCK(2, "微信支付");

    private final int code;
    private final String displayName;

    PayTypeEnum(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public int getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PayTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PayTypeEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw AppException.badRequest("不支持的支付方式");
    }

    public static PayTypeEnum fromValue(String value) {
        if (value == null || value.isBlank()) {
            return ALIPAY;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_");
        if ("WECHAT".equals(normalized)) {
            normalized = "WECHAT_MOCK";
        }
        for (PayTypeEnum item : values()) {
            if (item.name().equals(normalized)) {
                return item;
            }
        }
        throw AppException.badRequest("不支持的支付方式");
    }
}
