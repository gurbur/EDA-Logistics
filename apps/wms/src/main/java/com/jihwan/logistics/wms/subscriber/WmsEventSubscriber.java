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
    private final String region;

    public WmsEventSubscriber(WmsInventoryManager inventoryManager, WmsEventPublisher publisher, String region) {
        this.inventoryManager = inventoryManager;
        this.publisher = publisher;
        this.region = region.toUpperCase();  // 항상 대문자로
    }

    public void start() throws JCSMPException {
        JCSMPSession session = SolaceSessionFactory.createSession();
        session.connect();

        String queueName = "Q.JIHWAN.WMS." + region;
        Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);

        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(queue);
        flowProps.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);  // 수동 ACK

        FlowReceiver flow = session.createFlow(new XMLMessageListener() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public void onReceive(BytesXMLMessage message) {
                if (message instanceof TextMessage) {
                    String text = ((TextMessage) message).getText();
                    try {
                        Map<String, Object> payload = mapper.readValue(text, Map.class);
                        String orderId = (String) payload.get("order_id");
                        String itemId = (String) payload.get("item_id");

                        System.out.printf("[RECEIVED - %s] STOCK_CONFIRMED for order %s, item %s%n", region, orderId, itemId);

                        boolean picked = inventoryManager.pickItem(orderId, itemId);
                        if (picked) {
                            publisher.publishItemPicked(orderId, itemId);
                        }

                        message.ackMessage();  // ✅ 명시적 ACK

                    } catch (Exception e) {
                        System.err.println("Failed to process STOCK_CONFIRMED message: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onException(JCSMPException e) {
                System.err.println("Solace Flow error: " + e.getMessage());
            }
        }, flowProps);

        flow.start();
        System.out.printf("🏭 WMS FlowReceiver started for region [%s] on Queue [%s]%n", region, queueName);

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
