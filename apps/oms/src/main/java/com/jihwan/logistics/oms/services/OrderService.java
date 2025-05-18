package com.jihwan.logistics.oms.services;

import com.jihwan.logistics.oms.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OrderService {
    private final ConcurrentHashMap<String, Order> orderStore = new ConcurrentHashMap<>();

    public Order createOrder(String userId, String itemId, String destination) {
        Order order = new Order(userId, itemId, destination);
        orderStore.put(order.getOrderId(), order);

        log.info("Order created: {}", order.getOrderId());
        return order;
    }
}
