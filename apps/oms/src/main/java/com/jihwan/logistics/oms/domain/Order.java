package com.jihwan.logistics.oms.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
public class Order {
    private final String orderId;
    private final String userId;
    private final String itemId;
    private final String destination;
    private final Instant createdAt;
    private OrderStatus status;
    private Instant updatedAt;

    public Order(String userId, String itemId, String destination) {
        this.orderId = "ORD-" + UUID.randomUUID();
        this.userId = userId;
        this.itemId = itemId;
        this.destination = destination;
        this.status = OrderStatus.PLACED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public boolean updateStatus(OrderStatus next) {
        if (!isValidTransition(this.status, next)) {
            throw new IllegalStateException("Invalid status transition: " + this.status + " -> " + next);
        }
        this.status = next;
        this.updatedAt = Instant.now();
        return true;
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case PLACED -> next == OrderStatus.CONFIRMED || next == OrderStatus.FAILED;
            case CONFIRMED -> next == OrderStatus.DISPATCHED || next == OrderStatus.FAILED;
            case DISPATCHED -> next == OrderStatus.DELIVERED || next == OrderStatus.FAILED;
            case DELIVERED, FAILED -> false;
        };
    }
}
