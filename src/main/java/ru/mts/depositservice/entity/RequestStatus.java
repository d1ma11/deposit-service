package ru.mts.depositservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.mts.depositservice.enums.RequestStatusEnum;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "request_statuses", schema = "public")
public class RequestStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_request_status")
    private Integer id;

    @Column(name = "request_status_name")
    @Enumerated(EnumType.STRING)
    private RequestStatusEnum statusName;
}
