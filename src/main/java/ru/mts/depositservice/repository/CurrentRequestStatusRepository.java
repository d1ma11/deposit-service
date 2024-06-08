package ru.mts.depositservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mts.depositservice.entity.CurrentRequestStatus;
import ru.mts.depositservice.entity.Request;

@Repository
public interface CurrentRequestStatusRepository extends JpaRepository<CurrentRequestStatus, Integer> {
    CurrentRequestStatus findCurrentRequestStatusByRequest(Request request);
}
