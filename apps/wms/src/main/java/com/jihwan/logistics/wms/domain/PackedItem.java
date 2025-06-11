package com.jihwan.logistics.wms.domain;

public class PackedItem {
    private final String itemId;
    private ItemStatus status;

    public PackedItem(String itemId) {
        this.itemId = itemId;
        this.status = ItemStatus.PICKED;
    }

    public void pack() {
        if (status == ItemStatus.PICKED) {
            status = ItemStatus.PACKED;
        }
    }

    public ItemStatus getStatus() {
        return status;
    }

    public String getItemId() {
        return itemId;
    }
}
