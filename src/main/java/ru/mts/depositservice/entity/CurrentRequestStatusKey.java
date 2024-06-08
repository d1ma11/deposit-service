package ru.mts.depositservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class CurrentRequestStatusKey implements Serializable {

    @Column(name = "request_id")
    private Integer requestId;

    @Column(name = "request_status_id")
    private Integer requestStatusId;
}
