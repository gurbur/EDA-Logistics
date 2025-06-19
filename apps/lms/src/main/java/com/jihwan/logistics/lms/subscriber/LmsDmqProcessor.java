package com.jihwan.logistics.lms.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.lms.config.SolaceSessionFactory;
import com.jihwan.logistics.lms.publisher.LmsEventPublisher;
import com.jihwan.logistics.lms.service.WorkerAssignmentManager;
import com.solacesystems.jcsmp.*;

import java.util.Map;

public class LmsDmqProcessor {

    private final WorkerAssignmentManager workerManager;
    private final LmsEventPublisher publisher;

    public LmsDmqProcessor(WorkerAssignmentManager workerManager, LmsEventPublisher publisher) {
        this.workerManager = workerManager;
        this.publisher = publisher;
    }

    public void start() throws JCSMPException {
        JCSMPSession session = SolaceSessionFactory.createSession();
        session.connect();

        Queue dmq = JCSMPFactory.onlyInstance().createQueue("DMQ.JIHWAN.LMS");

        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(dmq);
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

                                System.out.printf("♻️ [DMQ REPROCESS] %s - %s 재처리 시도%n", region, orderId);

                                boolean assigned = workerManager.tryAssignWorker(orderId);
                                if (assigned) {
                                    publisher.publishAllocationResult(region, orderId, true, "[DMQ] 작업자 재배정 성공");
                                } else {
                                    publisher.publishAllocationResult(region, orderId, false, "[DMQ] 재시도 실패: 작업자 부족");
                                }
                            }
                        }

                        message.ackMessage(); // ✅ ACK on success

                    } catch (Exception e) {
                        System.err.println("📛 [DMQ] LMS 재처리 중 예외 발생: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onException(JCSMPException e) {
                System.err.println("📛 [DMQ] LMS 구독 오류: " + e.getMessage());
            }
        }, flowProps);

        flow.start();
        System.out.println("🧯 LMS DMQ Processor started for DMQ.JIHWAN.LMS");

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
