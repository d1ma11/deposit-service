package ru.mts.depositservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.mts.depositservice.enums.PercentPaymentTypeEnum;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "types_percent_payment", schema = "public")
public class TypesPercentPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type_percent_payment")
    private Integer id;

    @Column(name = "type_percent_payment_period")
    @Enumerated(EnumType.STRING)
    private PercentPaymentTypeEnum typeName;
}
