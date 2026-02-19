package com.arister.repository;

import com.arister.model.Customer;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerCode(String customerCode);

    boolean existsByEmail(String email);

    boolean existsByIdNumber(String idNumber);

    Page<Customer> findAll(Pageable pageable);
}
