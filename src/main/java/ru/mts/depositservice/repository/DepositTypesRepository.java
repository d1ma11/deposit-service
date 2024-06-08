package ru.mts.depositservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mts.depositservice.entity.DepositTypes;
import ru.mts.depositservice.enums.DepositTypeEnum;

import java.util.Optional;

@Repository
public interface DepositTypesRepository extends JpaRepository<DepositTypes, Integer> {
    Optional<DepositTypes> findDepositTypesByTypeName(DepositTypeEnum depositTypeEnum);
}
