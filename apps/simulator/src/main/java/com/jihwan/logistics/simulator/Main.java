package com.jihwan.logistics.simulator;

import com.jihwan.logistics.simulator.domain.Order;
import com.jihwan.logistics.simulator.domain.Truck;
import com.jihwan.logistics.simulator.domain.Worker;
import com.jihwan.logistics.simulator.simulator.Logger;
import com.jihwan.logistics.simulator.simulator.SimulatorRunner;
import com.jihwan.logistics.simulator.simulator.SimulatorState;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        Logger.logStart();

        // 1. 상태 객체 초기화
        SimulatorState state = new SimulatorState();

        // 2. 자원 등록
        state.registerTruck(new Truck("T1", "서울"));
        state.registerTruck(new Truck("T2", "부산"));
        state.registerTruck(new Truck("T3", "서울"));

        state.registerWorker(new Worker("W1", "서울"));
        state.registerWorker(new Worker("W2", "부산"));
        state.registerWorker(new Worker("W3", "서울"));

        // 3. 주문 생성
        List<Order> orders = List.of(
                new Order("O1", "서울", "대전"),
                new Order("O2", "부산", "광주"),
                new Order("O3", "서울", "부산"),
                new Order("O4", "서울", "인천") // 실패할 가능성 있음
        );

        // 4. 러너 실행
        SimulatorRunner runner = new SimulatorRunner(state, orders);
        runner.run();

        // 5. 상태 출력
        state.printStatus();
    }
}