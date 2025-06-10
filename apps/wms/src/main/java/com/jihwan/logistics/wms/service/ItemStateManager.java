package com.jihwan.logistics.wms.service;

import com.jihwan.logistics.wms.domain.ItemStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemStateManager {
    private final Map<String, ItemStatus> itemStateMap = new ConcurrentHashMap<>();

    public void initializeItem(String itemId) {
        itemStateMap.put(itemId, ItemStatus.AVAILABLE);
    }

    public boolean updateStatus(String itemId, ItemStatus nextStatus) {
        ItemStatus current = itemStateMap.get(itemId);
        if (current == null) return false;

        if (!current.canTransitionTo(nextStatus)) {
            System.err.printf("Invalid transition: %s -> %s%n", current, nextStatus);
            return false;
        }

        itemStateMap.put(itemId, nextStatus);
        System.out.printf("Item %s: %s -> %s%n", itemId, current, nextStatus);
        return true;
    }

    public ItemStatus getStatus(String itemId) {
        return itemStateMap.get(itemId);
    }
}
