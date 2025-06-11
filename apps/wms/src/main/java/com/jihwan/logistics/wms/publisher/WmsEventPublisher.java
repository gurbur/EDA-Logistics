package com.jihwan.logistics.wms.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.wms.config.SolaceSessionFactory;
import com.solacesystems.jcsmp.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class WmsEventPublisher {

    private final XMLMessageProducer producer;
    private final ObjectMapper mapper = new ObjectMapper();

    public WmsEventPublisher() throws JCSMPException {
        JCSMPSession session = SolaceSessionFactory.createSession();
        session.connect();

        producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
            @Override
            public void responseReceivedEx(Object correlationKey) {
                System.out.println("ACK received: " + correlationKey);
            }

            @Override
            public void handleErrorEx(Object correlationKey, JCSMPException exception, long timestamp) {
                System.err.printf("Failed to publish (%s) at %d: %s%n",
                        correlationKey,
                        timestamp,
                        exception != null ? exception.getMessage() : "Unknown error"
                );
            }
        });
    }

    public void publishItemPicked(String orderId, String itemId) {
        try {
            String topicStr = String.format("TOPIC/JIHWAN_LOGIS/WMS/ITEM_PICKED/%s/%s", orderId, itemId);
            Topic topic = JCSMPFactory.onlyInstance().createTopic(topicStr);

            Map<String, Object> payload = new HashMap<>();
            payload.put("order_id", orderId);
            payload.put("item_id", itemId);
            payload.put("timestamp", Instant.now().toString());

            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            msg.setText(mapper.writeValueAsString(payload));
            msg.setDeliveryMode(DeliveryMode.DIRECT);

            producer.send(msg, topic);
            System.out.printf("Published ITEM_PICKED to [%s]: %s%n", topicStr, msg.getText());
        } catch (Exception e) {
            System.err.println("Exception during ITEM_PICKED publish: " + e.getMessage());
        }
    }

    public void publishInitialInventory(String warehouseId, String itemId, int quantity) {
        try {
            String topicStr = String.format("TOPIC/JIHWAN_LOGIS/WMS/INVENTORY/INIT/%s/%s",
                    warehouseId.toUpperCase(), itemId);
            Topic topic = JCSMPFactory.onlyInstance().createTopic(topicStr);

            Map<String, Object> payload = new HashMap<>();
            payload.put("warehouse_id", warehouseId);
            payload.put("item_id", itemId);
            payload.put("quantity", quantity);
            payload.put("timestamp", Instant.now().toString());

            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            msg.setText(mapper.writeValueAsString(payload));
            msg.setDeliveryMode(DeliveryMode.DIRECT);

            producer.send(msg, topic);
            System.out.printf("Published INVENTORY INIT to [%s]: %s%n", topicStr, msg.getText());
        } catch (Exception e) {
            System.err.println("Exception during INVENTORY INIT publish: " + e.getMessage());
        }
    }

}
