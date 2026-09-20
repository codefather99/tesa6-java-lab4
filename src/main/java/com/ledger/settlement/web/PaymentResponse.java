package com.ledger.settlement.web;

import java.time.Instant;

import com.ledger.settlement.domain.PaymentEntity;

/**
 * The API shape for a payment - deliberately NOT PaymentEntity.
 *
 * This is the second of the four Agent Review defects (see research.md): returning the
 * JPA entity straight from a controller couples the wire format to the persistence
 * mapping, so a column rename becomes a breaking API change, and risks Jackson trying to
 * serialise a lazy-loaded Hibernate proxy field it was never meant to see. A dedicated
 * response record has neither problem.
 */
public record PaymentResponse(String id, String merchantId, long amountMinor, String currency, Instant recordedAt) {

    public static PaymentResponse from(PaymentEntity entity) {
        return new PaymentResponse(
                entity.getId(),
                entity.getMerchantId(),
                entity.getAmountMinor(),
                entity.getCurrency(),
                entity.getRecordedAt());
    }
}
