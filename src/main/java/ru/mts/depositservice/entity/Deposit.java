package ru.mts.depositservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "deposits", schema = "public")
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_deposit")
    private Integer id;

    @Column(name = "deposit_refill")
    private boolean depositRefill;
    @Column(name = "deposit_withdraw")
    private boolean depositWithdraw;
    @Column(name = "capitalization")
    private boolean capitalization;
    @Column(name = "deposit_amount")
    private BigDecimal depositAmount;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "deposit_rate")
    private BigDecimal depositRate;
    @Column(name = "percent_payment_date")
    private LocalDate percentPaymentDate;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deposit_account_id")
    private BankAccount bankAccount;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deposit_type_id")
    private DepositTypes depositType;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_percent_payment_id")
    private TypesPercentPayment typePercentPayment;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "percent_payment_account_id")
    private BankAccount percentPaymentAccount;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deposit_refund_account_id")
    private BankAccount depositRefundAccount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
