package com.jihwan.logistics.oms.dto;

import lombok.Data;

@Data
public class OrderRequestDto {
    private String userId;
    private String itemId;
    private String destination;
}
