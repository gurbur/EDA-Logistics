package com.jihwan.logistics.simulator.domain;

public class Order {

    private final String orderId;
    private final String source;
    private final String destination;

    public Order(String orderId, String source, String destination) {
        this.orderId = orderId;
        this.source = source;
        this.destination = destination;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        return "[" + orderId + "] " + source + " → " + destination;
    }
}
