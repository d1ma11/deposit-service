package ru.mts.depositservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mts.depositservice.entity.RequestStatus;
import ru.mts.depositservice.enums.RequestStatusEnum;

@Repository
public interface RequestStatusRepository extends JpaRepository<RequestStatus, Integer> {
    RequestStatus findRequestStatusByStatusName(RequestStatusEnum requestStatus);
}
