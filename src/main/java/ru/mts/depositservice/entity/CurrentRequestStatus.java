package ru.mts.depositservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "current_request_status", schema = "public")
public class CurrentRequestStatus {

    @EmbeddedId
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private CurrentRequestStatusKey id;

    @MapsId(value = "requestId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private Request request;

    @MapsId(value = "requestStatusId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_status_id")
    private RequestStatus status;

    @Column(name = "change_datetime")
    private LocalDateTime updateTime;
}
