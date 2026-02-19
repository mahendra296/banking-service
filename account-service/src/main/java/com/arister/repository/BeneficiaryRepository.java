package com.arister.repository;

import com.arister.model.Beneficiary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    List<Beneficiary> findByCustomerId(Long customerId);
    boolean existsByCustomerIdAndAccountNumber(Long customerId, String accountNumber);
}
