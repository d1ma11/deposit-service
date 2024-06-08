package ru.mts.depositservice.enums;

public enum DepositDurationEnum {
    MONTH_3(3),
    MONTH_6(6),
    YEAR(12);

    private final int duration;

    DepositDurationEnum(int duration) {
        this.duration = duration;
    }

    public int getDuration() {
        return this.duration;
    }
}
