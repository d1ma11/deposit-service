package ru.mts.depositservice.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Date;

@Data
@SuperBuilder
public class RequestResponse {
    private BigDecimal amount;
    private Date requestDate;
}
