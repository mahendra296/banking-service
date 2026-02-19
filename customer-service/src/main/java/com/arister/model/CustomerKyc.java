package com.arister.model;

import com.arister.common.model.BaseEntity;
import com.arister.enums.KycVerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@Table(name = "customer_kyc")
public class CustomerKyc extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "issuing_country")
    private String issuingCountry;

    @Column(name = "issuing_authority")
    private String issuingAuthority;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    private KycVerificationStatus verificationStatus;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "notes")
    private String notes;

    @Override
    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (verificationStatus == null) {
            verificationStatus = KycVerificationStatus.PENDING;
        }
    }
}
