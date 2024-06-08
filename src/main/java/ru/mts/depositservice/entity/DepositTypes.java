package ru.mts.depositservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.mts.depositservice.enums.DepositTypeEnum;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "deposit_types", schema = "public")
public class DepositTypes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_deposit_type")
    private Integer id;

    @Column(name = "deposit_type_name")
    @Enumerated(EnumType.STRING)
    private DepositTypeEnum typeName;
}
