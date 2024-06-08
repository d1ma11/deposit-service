package ru.mts.depositservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.mts.depositservice.enums.DepositDurationEnum;
import ru.mts.depositservice.enums.DepositTypeEnum;
import ru.mts.depositservice.enums.PercentPaymentTypeEnum;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OpenDepositRequest extends DepositRequest {
    private Integer customerId;
    private DepositTypeEnum depositType;
    private DepositDurationEnum duration;
    private boolean isCapitalized;
    private PercentPaymentTypeEnum percentPaymentType;
}
