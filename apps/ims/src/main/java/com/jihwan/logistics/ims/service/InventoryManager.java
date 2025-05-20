package com.jihwan.logistics.ims.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryManager {
    private final Map<String, Map<String, Integer>> inventoryMap = new ConcurrentHashMap<>();

    public InventoryManager () {
        initializeSampleInventory();
    }

    private void initializeSampleInventory() {
        Map<String, Integer> seoulWarehouse = new ConcurrentHashMap<>();
        seoulWarehouse.put("itemA", 5);
        seoulWarehouse.put("itemB", 3);

        Map<String, Integer> busanWarehouse = new ConcurrentHashMap<>();
        busanWarehouse.put("itemA", 0);
        busanWarehouse.put("itemC", 7);

        inventoryMap.put("SEOUL", seoulWarehouse);
        inventoryMap.put("BUSAN", busanWarehouse);
    }

    public boolean hasStock(String warehouseId, String itemId) {
        Map<String, Integer> warehouse = inventoryMap.get(warehouseId);
        if (warehouse == null) return false;
        return warehouse.getOrDefault(itemId, 0) > 0;
    }

    public synchronized boolean decrementStock(String warehouseId, String itemId) {
        Map<String, Integer> warehouse = inventoryMap.get(warehouseId);
        if (warehouse == null) return false;

        int qty = warehouse.getOrDefault(itemId, 0);
        if (qty > 0) {
            warehouse.put(itemId, qty - 1);
            return true;
        }
        return false;
    }

    public void printInventory() {
        System.out.println("현재 재고 상태:");
        for (String warehouse : inventoryMap.keySet()) {
            System.out.println("[" + warehouse + "]" + inventoryMap.get(warehouse));
        }
    }
}
