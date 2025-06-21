package com.jihwan.logistics.simulator.simulator;

import com.jihwan.logistics.simulator.domain.Truck;
import com.jihwan.logistics.simulator.domain.Worker;
import com.jihwan.logistics.simulator.publisher.SimulatorEventPublisher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimulatorDispatcher {

    private final SimulatorState state;
    private final SimulatorEventPublisher eventPublisher;

    public SimulatorDispatcher(SimulatorState state, SimulatorEventPublisher eventPublisher) {
        this.state = state;
        this.eventPublisher = eventPublisher;
    }

    private static class DispatchContext {
        String orderId;
        String workerId;
        String truckId;
        String source;
        String destination;

        DispatchContext(String orderId) {
            this.orderId = orderId;
        }
    }

    private final Map<String, DispatchContext> dispatchMap = new ConcurrentHashMap<>();

    public void onWorkerAllocated(String orderId, String workerId) {
        DispatchContext context = dispatchMap.computeIfAbsent(orderId, DispatchContext::new);
        context.workerId = workerId;
        tryStartDispatch(context);
    }

    public void onTruckAllocated(String orderId, String truckId) {
        DispatchContext context = dispatchMap.computeIfAbsent(orderId, DispatchContext::new);
        context.truckId = truckId;
        tryStartDispatch(context);
    }

    public void onDispatchStart(String orderId, String source, String destination) {
        DispatchContext context = dispatchMap.computeIfAbsent(orderId, DispatchContext::new);
        context.source = source;
        context.destination = destination;
        tryStartDispatch(context);
    }

    private void tryStartDispatch(DispatchContext context) {
        if (isReadyToStart(context)) {
            Logger.logDispatchStarted(context.orderId, context.source, context.destination);

            // 자원 상태 변경
            state.updateWorkerStatus(context.workerId, Worker.Status.ASSIGNED, context.source);
            state.updateTruckStatus(context.truckId, Truck.Status.ASSIGNED, context.source);

            // 시뮬레이션 시작 (새 스레드)
            SimulatorDeliveryTask task = new SimulatorDeliveryTask(
                    context.orderId,
                    context.truckId,
                    context.workerId,
                    context.source,
                    context.destination,
                    state,
                    eventPublisher
            );
            Thread thread = new Thread(task);
            thread.start();

            // 더 이상 context 필요 없음
            dispatchMap.remove(context.orderId);
        }
    }

    private boolean isReadyToStart(DispatchContext context) {
        return context.workerId != null &&
                context.truckId != null &&
                context.source != null &&
                context.destination != null;
    }
}
