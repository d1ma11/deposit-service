package ru.mts.depositservice.service;

public interface ConfirmationService {
    /**
     * Генерирует случайный код подтверждения
     *
     * @return Строка, представляющая сгенерированный код подтверждения
     */
    String generateVerificationCode();
}
