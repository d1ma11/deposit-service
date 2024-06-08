package ru.mts.depositservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mts.depositservice.entity.TypesPercentPayment;
import ru.mts.depositservice.enums.PercentPaymentTypeEnum;

import java.util.Optional;

@Repository
public interface PercentPaymentTypesRepository extends JpaRepository<TypesPercentPayment, Integer> {
    Optional<TypesPercentPayment> findTypesPercentPaymentByTypeName(PercentPaymentTypeEnum typeName);
}
