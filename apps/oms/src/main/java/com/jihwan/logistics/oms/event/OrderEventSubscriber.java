package com.jihwan.logistics.oms.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.oms.services.OrderService;
import com.solacesystems.jcsmp.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventSubscriber {

    private final OrderService orderService;
    private final ObjectMapper mapper = new ObjectMapper();

    private JCSMPSession session;
    private XMLMessageConsumer consumer;

    @PostConstruct
    public void init() {
        try {
            // Solace 설정
            JCSMPProperties props = new JCSMPProperties();
            props.setProperty(JCSMPProperties.HOST, System.getenv("SOLACE_HOST"));
            props.setProperty(JCSMPProperties.VPN_NAME, System.getenv("SOLACE_VPN"));
            props.setProperty(JCSMPProperties.USERNAME, System.getenv("SOLACE_USER"));
            props.setProperty(JCSMPProperties.PASSWORD, System.getenv("SOLACE_PASS"));
            props.setBooleanProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);

            // 세션 및 소비자 생성
            session = JCSMPFactory.onlyInstance().createSession(props);
            session.connect();

            consumer = session.getMessageConsumer(new XMLMessageListener() {
                @Override
                public void onReceive(BytesXMLMessage message) {
                    if (message instanceof TextMessage) {
                        try {
                            String text = ((TextMessage) message).getText();
                            Map<String, Object> payload = mapper.readValue(text, Map.class);
                            String orderId = (String) payload.get("order_id");

                            if (message.getDestination() instanceof Topic topic) {
                                String topicName = topic.getName();
                                if (topicName.contains("STOCK_CONFIRMED")) {
                                    orderService.confirmOrder(orderId);
                                    log.info("Confirmed order: {}", orderId);
                                } else if (topicName.contains("STOCK_INSUFFICIENT")) {
                                    orderService.failOrder(orderId);
                                    log.warn("Failed to confirm order due to insufficient stock: {}", orderId);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to process stock event: {}", e.getMessage());
                        }
                    }
                }

                @Override
                public void onException(JCSMPException e) {
                    log.error("Subscriber error: {}", e.getMessage());
                }
            });

            // Topic 구독
            session.addSubscription(JCSMPFactory.onlyInstance()
                    .createTopic("TOPIC/JIHWAN_LOGIS/STOCK/>"));

            consumer.start();
            log.info("Started subscription to STOCK topics");
        } catch (JCSMPException e) {
            log.error("Failed to initialize OrderEventSubscriber:", e);
        }
    }
}
