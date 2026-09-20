package com.ledger.settlement.web;

/** The GET /payments/settlement response body. */
public record SettlementResponse(String merchantId, long amountOwedMinor) {
}
