package com.ledger.settlement.error;

/**
 * Thrown when a merchant id has no payments recorded against it.
 *
 * Unchecked, exactly as in the sjv-l0-2 lab: this means the request itself refers to a
 * merchant Ledger has never heard of, which is a client error to be turned into a 404
 * problem detail by ProblemHandler, not a condition the calling code can retry around.
 */
public class MerchantNotFoundException extends RuntimeException {

    private final String merchantId;

    public MerchantNotFoundException(String merchantId) {
        super("no merchant found with id: " + merchantId);
        this.merchantId = merchantId;
    }

    public String getMerchantId() {
        return merchantId;
    }
}
