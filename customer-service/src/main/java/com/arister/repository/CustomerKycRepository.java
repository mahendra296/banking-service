package com.arister.repository;

import com.arister.model.CustomerKyc;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerKycRepository extends JpaRepository<CustomerKyc, Long> {

    List<CustomerKyc> findByCustomerId(Long customerId);
}
