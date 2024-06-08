package ru.mts.depositservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class RejectedRefillResponse extends RequestResponse {
    private String rejectionReason;
}
