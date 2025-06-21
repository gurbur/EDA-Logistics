package com.jihwan.logistics.simulator.simulator;

import com.jihwan.logistics.simulator.domain.Truck;
import com.jihwan.logistics.simulator.domain.Worker;
import com.jihwan.logistics.simulator.publisher.SimulatorEventPublisher;

public class SimulatorDeliveryTask implements Runnable {

    private final String orderId;
    private final String truckId;
    private final String workerId;
    private final String source;
    private final String destination;

    private final SimulatorState state;
    private final SimulatorEventPublisher publisher;

    private static final int STEP_INTERVAL_MS = 10_000; // 10초 간격
    private static final int TOTAL_STEPS = 5; // 가상 경로상 총 5단계

    public SimulatorDeliveryTask(
            String orderId,
            String truckId,
            String workerId,
            String source,
            String destination,
            SimulatorState state,
            SimulatorEventPublisher publisher
    ) {
        this.orderId = orderId;
        this.truckId = truckId;
        this.workerId = workerId;
        this.source = source;
        this.destination = destination;
        this.state = state;
        this.publisher = publisher;
    }

    @Override
    public void run() {
        try {
            for (int step = 1; step <= TOTAL_STEPS; step++) {
                Thread.sleep(STEP_INTERVAL_MS);

                String progressLocation = source + " → " + destination + " (Step " + step + "/" + TOTAL_STEPS + ")";
                Logger.logStatus(orderId, progressLocation);
                publisher.publishStatus(orderId, progressLocation);
            }

            // 배송 완료 처리
            Logger.logCompletion(orderId, truckId, workerId);

            // 자원 상태 갱신: RETURNING → IDLE
            state.updateTruckStatus(truckId, Truck.Status.RETURNING, destination);
            state.updateWorkerStatus(workerId, Worker.Status.RETURNING, destination);

            // 자원 반환 메시지 발행
            publisher.publishResourceReturned(orderId, workerId, truckId);

            // 자원 복귀 처리
            state.updateTruckStatus(truckId, Truck.Status.IDLE, destination);
            state.updateWorkerStatus(workerId, Worker.Status.IDLE, destination);

            Logger.logReturn(truckId, workerId, destination);

        } catch (InterruptedException e) {
            Logger.logFailure(orderId, "Thread interrupted during delivery.");
            Thread.currentThread().interrupt(); // reset interrupt flag
        }
    }
}
