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

        Queue queue = JCSMPFactory.onlyInstance().createQueue("Q.JIHWAN.LMS");

        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(queue);
        flowProps.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);

        FlowReceiver flow = session.createFlow(new XMLMessageListener() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public void onReceive(BytesXMLMessage message) {
                if (message instanceof TextMessage) {
                    String text = ((TextMessage) message).getText();
                    try {
                        Map<String, Object> payload = mapper.readValue(text, Map.class);

                        if (message.getDestination() instanceof Topic topic) {
                            String[] parts = topic.getName().split("/");
                            if (parts.length >= 6 && parts[2].equals("OMS") && parts[3].equals("ORDER_CREATED")) {
                                String region = parts[4];
                                String orderId = parts[5];

                                boolean assigned = workerManager.tryAssignWorker(orderId);
                                if (assigned) {
                                    publisher.publishAllocationResult(region, orderId, true, "작업자 배정 성공");
                                } else {
                                    publisher.publishAllocationResult(region, orderId, false, "할당 가능한 작업자 없음");
                                }
                            }
                        }

                        message.ackMessage(); // ✅ 수신 성공 시 명시적 ACK

                    } catch (Exception e) {
                        System.err.println("LMS 메시지 처리 실패: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onException(JCSMPException e) {
                System.err.println("LMS 구독 중 오류 발생: " + e.getMessage());
            }
        }, flowProps);

        flow.start();
        System.out.println("👷 LMS FlowReceiver started for Q.JIHWAN.LMS");

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
