package com.jihwan.logistics.oms.event;

import com.jihwan.logistics.oms.utils.JsonUtil;
import com.solacesystems.jcsmp.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class OrderEventPublisher {
    private JCSMPSession session;
    private XMLMessageProducer producer;

    @PostConstruct
    public void init() {
        try {
            JCSMPProperties props = new JCSMPProperties();
            props.setProperty(JCSMPProperties.HOST, System.getenv("SOLACE_HOST"));
            props.setProperty(JCSMPProperties.VPN_NAME, System.getenv("SOLACE_VPN"));
            props.setProperty(JCSMPProperties.USERNAME, System.getenv("SOLACE_USER"));
            props.setProperty(JCSMPProperties.PASSWORD, System.getenv("SOLACE_PASS"));
            props.setBooleanProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);

            session = JCSMPFactory.onlyInstance().createSession(props);
            session.connect();
            producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
                @Override
                public void responseReceivedEx(Object correlationKey) {
                    log.info("ACK received (correlationKey={})", correlationKey);
                }

                @Override
                public void handleErrorEx(Object correlationKey, JCSMPException e, long timestamp) {
                    log.error("Failed to publish (correlationKey={}) at {}: {}", correlationKey, timestamp, e.getMessage());
                }
            });

            log.info("Solace JCSMP session connected");
        } catch (JCSMPException e) {
            log.error("Failed to initialize Solace JCSMP session:", e);
        }
    }

    public void publishOrderCreated(Map<String, Object> payload, String region, String orderId) {
        publishToTopic("CREATED", payload, region, orderId);
    }

    public void publishOrderConfirmed(Map<String, Object> payload, String region, String orderId) {
        publishToTopic("CONFIRMED", payload, region, orderId);
    }

    public void publishOrderFailed(Map<String, Object> payload, String region, String orderId) {
        publishToTopic("FAILED", payload, region, orderId);
    }

    private void publishToTopic(String eventType, Map<String, Object> payload, String region, String orderId) {
        try {
            String topicStr = String.format("TOPIC/JIHWAN_LOGIS/ORDER/%s/%s/%s",
                    eventType.toUpperCase(), region.toUpperCase(), orderId);
            Topic topic = JCSMPFactory.onlyInstance().createTopic(topicStr);

            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            msg.setText(JsonUtil.toJson(payload));
            msg.setDeliveryMode(DeliveryMode.DIRECT);

            producer.send(msg, topic);
            log.info("Published to Topic [{}]: {}", topicStr, msg.getText());
        } catch (JCSMPException e) {
            log.error("Failed to publish ORDER_{}: {}", eventType, e.getMessage());
        }
    }

    public void publishOrderDelivered(Map<String, Object> payload, String region, String orderId) {
        publishToTopic("DELIVERED", payload, region, orderId);
    }

    public void publishOrderDispatched(Map<String, Object> payload, String region, String orderId) {
        publishToTopic("DISPATCHED", payload, region, orderId);
    }

}
