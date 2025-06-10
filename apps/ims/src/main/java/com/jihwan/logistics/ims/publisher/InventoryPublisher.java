package com.jihwan.logistics.ims.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.ims.config.SolaceSessionFactory;
import com.solacesystems.jcsmp.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryPublisher {

    private final XMLMessageProducer producer;
    private final ObjectMapper mapper = new ObjectMapper();

    public InventoryPublisher() throws JCSMPException {

        JCSMPSession session = SolaceSessionFactory.createSession();
        session.connect();
        producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
            @Override
            public void responseReceivedEx(Object correlationKey) {
                System.out.println("ACK received: " + correlationKey);
            }

            @Override
            public void handleErrorEx(Object correlationKey, JCSMPException exception, long timestamp) {
                System.err.println("Failed to publish (" + correlationKey + "): " + exception.getMessage());
            }
        });
    }

    public void publishStockEvent(String eventType, String warehouseId, String orderId, String itemId, int quantity) {
        try {
            String topicStr = String.format(
                    "TOPIC/JIHWAN_LOGIS/STOCK/%s/%s/%s",
                    eventType.toUpperCase(), warehouseId.toUpperCase(), orderId
            );
            Topic topic = JCSMPFactory.onlyInstance().createTopic(topicStr);

            Map<String, Object> payload = new HashMap<>();
            payload.put("order_id", orderId);
            payload.put("item_id", itemId);
            payload.put("warehouse_id", warehouseId);
            payload.put("quantity", quantity);
            payload.put("timestamp", Instant.now().toString());

            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            msg.setText(mapper.writeValueAsString(payload));
            msg.setDeliveryMode(DeliveryMode.DIRECT);

            producer.send(msg, topic);
            System.out.printf("Published to [%s]: %s%n", topicStr, msg.getText());
        } catch (Exception e) {
            System.err.println("Exception during stock event publish: " + e.getMessage());
        }
    }

    public void publishStockInsufficient(String orderId, String itemId, List<String> checkedRegions) {
        try {
            String topicStr = String.format("TOPIC/JIHWAN_LOGIS/STOCK/INSUFFICIENT/NULL/%s", orderId);
            Topic topic = JCSMPFactory.onlyInstance().createTopic(topicStr);

            Map<String, Object> payload = new HashMap<>();
            payload.put("order_id", orderId);
            payload.put("item_id", itemId);
            payload.put("regions_checked", checkedRegions);
            payload.put("timestamp", Instant.now().toString());

            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            msg.setText(mapper.writeValueAsString(payload));
            msg.setDeliveryMode(DeliveryMode.DIRECT);

            producer.send(msg, topic);
            System.out.printf("Published INSUFFICIENT to [%s]: %s%n", topicStr, msg.getText());
        } catch (Exception e) {
            System.err.println("Exception during insufficient stock publish: " + e.getMessage());
        }
    }
}
