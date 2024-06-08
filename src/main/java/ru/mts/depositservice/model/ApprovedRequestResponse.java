package ru.mts.depositservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ru.mts.depositservice.enums.DepositTypeEnum;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
public class ApprovedRequestResponse extends RequestResponse {
    private Integer requestId;
    private DepositTypeEnum depositTypeEnum;
    private BigDecimal percentageRate;
}
