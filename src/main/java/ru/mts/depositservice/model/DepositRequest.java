package ru.mts.depositservice.model;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {
    private Integer requestId;
    @Positive(message = "Нельзя оперировать отрицательной суммой денег")
    private BigDecimal depositAmount;
    private String confirmationCode;
}
