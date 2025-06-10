package com.jihwan.logistics.ims.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.ims.config.SolaceSessionFactory;
import com.jihwan.logistics.ims.publisher.InventoryPublisher;
import com.jihwan.logistics.ims.service.InventoryManager;
import com.solacesystems.jcsmp.*;

import java.util.List;
import java.util.Map;

public class InventorySubscriber {

    private final InventoryManager inventoryManager;
    private final InventoryPublisher inventoryPublisher;

    public InventorySubscriber(InventoryManager inventoryManager, InventoryPublisher inventoryPublisher) {
        this.inventoryManager = inventoryManager;
        this.inventoryPublisher = inventoryPublisher;
    }

    public void start() throws JCSMPException {
        JCSMPSession session = SolaceSessionFactory.createSession();
        session.connect();

        final Topic orderCreatedTopic = JCSMPFactory.onlyInstance()
                .createTopic("TOPIC/JIHWAN_LOGIS/ORDER/CREATED/>");

        final Topic itemPackedTopic = JCSMPFactory.onlyInstance()
                .createTopic("TOPIC/JIHWAN_LOGIS/PACKING/ITEM_PACKED/>");

        XMLMessageConsumer consumer = session.getMessageConsumer(new XMLMessageListener() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public void onReceive(BytesXMLMessage message) {
                if (message instanceof TextMessage) {
                    String text = ((TextMessage) message).getText();
                    try {
                        Map<String, Object> payload = mapper.readValue(text, Map.class);

                        if (message.getDestination() instanceof Topic topic) {
                            String topicStr = topic.getName();

                            if (topicStr.contains("ORDER/CREATED")) {
                                String orderId = (String) payload.get("order_id");
                                String itemId = (String) payload.get("item_id");
                                String destination = (String) payload.get("destination");

                                boolean inStock = inventoryManager.hasStock(destination, itemId);

                                if (inStock) {
                                    int remainingQty = inventoryManager.getStock(destination, itemId);
                                    inventoryPublisher.publishStockEvent("CONFIRMED", destination, orderId, itemId, remainingQty);
                                } else {
                                    inventoryPublisher.publishStockInsufficient(orderId, itemId, List.of(destination));
                                }
                                System.out.printf("Order %s - Item %s @%s -> Stock %s%n",
                                        orderId, itemId, destination, inStock ? "PRESENT" : "OUT_OF_STOCK");

                            } else if (topicStr.contains("PACKING/ITEM_PACKED")) {
                                String orderId = (String) payload.get("order_id");
                                String itemId = (String) payload.get("item_id");
                                String warehouseId = (String) payload.get("warehouse_id");

                                boolean result = inventoryManager.decrementStock(warehouseId, itemId);
                                if (result) {
                                    int remain = inventoryManager.getStock(warehouseId, itemId);
                                    System.out.printf("[PACKED] 재고 차감됨: %s - %s @%s (남은 수량: %d)%n",
                                            orderId, itemId, warehouseId, remain);
                                } else {
                                    System.err.printf("[PACKED] 차감 실패: 재고 없음 (%s - %s @%s)%n",
                                            orderId, itemId, warehouseId);
                                }
                            }
                        }

                    } catch (Exception e) {
                        System.err.println("Failed to parse JSON: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onException(JCSMPException e) {
                System.err.println("Solace receive error: " + e.getMessage());
            }
        });

        session.addSubscription(orderCreatedTopic);
        session.addSubscription(itemPackedTopic);
        consumer.start();

        System.out.println("Solace Subscriber started for ORDER_CREATED and ITEM_PACKED");

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

}
