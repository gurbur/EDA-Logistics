package com.jihwan.logistics.lms.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.lms.config.SolaceSessionFactory;
import com.jihwan.logistics.lms.publisher.LmsEventPublisher;
import com.jihwan.logistics.lms.service.WorkerAssignmentManager;
import com.solacesystems.jcsmp.*;

import java.util.Map;

public class LmsEventSubscriber {

    private final WorkerAssignmentManager workerManager;
    private final LmsEventPublisher publisher;

    public LmsEventSubscriber(WorkerAssignmentManager workerManager, LmsEventPublisher publisher) {
        this.workerManager = workerManager;
        this.publisher = publisher;
    }

    public void start() throws JCSMPException {
        JCSMPSession session = SolaceSessionFactory.createSession();
        session.connect();

        final Topic orderTopic = JCSMPFactory.onlyInstance().createTopic("TOPIC/JIHWAN_LOGIS/ORDER/CREATED/>");

        XMLMessageConsumer consumer = session.getMessageConsumer(new XMLMessageListener() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public void onReceive(BytesXMLMessage message) {
                if (message instanceof TextMessage) {
                    try {
                        String text = ((TextMessage) message).getText();
                        Map<String, Object> payload = mapper.readValue(text, Map.class);
                        String orderId = (String) payload.get("order_id");

                        boolean assigned = workerManager.tryAssignWorker(orderId);
                        if (assigned) {
                            publisher.publishAllocationResult(orderId, "SUCCESS", "작업자 배정 성공");
                        } else {
                            publisher.publishAllocationResult(orderId, "FAILED", "할당 가능한 작업자 없음");
                        }

                    } catch (Exception e) {
                        System.err.println("LMS 메시지 처리 실패: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onException(JCSMPException e) {
                System.err.println("LMS 구독 중 오류 발생: " + e.getMessage());
            }
        });

        session.addSubscription(orderTopic);
        consumer.start();

        System.out.println("👷 LMS Subscriber started for ORDER_CREATED");
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }

    }
}
