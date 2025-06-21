package com.jihwan.logistics.simulator;

import com.jihwan.logistics.simulator.domain.Truck;
import com.jihwan.logistics.simulator.domain.Worker;
import com.jihwan.logistics.simulator.publisher.SimulatorEventPublisher;
import com.jihwan.logistics.simulator.simulator.Logger;
import com.jihwan.logistics.simulator.simulator.SimulatorDispatcher;
import com.jihwan.logistics.simulator.subscriber.SimulatorEventSubscriber;
import com.jihwan.logistics.simulator.simulator.SimulatorState;
import com.solacesystems.jcsmp.JCSMPException;

public class Main {

    public static void main(String[] args) throws JCSMPException {
        Logger.logStart();

        // 1. 시뮬레이터 상태 초기화
        SimulatorState state = new SimulatorState();

        // 2. 자원 등록
        state.registerTruck(new Truck("T1", "서울"));
        state.registerTruck(new Truck("T2", "부산"));
        state.registerTruck(new Truck("T3", "서울"));

        state.registerWorker(new Worker("W1", "서울"));
        state.registerWorker(new Worker("W2", "부산"));
        state.registerWorker(new Worker("W3", "서울"));

        // 3. 메시지 발행기 및 Dispatcher 초기화
        SimulatorEventPublisher publisher = new SimulatorEventPublisher();
        SimulatorDispatcher dispatcher = new SimulatorDispatcher(state, publisher);

        // 4. 메시지 수신기 구동 (비동기 스레드)
        SimulatorEventSubscriber subscriber = new SimulatorEventSubscriber(dispatcher);
        Thread subscriberThread = new Thread(() -> {
            try {
                subscriber.start(); // 메시지 수신 시작
            } catch (Exception e) {
                System.err.println("❌ SimulatorEventSubscriber 실패: " + e.getMessage());
            }
        });
        subscriberThread.start();
    }
}
