package com.ledger.settlement.error;

/**
 * Thrown when a payment id does not exist. Added alongside the GET /payments/{id}
 * endpoint required by the Agent Review step - see research.md.
 */
public class PaymentNotFoundException extends RuntimeException {

    private final String paymentId;

    public PaymentNotFoundException(String paymentId) {
        super("no payment found with id: " + paymentId);
        this.paymentId = paymentId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}
