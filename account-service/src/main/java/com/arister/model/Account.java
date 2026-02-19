package com.arister.model;

import com.arister.common.model.BaseEntity;
import com.arister.enums.AccountStatus;
import com.arister.enums.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {

    @Column(name = "account_number", insertable = false, updatable = false)
    private String accountNumber;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal balance;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "interest_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal interestRate;

    @Column(name = "min_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal minBalance;

    @Column(name = "overdraft_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal overdraftLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Column(name = "closed_at")
    private ZonedDateTime closedAt;

    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (status == null) status = AccountStatus.ACTIVE;
        if (balance == null) balance = BigDecimal.ZERO;
        if (currency == null || currency.isBlank()) currency = "USD";
        if (interestRate == null) interestRate = BigDecimal.ZERO;
        if (minBalance == null) minBalance = new BigDecimal("500.00");
        if (overdraftLimit == null) overdraftLimit = BigDecimal.ZERO;
    }
}
