package com.arister.model;

import com.arister.common.model.BaseEntity;
import com.arister.enums.CustomerStatus;
import com.arister.enums.IdType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(name = "customer_code", insertable = false, updatable = false)
    private String customerCode;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type")
    private IdType idType;

    @Column(name = "id_number", unique = true)
    private String idNumber;

    @Column(name = "kyc_verified")
    private boolean kycVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CustomerStatus status;

    @Column(name = "branch_id")
    private Integer branchId;

    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (status == null) {
            status = CustomerStatus.ACTIVE;
        }
    }
}
