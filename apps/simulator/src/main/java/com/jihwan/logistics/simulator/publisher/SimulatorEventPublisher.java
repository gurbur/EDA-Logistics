package com.jihwan.logistics.simulator.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.simulator.config.SolaceSessionFactory;
import com.solacesystems.jcsmp.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class SimulatorEventPublisher {

    private final XMLMessageProducer producer;
    private final ObjectMapper mapper = new ObjectMapper();

    public SimulatorEventPublisher() throws JCSMPException {
        JCSMPSession session = SolaceSessionFactory.createSession();
        session.connect();

        this.producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
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

    public void publishStatus(String orderId, String currentLocation) {
        try {
            String topicStr = String.format("TOPIC/JIHWAN_LOGIS/SIMULATOR/STATUS/%s", orderId);
            Topic topic = JCSMPFactory.onlyInstance().createTopic(topicStr);

            Map<String, Object> payload = new HashMap<>();
            payload.put("order_id", orderId);
            payload.put("location", currentLocation);
            payload.put("timestamp", Instant.now().toString());

            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            msg.setText(mapper.writeValueAsString(payload));
            msg.setDeliveryMode(DeliveryMode.DIRECT);

            producer.send(msg, topic);
            System.out.printf("Published STATUS to [%s]: %s%n", topicStr, msg.getText());
        } catch (Exception e) {
            System.err.println("Exception during STATUS publish: " + e.getMessage());
        }
    }

    public void publishResourceReturned(String orderId, String workerId, String truckId) {
        try {
            String topicStr = String.format("TOPIC/JIHWAN_LOGIS/SIMULATOR/RESOURCE_RETURNED/%s", orderId);
            Topic topic = JCSMPFactory.onlyInstance().createTopic(topicStr);

            Map<String, Object> payload = new HashMap<>();
            payload.put("order_id", orderId);
            payload.put("worker_id", workerId);
            payload.put("truck_id", truckId);
            payload.put("timestamp", Instant.now().toString());

            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            msg.setText(mapper.writeValueAsString(payload));
            msg.setDeliveryMode(DeliveryMode.DIRECT);

            producer.send(msg, topic);
            System.out.printf("Published RESOURCE_RETURNED to [%s]: %s%n", topicStr, msg.getText());
        } catch (Exception e) {
            System.err.println("Exception during RESOURCE_RETURNED publish: " + e.getMessage());
        }
    }
}
