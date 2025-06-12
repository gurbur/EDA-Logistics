package com.jihwan.logistics.lms;

import com.jihwan.logistics.lms.publisher.LmsEventPublisher;
import com.jihwan.logistics.lms.service.WorkerAssignmentManager;
import com.jihwan.logistics.lms.subscriber.LmsEventSubscriber;

public class LmsApplication {
    public static void main(String[] args) {
        try {
            WorkerAssignmentManager service = new WorkerAssignmentManager();
            LmsEventPublisher publisher = new LmsEventPublisher();
            LmsEventSubscriber subscriber = new LmsEventSubscriber(service, publisher);
            subscriber.start();  // 구독 및 메시지 루프 시작

        } catch (Exception e) {
            System.err.println("LMS Application failed to start: " + e.getMessage());
        }
    }
}
