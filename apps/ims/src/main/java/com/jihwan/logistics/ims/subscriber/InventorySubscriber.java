package com.jihwan.logistics.ims.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.ims.config.SolaceSessionFactory;
import com.jihwan.logistics.ims.service.InventoryManager;
import com.solacesystems.jcsmp.*;

import java.util.Map;

public class InventorySubscriber {

    private final InventoryManager inventoryManager;

    public InventorySubscriber(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
    }

    public void start() throws JCSMPException {
        JCSMPSession session = SolaceSessionFactory.createSession();
        session.connect();

        final Topic topic = JCSMPFactory.onlyInstance()
                .createTopic("TOPIC/JIHWAN_LOGIS/ORDER/CREATED/>");

        XMLMessageConsumer consumer = session.getMessageConsumer(new XMLMessageListener() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public void onReceive(BytesXMLMessage message) {
                if (message instanceof TextMessage) {
                    String text = ((TextMessage) message).getText();
                    try {
                        Map<String, Object> payload = mapper.readValue(text, Map.class);
                        String orderId = (String) payload.get("order_id");
                        String itemId = (String) payload.get("item_id");
                        String destination = (String) payload.get("destination");

                        boolean inStock = inventoryManager.hasStock(destination, itemId);

                        System.out.printf("Order %s - Item %s @%s -> Stock %s%n",
                                orderId, itemId, destination, inStock ? "PRESENT" : "OUT_OF_STOCK");
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

        session.addSubscription(topic);
        consumer.start();
        System.out.println("Solace Subscriber started for ORDER_CREATED");

        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException _ignored) {

            }
        }
    }
}
