package com.jihwan.logistics.wms.domain;

public enum ItemStatus {
    AVAILABLE,
    PICKED,
    PACKED;

    public boolean canTransitionTo(ItemStatus next) {
        return switch (this) {
            case AVAILABLE -> next == PICKED;
            case PICKED -> next == PACKED;
            case PACKED -> false;
        };
    }
}
