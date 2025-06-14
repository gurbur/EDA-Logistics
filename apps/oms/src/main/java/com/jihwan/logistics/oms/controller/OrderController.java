package com.jihwan.logistics.oms.controller;

import com.jihwan.logistics.oms.domain.Order;
import com.jihwan.logistics.oms.dto.OrderRequestDto;
import com.jihwan.logistics.oms.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<String> createOrder(@Valid @RequestBody OrderRequestDto request) {
        Order order = orderService.createOrder(
                request.getUserId(),
                request.getItemId(),
                request.getDestination()
        );

        return ResponseEntity.ok(order.getOrderId());
    }

    @GetMapping("/{orderId}/failures")
    public ResponseEntity<Map<String, String>> getFailureReasons(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getFailureReasons(orderId));
    }

}
