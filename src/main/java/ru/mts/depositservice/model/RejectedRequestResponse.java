package ru.mts.depositservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
public class RejectedRequestResponse extends RequestResponse {
    private BigDecimal amount;
    private String rejectionReason;
}
