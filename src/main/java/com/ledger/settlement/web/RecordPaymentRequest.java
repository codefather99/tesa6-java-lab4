package com.ledger.settlement.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * The POST /payments request body.
 *
 * A record, unlike PaymentEntity: this is a transport shape with no identity and no
 * lifecycle - it exists for exactly one request and is never managed, proxied or
 * dirty-checked, so record's all-final, all-args-equal semantics are exactly right here.
 */
public record RecordPaymentRequest(
        @NotBlank(message = "merchantId must not be blank") String merchantId,
        @Positive(message = "amountMinor must be positive") long amountMinor,
        @NotBlank(message = "currency must not be blank") String currency) {
}
