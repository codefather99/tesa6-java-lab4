package com.ledger.settlement.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds ledger.fee-rate from application.yml.
 *
 * Deliberately typed as BigDecimal, not double: it is read once from human-edited YAML
 * (0.031 is easy to write and easy to review), but it must never be multiplied against
 * amountMinor as a double - that is exactly the floating point mistake labs sjv-l0-1 and
 * ledger-settlement (Maven lab) both audited out. SettlementService converts this value
 * to an exact integer basis-points figure once, at construction, and every calculation
 * after that point is long arithmetic. See SettlementService's constructor for the
 * conversion and its guard.
 */
@ConfigurationProperties(prefix = "ledger")
public class LedgerProperties {

    private final BigDecimal feeRate;

    public LedgerProperties(BigDecimal feeRate) {
        this.feeRate = feeRate;
    }

    public BigDecimal getFeeRate() {
        return feeRate;
    }
}
