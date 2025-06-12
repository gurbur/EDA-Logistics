package com.jihwan.logistics.tms.service;

import java.util.HashSet;
import java.util.Set;

public class TruckAssignmentManager {

    private final Set<String> assignedOrders = new HashSet<>();

    public boolean tryAssignTruck(String orderId) {
        assignedOrders.add(orderId);
        return true;
        /*if (assignedOrders.contains(orderId)) return true;

        boolean assigned = orderId.hashCode() % 2 == 0;
        if (assigned) {
            assignedOrders.add(orderId);
        }
        return assigned;*/
    }

}
