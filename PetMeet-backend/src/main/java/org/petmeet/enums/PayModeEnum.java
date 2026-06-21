package org.petmeet.enums;

public enum PayModeEnum {
    QR_CODE(1);

    private final int code;

    PayModeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static PayModeEnum fromValue(String value) {
        if (value == null || value.isBlank()) {
            return QR_CODE;
        }
        return QR_CODE;
    }
}
