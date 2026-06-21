package org.petmeet.enums;

public enum PayStatusEnum {
    PENDING(0),
    SUCCESS(1),
    FAILED(2),
    CLOSED(3);

    private final int code;

    PayStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static PayStatusEnum fromCode(Integer code) {
        if (code == null) {
            return PENDING;
        }
        for (PayStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return PENDING;
    }
}
