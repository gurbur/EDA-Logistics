package com.jihwan.logistics.ims.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryManager {
    private final Map<String, Map<String, Integer>> inventoryMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> warehouseLocationMap = new HashMap<>();
    private final List<String> worldMap = Arrays.asList(
            "SEOUL", "INCHEON", "DAEJEON", "GWANGJU", "BUSAN", "ULSAN",
            "DAEGU", "JEONJU", "CHUNCHEON", "SUWON", "CHEONGJU", "JEJU"
    );

    public InventoryManager () {}

    public synchronized void updateInventory(String warehouseId, String itemId, int quantity) {
        inventoryMap.computeIfAbsent(warehouseId.toUpperCase(), k -> new ConcurrentHashMap<>())
                .put(itemId, quantity);
        warehouseLocationMap.putIfAbsent(warehouseId.toUpperCase(), worldMap.indexOf(warehouseId.toUpperCase()));

        System.out.printf("[UPDATED] %s - %s = %d개%n", warehouseId, itemId, quantity);
        printInventory();
    }

    public boolean hasStock(String warehouseId, String itemId) {
        Map<String, Integer> warehouse = inventoryMap.get(warehouseId);
        if (warehouse == null) return false;
        return warehouse.getOrDefault(itemId, 0) > 0;
    }

    public int getStock(String warehouseId, String itemId) {
        Map<String, Integer> warehouse = inventoryMap.get(warehouseId);
        if (warehouse == null) return 0;
        return warehouse.getOrDefault(itemId, 0);
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

    public String findNearestWarehouse(String destination, String itemId) {
        if (!worldMap.contains(destination)) return null;

        int destIdx = worldMap.indexOf(destination);
        int ringSize = worldMap.size();

        for (int offset = 0; offset <= ringSize / 2; offset++) {
            int clockwise = (destIdx + offset) % ringSize;
            int counterClockwise = (destIdx - offset + ringSize) % ringSize;

            for (int idx : List.of(clockwise, counterClockwise)) {
                String candidate = worldMap.get(idx);
                if (hasStock(candidate, itemId)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public void printInventory() {
        System.out.println("현재 재고 상태:");
        for (String warehouse : inventoryMap.keySet()) {
            System.out.println("[" + warehouse + "]" + inventoryMap.get(warehouse));
        }
    }
}
