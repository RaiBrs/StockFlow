package com.rai.stockflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Registro imutável de uma alteração de saldo. */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "balance_id", nullable = false)
    private StockBalance balance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockMovementType type;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "previous_balance", nullable = false)
    private long previousBalance;

    @Column(name = "new_balance", nullable = false)
    private long newBalance;

    @Column(length = 500)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected StockMovement() {
    }

    public StockMovement(StockBalance balance, User actor, StockMovementType type,
                         long quantity, long previousBalance, long newBalance,
                         String reason, Instant occurredAt) {
        this.balance = balance;
        this.actor = actor;
        this.type = type;
        this.quantity = quantity;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public StockBalance getBalance() { return balance; }
    public User getActor() { return actor; }
    public StockMovementType getType() { return type; }
    public long getQuantity() { return quantity; }
    public long getPreviousBalance() { return previousBalance; }
    public long getNewBalance() { return newBalance; }
    public String getReason() { return reason; }
    public Instant getOccurredAt() { return occurredAt; }
}
