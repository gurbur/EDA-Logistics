package com.jihwan.logistics.simulator.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.simulator.config.SolaceSessionFactory;
import com.jihwan.logistics.simulator.simulator.SimulatorDispatcher;
import com.solacesystems.jcsmp.*;

import java.util.Map;

public class SimulatorEventSubscriber {

    private final SimulatorDispatcher dispatcher;

    public SimulatorEventSubscriber(SimulatorDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void start() throws JCSMPException {
        JCSMPSession session = SolaceSessionFactory.createSession();
        session.connect();

        String queueName = "Q.JIHWAN.SIMULATOR";
        Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);

        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(queue);
        flowProps.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);

        FlowReceiver flow = session.createFlow(new XMLMessageListener() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public void onReceive(BytesXMLMessage message) {
                try {
                    String text;
                    if (message instanceof TextMessage) {
                        text = ((TextMessage) message).getText();
                    } else {
                        byte[] bytes = message.getAttachmentByteBuffer().array();
                        text = new String(bytes); // binary payload 처리
                    }
                    Map<String, Object> payload = mapper.readValue(text, Map.class);

                    String topic = message.getDestination().getName(); // e.g., TOPIC/JIHWAN_LOGIS/TRUCK/ALLOC/O123
                    String[] tokens = topic.split("/");

                    if (tokens.length < 5 || !"TOPIC".equals(tokens[0]) || !"JIHWAN_LOGIS".equals(tokens[1])) {
                        System.err.printf("⚠️ [INVALID TOPIC FORMAT] %s%n", topic);
                        return;
                    }

                    String service = tokens[2];
                    String event = tokens[3];
                    String orderId = tokens[4];

                    switch (service + "/" + event) {
                        case "WORKER/ALLOC" -> {
                            String workerId = (String) payload.get("worker_id");
                            System.out.printf("🚶 [WORKER_ALLOC] %s assigned to order %s%n", workerId, orderId);
                            dispatcher.onWorkerAllocated(orderId, workerId);
                        }
                        case "TRUCK/ALLOC" -> {
                            String truckId = (String) payload.get("truck_id");
                            System.out.printf("🚚 [TRUCK_ALLOC] %s assigned to order %s%n", truckId, orderId);
                            dispatcher.onTruckAllocated(orderId, truckId);
                        }
                        case "DISPATCH/START" -> {
                            String source = (String) payload.get("source");
                            String destination = (String) payload.get("destination");
                            System.out.printf("📦 [DISPATCH_START] Order %s from %s to %s%n", orderId, source, destination);
                            dispatcher.onDispatchStart(orderId, source, destination);
                        }
                        default -> System.err.printf("⚠️ [UNKNOWN SERVICE/EVENT] %s%n", service + "/" + event);
                    }

                    message.ackMessage();

                } catch (Exception e) {
                    System.err.println("❌ Failed to process SIMULATOR message: " + e.getMessage());
                }

            }

            @Override
            public void onException(JCSMPException e) {
                System.err.println("❌ Solace Flow error: " + e.getMessage());
            }
        }, flowProps);

        flow.start();
        System.out.printf("🛰️ SIMULATOR FlowReceiver started on Queue [%s]%n", queueName);

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
