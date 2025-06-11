package com.jihwan.logistics.wms.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.wms.config.SolaceSessionFactory;
import com.jihwan.logistics.wms.publisher.WmsEventPublisher;
import com.jihwan.logistics.wms.service.WmsInventoryManager;
import com.solacesystems.jcsmp.*;

import java.util.Map;

public class WmsEventSubscriber {

    private final WmsInventoryManager inventoryManager;
    private final WmsEventPublisher publisher;

    public WmsEventSubscriber(WmsInventoryManager inventoryManager, WmsEventPublisher publisher) {
        this.inventoryManager = inventoryManager;
        this.publisher = publisher;
    }

    public void start() throws JCSMPException {
        JCSMPSession session = SolaceSessionFactory.createSession();
        session.connect();

        final Topic stockConfirmedTopic = JCSMPFactory.onlyInstance()
                .createTopic("TOPIC/JIHWAN_LOGIS/IMS/STOCK_CONFIRMED/>");

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

                        System.out.printf("[RECEIVED] STOCK_CONFIRMED for order %s, item %s%n", orderId, itemId);

                        boolean picked = inventoryManager.pickItem(orderId, itemId);
                        if (picked) {
                            publisher.publishItemPicked(orderId, itemId);
                        }

                    } catch (Exception e) {
                        System.err.println("Failed to process STOCK_CONFIRMED: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onException(JCSMPException e) {
                System.err.println("Solace receive error: " + e.getMessage());
            }
        });

        session.addSubscription(stockConfirmedTopic);
        consumer.start();

        System.out.println("WMS Subscriber started for STOCK_CONFIRMED");

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
