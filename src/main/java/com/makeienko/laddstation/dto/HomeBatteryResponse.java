package com.makeienko.laddstation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeBatteryResponse {
    private double capacityPercent;
    private double currentCapacityKwh;
    private double maxCapacityKwh;
    private double minCapacityKwh;
    private String mode;
    private String healthStatus;
    private double reserveHours;
    private double totalAvailableEnergy;
    private String[] warnings;
    private boolean lowBatteryWarning;
    private boolean criticalBattery;
} 