package ru.mts.depositservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.mts.depositservice.entity.Request;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Integer> {

    @Query(nativeQuery = true, value =
            "SELECT * FROM requests " +
                    "INNER JOIN current_request_status crs on requests.id_request = crs.request_id " +
                    "WHERE customer_id = :customerId " +
                    "AND " +
                    "request_status_id = 4"
    )
    List<Request> findRequestsByCustomerId(@Param("customerId") Integer integer);
}
