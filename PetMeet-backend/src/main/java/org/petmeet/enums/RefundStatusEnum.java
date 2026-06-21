package org.petmeet.enums;

public enum RefundStatusEnum {
    PENDING(0),
    SUCCESS(1),
    FAILED(2);

    private final int code;

    RefundStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
