package ru.mts.depositservice.enums;

public enum DepositDurationEnum {
    MONTH_3(3),     // на 3 месяца
    MONTH_6(6),     // на 6 месяцев
    YEAR(12);       // на 1 год

    private final int duration;

    DepositDurationEnum(int duration) {
        this.duration = duration;
    }

    /**
     * Возвращает продолжительность вклада в месяцах
     *
     * @return Продолжительность вклада в месяцах
     */
    public int getDuration() {
        return this.duration;
    }
}
