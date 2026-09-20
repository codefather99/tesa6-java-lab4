package com.ledger.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * The JPA-mapped row for one payment.
 *
 * This is a mutable CLASS, not a record, and that is a JPA constraint, not a style
 * choice. Hibernate instantiates every managed entity through a no-argument constructor
 * and then populates its fields by reflection before running any application code -
 * there is no constructor call it can pass values through. A record's canonical
 * constructor is the only constructor a record can have, and its components are final,
 * so a record can never satisfy that instantiate-then-populate protocol. Records also
 * define equals/hashCode over every component, which is wrong for an entity: two
 * managed rows with the same id should be equal even if a field changes underneath one
 * of them mid-transaction, and JPA's own identity-based equality (or none at all, which
 * is what this class does by inheriting Object's) is what proxies and dirty-checking
 * expect.
 *
 * amountMinor stays a bigint-backed long, never a floating point column - see
 * V1__create_payments.sql - for the same reason it has been long in every earlier lab:
 * binary floating point cannot represent minor units exactly, and the drift is the
 * MR-4471 rounding gap this module exists to prevent.
 */
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    /** Required by JPA. Hibernate populates the fields afterward by reflection. */
    protected PaymentEntity() {
    }

    public PaymentEntity(String id, String merchantId, long amountMinor, String currency, Instant recordedAt) {
        this.id = id;
        this.merchantId = merchantId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.recordedAt = recordedAt;
    }

    public String getId() {
        return id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    /**
     * Identity equality by id only, the conventional and safe choice for a JPA entity -
     * unlike a record's all-fields equality, this stays correct across lazy-loaded or
     * partially-flushed fields.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PaymentEntity that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        // A constant, not Objects.hash(id): id can be null before the first flush, and a
        // hash code must never change once an entity is placed in a HashSet or as a Map
        // key, which id-based hashing would violate across that transition.
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PaymentEntity[id=" + id + ", merchantId=" + merchantId
                + ", amountMinor=" + amountMinor + ", currency=" + currency
                + ", recordedAt=" + recordedAt + "]";
    }
}
