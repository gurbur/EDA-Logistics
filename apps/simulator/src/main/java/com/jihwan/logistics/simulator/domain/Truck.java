package com.jihwan.logistics.simulator.domain;

public class Truck {

    public enum Status {
        IDLE, ASSIGNED, RETURNING
    }

    private final String id;
    private Status status;
    private String location; // 현재 위치 = 항상 출발지 기준

    public Truck(String id, String location) {
        this.id = id;
        this.location = location;
        this.status = Status.IDLE;
    }

    public String getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void assignTo(String destination) {
        this.status = Status.ASSIGNED;
    }

    public void completeDelivery() {
        this.status = Status.RETURNING;
    }

    public void returnTo(String homeLocation) {
        this.status = Status.IDLE;
        this.location = homeLocation;
    }

    @Override
    public String toString() {
        return id + " (" + status + ")";
    }
}
