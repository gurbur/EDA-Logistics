package com.jihwan.logistics.simulator.simulator;

import com.jihwan.logistics.simulator.domain.Order;

import java.util.List;

public class SimulatorRunner {

    private final SimulatorState state;
    private final List<Order> orders;

    public SimulatorRunner(SimulatorState state, List<Order> orders) {
        this.state = state;
        this.orders = orders;
    }

    public void run() {

        for (Order order : orders) {
            state.processOrder(order);
        }
    }
}
