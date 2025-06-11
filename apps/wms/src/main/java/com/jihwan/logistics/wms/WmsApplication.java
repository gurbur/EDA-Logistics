package com.jihwan.logistics.wms;

import com.jihwan.logistics.wms.publisher.WmsEventPublisher;
import com.jihwan.logistics.wms.service.WmsInventoryManager;
import com.jihwan.logistics.wms.subscriber.WmsEventSubscriber;

public class WmsApplication {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar WmsApp.jar <inventory-config.json>");
            System.exit(1);
        }

        String configPath = args[0];

        try {
            WmsInventoryManager inventoryManager = new WmsInventoryManager(configPath);
            WmsEventPublisher publisher = new WmsEventPublisher();

            inventoryManager.getInventorySnapshot().forEach((itemId, qty) -> {
                publisher.publishInitialInventory(
                        inventoryManager.getWarehouseId(),
                        itemId,
                        qty
                );
            });

            WmsEventSubscriber subscriber = new WmsEventSubscriber(inventoryManager, publisher);
            subscriber.start();
        } catch (Exception e) {
            System.err.println("WMS Application failed to start: " + e.getMessage());
        }
    }
}
