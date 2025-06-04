package com.makeienko.laddstation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response för husbatteristatus
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeBatteryResponse {
    private double batteryLevel;
    private double capacityKwh;
    private double maxCapacityKwh;
    private double minCapacityKwh;
    private String batteryMode;
} 