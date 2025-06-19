package com.jihwan.logistics.tms;

import com.jihwan.logistics.tms.publisher.TmsEventPublisher;
import com.jihwan.logistics.tms.subscriber.TmsDmqProcessor;
import com.jihwan.logistics.tms.subscriber.TmsEventSubscriber;
import com.jihwan.logistics.tms.service.TruckAssignmentManager;

public class TmsApplication {
    public static void main(String[] args) {
        try {
            TruckAssignmentManager service = new TruckAssignmentManager();
            TmsEventPublisher publisher = new TmsEventPublisher();
            TmsEventSubscriber subscriber = new TmsEventSubscriber(service, publisher);
            TmsDmqProcessor dmqProcessor = new TmsDmqProcessor(service, publisher);

            subscriber.start();  // Solace 메시지 수신 시작
            dmqProcessor.start();
        } catch (Exception e) {
            System.err.println("TMS Application failed to start: " + e.getMessage());
        }
    }
}
