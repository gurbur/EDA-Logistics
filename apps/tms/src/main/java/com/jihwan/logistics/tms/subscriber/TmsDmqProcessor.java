package com.jihwan.logistics.tms.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.tms.config.SolaceSessionFactory;
import com.jihwan.logistics.tms.publisher.TmsEventPublisher;
import com.jihwan.logistics.tms.service.TruckAssignmentManager;
import com.solacesystems.jcsmp.*;

import java.util.Map;

public class TmsDmqProcessor {

    private final TruckAssignmentManager truckManager;
    private final TmsEventPublisher publisher;

    public TmsDmqProcessor(TruckAssignmentManager truckManager, TmsEventPublisher publisher) {
        this.truckManager = truckManager;
        this.publisher = publisher;
    }

    public void start() throws JCSMPException {
        JCSMPSession session = SolaceSessionFactory.createSession();
        session.connect();

        Queue dmq = JCSMPFactory.onlyInstance().createQueue("DMQ.JIHWAN.TMS");

        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(dmq);
        flowProps.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);

        FlowReceiver flow = session.createFlow(new XMLMessageListener() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public void onReceive(BytesXMLMessage message) {
                if (message instanceof TextMessage) {
                    try {
                        String text = ((TextMessage) message).getText();
                        Map<String, Object> payload = mapper.readValue(text, Map.class);

                        if (message.getDestination() instanceof Topic topic) {
                            String[] parts = topic.getName().split("/");
                            if (parts.length >= 6 && parts[2].equals("OMS") && parts[3].equals("ORDER_CREATED")) {
                                String region = parts[4];
                                String orderId = parts[5];

                                System.out.printf("♻️ [DMQ REPROCESS] TMS: 주문 %s (%s) 재처리 시도%n", orderId, region);

                                boolean assigned = truckManager.tryAssignTruck(orderId);
                                if (assigned) {
                                    publisher.publishAllocationResult(region, orderId, true, "[DMQ] 트럭 배정 성공");
                                } else {
                                    publisher.publishAllocationResult(region, orderId, false, "[DMQ] 사용 가능한 트럭 없음");
                                }
                            }
                        }

                        message.ackMessage(); // ✅ 수신 성공 시 ACK

                    } catch (Exception e) {
                        System.err.println("📛 [DMQ] TMS 재처리 중 오류: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onException(JCSMPException e) {
                System.err.println("📛 [DMQ] TMS 구독 오류: " + e.getMessage());
            }
        }, flowProps);

        flow.start();
        System.out.println("🧯 TMS DMQ Processor started for DMQ.JIHWAN.TMS");

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
