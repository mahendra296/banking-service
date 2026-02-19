package com.arister.repository;

import com.arister.model.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("SELECT t FROM Transfer t WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId")
    Page<Transfer> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);
}
