package com.jihwan.logistics.ims.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihwan.logistics.ims.config.SolaceSessionFactory;
import com.jihwan.logistics.ims.publisher.InventoryPublisher;
import com.jihwan.logistics.ims.service.InventoryManager;
import com.solacesystems.jcsmp.*;

import java.util.List;
import java.util.Map;

public class InventorySubscriber {

    private final InventoryManager inventoryManager;
    private final InventoryPublisher inventoryPublisher;

    public InventorySubscriber(InventoryManager inventoryManager, InventoryPublisher inventoryPublisher) {
        this.inventoryManager = inventoryManager;
        this.inventoryPublisher = inventoryPublisher;
    }

    public void start() throws JCSMPException {
        JCSMPSession session = SolaceSessionFactory.createSession();
        session.connect();

        Queue queue = JCSMPFactory.onlyInstance().createQueue("Q.JIHWAN.IMS");

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
                            String topicStr = topic.getName();
                            String[] parts = topicStr.split("/");

                            if (parts.length >= 6 && parts[2].equals("OMS") && parts[3].equals("ORDER_CREATED")) {
                                String region = parts[4];
                                String orderId = parts[5];
                                String itemId = (String) payload.get("item_id");

                                String assignedWarehouse = inventoryManager.findNearestWarehouse(region, itemId);

                                if (assignedWarehouse != null) {
                                    int remainingQty = inventoryManager.getStock(assignedWarehouse, itemId);
                                    inventoryPublisher.publishStockEvent(region, orderId, itemId, remainingQty, true, "재고 확인 및 배정 성공");
                                } else {
                                    inventoryPublisher.publishStockEvent(region, orderId, itemId, 0, false, "모든 창고에서 재고 부족");
                                }

                            } else if (parts.length >= 6 && parts[2].equals("WMS") && parts[3].equals("ITEM_PACKED")) {
                                String orderId = (String) payload.get("order_id");
                                String itemId = (String) payload.get("item_id");
                                String warehouseId = (String) payload.get("warehouse_id");

                                boolean result = inventoryManager.decrementStock(warehouseId, itemId);
                                if (result) {
                                    int remain = inventoryManager.getStock(warehouseId, itemId);
                                    System.out.printf("[PACKED] 재고 차감됨: %s - %s @%s (남은 수량: %d)%n",
                                            orderId, itemId, warehouseId, remain);
                                } else {
                                    System.err.printf("[PACKED] 차감 실패: 재고 없음 (%s - %s @%s)%n",
                                            orderId, itemId, warehouseId);
                                }

                            } else if (parts.length >= 5 && parts[2].equals("WMS") && parts[3].equals("INVENTORY_INIT")) {
                                String warehouseId = parts[4];
                                Map<String, Integer> stockMap = (Map<String, Integer>) payload.get("stock");

                                if (warehouseId != null && stockMap != null) {
                                    stockMap.forEach((itemId, quantity) ->
                                            inventoryManager.updateInventory(warehouseId, itemId, quantity));
                                    System.out.printf("[INIT RECV] %s 재고 갱신: %s%n", warehouseId, stockMap);
                                } else {
                                    System.err.printf("[INIT ERROR] 잘못된 데이터 수신: %s%n", text);
                                }
                            }
                        }

                        message.ackMessage(); // 수신 성공 시 ACK

                    } catch (Exception e) {
                        System.err.println("Failed to parse JSON: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onException(JCSMPException e) {
                System.err.println("Solace receive error: " + e.getMessage());
            }
        }, flowProps);

        flow.start();
        System.out.println("🔄 IMS FlowReceiver started for Q.JIHWAN.IMS");

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
