package com.jihwan.logistics.ims;

import com.jihwan.logistics.ims.service.InventoryManager;
import com.jihwan.logistics.ims.subscriber.InventorySubscriber;

public class InventoryApplication {
    public static void main (String[] args) throws Exception {
        InventoryManager manager = new InventoryManager();
        InventorySubscriber subscriber = new InventorySubscriber(manager);
        manager.printInventory();
        subscriber.start();
    }
}
