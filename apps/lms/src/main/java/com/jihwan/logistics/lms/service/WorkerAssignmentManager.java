package com.jihwan.logistics.lms.service;

import java.util.HashSet;
import java.util.Set;

public class WorkerAssignmentManager {
    private final Set<String> assignedOrders = new HashSet<>();

    public boolean tryAssignWorker(String orderId) {
        assignedOrders.add(orderId);
        return true;
        /*boolean canAssign = orderId.hashCode() % 2 != 0;
        if (canAssign) assignedOrders.add(orderId);
        return canAssign;*/
    }
}
