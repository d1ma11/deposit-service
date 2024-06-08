package ru.mts.depositservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfoResponse {
    private String phoneNumber;
    private BigDecimal amount;
}
