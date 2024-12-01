package com.makeienko.laddstation.dto;

import lombok.Data;

@Data
public class ChargingSession {
    private String stationId;
    private String sessionId;
    private String userId;
    private double currentPower;
    private double totalEnergy;
    private String status;
}
