package ru.mts.depositservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Date;

@Data
@SuperBuilder
public class RequestResponse {
    private BigDecimal amount;
    @JsonFormat(pattern="dd-MM-yyyy")
    private Date requestDate;
}
