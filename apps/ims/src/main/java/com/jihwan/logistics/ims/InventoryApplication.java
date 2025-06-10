package com.jihwan.logistics.ims;

import com.jihwan.logistics.ims.publisher.InventoryPublisher;
import com.jihwan.logistics.ims.service.InventoryManager;
import com.jihwan.logistics.ims.subscriber.InventorySubscriber;

public class InventoryApplication {
    public static void main (String[] args) throws Exception {
        InventoryManager manager = new InventoryManager();
        InventoryPublisher publisher = new InventoryPublisher();
        InventorySubscriber subscriber = new InventorySubscriber(manager, publisher);
        manager.printInventory();
        subscriber.start();
    }
}
