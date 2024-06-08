package ru.mts.depositservice.model;

import lombok.Data;
import ru.mts.depositservice.enums.RequestStatusEnum;

import java.util.Date;

@Data
public class ConfirmationResponse {
    private Integer requestId;
    private boolean isApproved;
    private Date requestDate;
    private RequestStatusEnum requestStatus;
}
