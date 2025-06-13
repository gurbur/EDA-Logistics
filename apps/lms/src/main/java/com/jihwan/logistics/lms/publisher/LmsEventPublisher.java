package com.jihwan.logistics.lms.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.lms.config.SolaceSessionFactory;
import com.solacesystems.jcsmp.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class LmsEventPublisher {

    private final XMLMessageProducer producer;
    private final ObjectMapper mapper = new ObjectMapper();

    public LmsEventPublisher() throws JCSMPException {
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

    public void publishAllocationResult(String region, String orderId, boolean success, String reason) {
        try {
            String topicStr = String.format("TOPIC/JIHWAN_LOGIS/LMS/ALLOCATION_RESULT/%s/%s",
                    region.toUpperCase(), orderId);
            Topic topic = JCSMPFactory.onlyInstance().createTopic(topicStr);

            Map<String, Object> payload = new HashMap<>();
            payload.put("order_id", orderId);
            payload.put("result", success ? "SUCCESS" : "FAILED");
            payload.put("reason", reason);
            payload.put("timestamp", Instant.now().toString());

            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            msg.setText(mapper.writeValueAsString(payload));
            msg.setDeliveryMode(DeliveryMode.DIRECT);

            producer.send(msg, topic);
            System.out.printf("[LMS ALLOCATION %s] Published to [%s]: %s%n",
                    success ? "SUCCESS" : "FAILED", topicStr, msg.getText());
        } catch (Exception e) {
            System.err.println("Exception during LMS allocation publish: " + e.getMessage());
        }
    }

}
