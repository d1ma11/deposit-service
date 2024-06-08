package ru.mts.depositservice.model;

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
    private BigDecimal depositAmount;
    private String confirmationCode;
}
