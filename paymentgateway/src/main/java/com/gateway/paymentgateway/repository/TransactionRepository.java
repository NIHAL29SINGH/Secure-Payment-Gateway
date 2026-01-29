package com.gateway.paymentgateway.repository;

import com.gateway.paymentgateway.entity.Transaction;
import com.gateway.paymentgateway.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUser(User user);

    long countByUser(User user);
}
