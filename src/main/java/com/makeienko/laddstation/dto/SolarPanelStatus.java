package com.makeienko.laddstation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolarPanelStatus {
    private double currentProductionKwh;
    private double maxCapacityKwh;
    private double productionPercent;
    private double netHouseholdLoadKwh;
    private String productionStatus; // "Ingen produktion", "Låg produktion", "Normal produktion", "Hög produktion", "Max produktion"
    private double dailyProductionEstimate;
    private double energySurplus; // Överskott som kan användas för laddning
    private boolean isSurplusAvailable;
    private String[] optimizationTips; // Lägg till optimeringstips direkt i denna klass
} 