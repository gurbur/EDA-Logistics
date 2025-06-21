package com.jihwan.logistics.simulator.simulator;

import com.jihwan.logistics.simulator.domain.Truck;
import com.jihwan.logistics.simulator.domain.Worker;

public class Logger {

    public static void logStart() {
        System.out.println("[시뮬레이터 시작]");
    }

    public static void logAssignment(String orderId, Truck truck, Worker worker, String dest) {
        System.out.printf("📦 주문 %s 할당됨 → Truck[%s] + Worker[%s], 목적지: %s%n",
                orderId, truck.getId(), worker.getId(), dest);
    }

    public static void logCompletion(String orderId, Truck truck, Worker worker) {
        System.out.printf("✅ 주문 %s 처리 완료 → Truck[%s], Worker[%s] 복귀 준비%n",
                orderId, truck.getId(), worker.getId());
    }

    public static void logFailure(String orderId, String reason) {
        System.out.printf("❌ 주문 %s 처리 실패 → 사유: %s%n", orderId, reason);
    }

    public static void logReturn(String truckId, String workerId, String location) {
        System.out.printf("↩ Truck[%s], Worker[%s] %s로 복귀 완료%n", truckId, workerId, location);
    }
}
