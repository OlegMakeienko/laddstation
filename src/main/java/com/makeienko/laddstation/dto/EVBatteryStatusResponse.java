package com.makeienko.laddstation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EVBatteryStatusResponse {
    private double percentage;
    private double currentEnergyKwh;
    private double maxCapacityKwh;
    private boolean isCharging;
} 