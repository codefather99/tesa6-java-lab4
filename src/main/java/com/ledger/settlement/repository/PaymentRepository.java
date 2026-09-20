package com.ledger.settlement.repository;

import com.ledger.settlement.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * No SQL of our own: findByMerchantId is a derived query, parsed by Spring Data from the
 * method name at startup and validated against the entity mapping then - a typo in the
 * property name fails fast at boot, not at the first request against an unlucky merchant.
 */
public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

    List<PaymentEntity> findByMerchantId(String merchantId);
}
