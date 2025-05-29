package com.makeienko.laddstation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeBatteryStatus {
    private double currentCapacityKwh;
    private double maxCapacityKwh;
    private double minCapacityKwh;
    private double capacityPercent;
    private String mode;
    private String healthStatus;
    private double reserveHours;
    private boolean lowBatteryWarning;
    private boolean criticalBattery;
} 