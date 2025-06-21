package com.jihwan.logistics.simulator.simulator;

import com.jihwan.logistics.simulator.domain.*;

import java.util.*;

public class SimulatorState {

    private final Map<String, Truck> trucks = new HashMap<>();
    private final Map<String, Worker> workers = new HashMap<>();
    private final Map<String, List<String>> trucksByLocation = new HashMap<>();
    private final Map<String, List<String>> workersByLocation = new HashMap<>();

    // 자원 등록
    public void registerTruck(Truck truck) {
        trucks.put(truck.getId(), truck);
        trucksByLocation.computeIfAbsent(truck.getLocation(), k -> new ArrayList<>()).add(truck.getId());
    }

    public void registerWorker(Worker worker) {
        workers.put(worker.getId(), worker);
        workersByLocation.computeIfAbsent(worker.getLocation(), k -> new ArrayList<>()).add(worker.getId());
    }

    // 자원 조회
    private Truck findAvailableTruck(String location) {
        List<String> truckList = trucksByLocation.getOrDefault(location, Collections.emptyList());
        for (String id : truckList) {
            Truck t = trucks.get(id);
            if (t.getStatus() == Truck.Status.IDLE) return t;
        }
        return null;
    }

    private Worker findAvailableWorker(String location) {
        List<String> workerList = workersByLocation.getOrDefault(location, Collections.emptyList());
        for (String id : workerList) {
            Worker w = workers.get(id);
            if (w.getStatus() == Worker.Status.IDLE) return w;
        }
        return null;
    }

    // 주문 처리
    public boolean processOrder(Order order) {
        String from = order.getSource();
        String to = order.getDestination();
        String orderId = order.getOrderId();

        Truck truck = findAvailableTruck(from);
        Worker worker = findAvailableWorker(from);

        if (truck == null || worker == null) {
            Logger.logFailure(orderId, "출발지 " + from + "에 사용 가능한 자원이 없습니다.");
            return false;
        }

        // 할당 처리
        truck.assignTo(to);
        worker.assignTo(to);
        removeFromLocation(trucksByLocation, from, truck.getId());
        removeFromLocation(workersByLocation, from, worker.getId());
        Logger.logAssignment(orderId, truck, worker, to);

        // 배송 완료 처리
        truck.completeDelivery();
        worker.completeDelivery();
        truck.returnTo(from);
        worker.returnTo(from);

        addToLocation(trucksByLocation, from, truck.getId());
        addToLocation(workersByLocation, from, worker.getId());

        Logger.logCompletion(orderId, truck, worker);
        Logger.logReturn(truck.getId(), worker.getId(), from);
        return true;
    }

    private void removeFromLocation(Map<String, List<String>> map, String location, String id) {
        List<String> list = map.get(location);
        if (list != null) list.remove(id);
    }

    private void addToLocation(Map<String, List<String>> map, String location, String id) {
        map.computeIfAbsent(location, k -> new ArrayList<>());
        if (!map.get(location).contains(id)) {
            map.get(location).add(id);
        }
    }

    public void updateTruckStatus(String truckId, Truck.Status status, String location) {
        Truck truck = trucks.get(truckId);
        if (truck == null) return;

        String oldLocation = truck.getLocation();

        // 상태 및 위치 갱신
        truck.setStatus(status);
        truck.setLocation(location);

        // 위치가 변경되었으면 location map 갱신
        if (!Objects.equals(oldLocation, location)) {
            removeFromLocation(trucksByLocation, oldLocation, truckId);
            addToLocation(trucksByLocation, location, truckId);
        }
    }

    public void updateWorkerStatus(String workerId, Worker.Status status, String location) {
        Worker worker = workers.get(workerId);
        if (worker == null) return;

        String oldLocation = worker.getLocation();

        // 상태 및 위치 갱신
        worker.setStatus(status);
        worker.setLocation(location);

        // 위치가 변경되었으면 location map 갱신
        if (!Objects.equals(oldLocation, location)) {
            removeFromLocation(workersByLocation, oldLocation, workerId);
            addToLocation(workersByLocation, location, workerId);
        }
    }


    public void printStatus() {
        System.out.println("====== WORLD STATUS ======");
        Set<String> allLocations = new TreeSet<>();
        allLocations.addAll(trucksByLocation.keySet());
        allLocations.addAll(workersByLocation.keySet());

        for (String location : allLocations) {
            System.out.println("📍 " + location);
            List<String> tList = trucksByLocation.getOrDefault(location, Collections.emptyList());
            System.out.print("  🚚 Trucks: ");
            if (tList.isEmpty()) System.out.println("[]");
            else {
                List<String> truckStates = new ArrayList<>();
                for (String id : tList) truckStates.add(trucks.get(id).toString());
                System.out.println(truckStates);
            }

            List<String> wList = workersByLocation.getOrDefault(location, Collections.emptyList());
            System.out.print("  👷 Workers: ");
            if (wList.isEmpty()) System.out.println("[]");
            else {
                List<String> workerStates = new ArrayList<>();
                for (String id : wList) workerStates.add(workers.get(id).toString());
                System.out.println(workerStates);
            }
        }
    }
}
