package com.jihwan.logistics.oms.services;

import com.jihwan.logistics.oms.domain.Order;
import com.jihwan.logistics.oms.domain.OrderStatus;
import com.jihwan.logistics.oms.event.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderEventPublisher publisher;
    private final ConcurrentHashMap<String, Order> orderStore = new ConcurrentHashMap<>();

    public Order createOrder(String userId, String itemId, String destination) {
        Order order = new Order(userId, itemId, destination);
        orderStore.put(order.getOrderId(), order);

        Map<String, Object> payload = buildPayload(order);
        publisher.publishOrderCreated(payload, destination, order.getOrderId());

        log.info("Order created: {}", order.getOrderId());
        return order;
    }

    public void confirmOrder(String orderId) {
        Order order = orderStore.get(orderId);
        if (order == null) {
            log.warn("No such order to confirm: {}", orderId);
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Map<String, Object> payload = buildPayload(order);
        publisher.publishOrderConfirmed(payload, order.getDestination(), order.getOrderId());
        log.info("Order confirmed: {}", orderId);
    }

    public void failOrder(String orderId) {
        Order order = orderStore.get(orderId);
        if (order == null) {
            log.warn("No such order to fail: {}", orderId);
            return;
        }

        order.setStatus(OrderStatus.FAILED);
        Map<String, Object> payload = buildPayload(order);
        publisher.publishOrderFailed(payload, order.getDestination(), order.getOrderId());
        log.info("Order failed: {}", orderId);
    }

    private Map<String, Object> buildPayload(Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("order_id", order.getOrderId());
        payload.put("user_id", order.getUserId());
        payload.put("item_id", order.getItemId());
        payload.put("destination", order.getDestination());
        payload.put("status", order.getStatus().name());
        payload.put("timestamp", Instant.now().toString());
        return payload;
    }

    public void deliverOrder(String orderId) {
        Order order = orderStore.get(orderId);
        if (order == null) {
            log.warn("No such order to deliver: {}", orderId);
            return;
        }

        order.setStatus(OrderStatus.DELIVERED);
        Map<String, Object> payload = buildPayload(order);
        publisher.publishOrderDelivered(payload, order.getDestination(), order.getOrderId());
        log.info("Order delivered: {}", orderId);
    }

    public void dispatchOrder(String orderId) {
        Order order = orderStore.get(orderId);
        if (order == null) {
            log.warn("No such order to dispatch: {}", orderId);
            return;
        }

        order.setStatus(OrderStatus.DISPATCHED);
        Map<String, Object> payload = buildPayload(order);
        publisher.publishOrderDispatched(payload, order.getDestination(), order.getOrderId());
        log.info("Order dispatched: {}", orderId);
    }

    public void modifyOrder(String orderId, Map<String, Object> updates) {
        Order order = orderStore.get(orderId);
        if (order == null) {
            log.warn("Cannot modify non-existing order: {}", orderId);
            return;
        }

        if (updates.containsKey("status")) {
            try {
                OrderStatus newStatus = OrderStatus.valueOf((String) updates.get("status"));
                order.setStatus(newStatus);
                log.info("Order {} status updated to {}", orderId, newStatus);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status provided for order {}: {}", orderId, updates.get("status"));
            }
        }

        if (updates.containsKey("destination")) {
            String newDest = (String) updates.get("destination");
            log.info("Order {} destination changed: {} -> {}", orderId, order.getDestination(), newDest);
            order.updateDestination(newDest);  // 이 메서드는 Order.java에 새로 정의해야 함
        }

    }

}
