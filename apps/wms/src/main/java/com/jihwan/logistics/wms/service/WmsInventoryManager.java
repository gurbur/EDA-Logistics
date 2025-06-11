package com.jihwan.logistics.wms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.wms.config.InventoryInitData;
import com.jihwan.logistics.wms.domain.ItemStatus;
import com.jihwan.logistics.wms.domain.PackedItem;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WmsInventoryManager {

    private final Map<String, Integer> itemStockMap = new ConcurrentHashMap<>();
    private final Map<String, PackedItem> itemAssignmentMap = new ConcurrentHashMap<>();
    private String warehouseId;

    public WmsInventoryManager(String configPath) {
        initializeFromFile(configPath);
    }

    private void initializeFromFile(String configPath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InventoryInitData initData = mapper.readValue(new File(configPath), InventoryInitData.class);
            this.warehouseId = initData.warehouse_id;
            itemStockMap.putAll(initData.items);
            System.out.printf("[INIT] WMS for [%s] loaded: %s%n", warehouseId, itemStockMap);
        } catch (Exception e) {
            System.err.printf("Failed to initialize inventory from [%s]: %s%n", configPath, e.getMessage());
            this.warehouseId = "UNKNOWN";
        }
    }

    public synchronized boolean pickItem(String orderId, String itemId) {
        int stock = itemStockMap.getOrDefault(itemId, 0);
        if (stock > 0) {
            itemStockMap.put(itemId, stock - 1);
            itemAssignmentMap.put(orderId, new PackedItem(itemId));
            System.out.printf("[PICKED] %s for Order %s (남은 수량: %d)%n", itemId, orderId, stock - 1);
            return true;
        }
        System.err.printf("[PICK FAILED] 재고 부족: %s%n", itemId);
        return false;
    }

    public synchronized boolean packItem(String orderId) {
        PackedItem assigned = itemAssignmentMap.get(orderId);
        if (assigned != null && assigned.getStatus() == ItemStatus.PICKED) {
            assigned.pack();
            System.out.printf("[PACKED] %s (%s)%n", orderId, assigned.getItemId());
            return true;
        }
        System.err.printf("[PACK FAILED] %s - 상태 오류 또는 없음%n", orderId);
        return false;
    }

    public void printStock() {
        System.out.println("=== 재고 ===");
        itemStockMap.forEach((k, v) -> System.out.printf("%s: %d개%n", k, v));
    }

    public void printAssignments() {
        System.out.println("=== 할당 상태 ===");
        itemAssignmentMap.forEach((k, v) -> System.out.printf("Order %s -> %s [%s]%n",
                k, v.getItemId(), v.getStatus()));
    }

    public Map<String, Integer> getInventorySnapshot() {
        return new ConcurrentHashMap<>(itemStockMap);
    }

    public String getWarehouseId() {
        return warehouseId;
    }
}
