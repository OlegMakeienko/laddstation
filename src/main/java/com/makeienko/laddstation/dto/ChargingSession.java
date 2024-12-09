package com.makeienko.laddstation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChargingSession {
    private List<Double> hourlyPrices; // Elpriser per timme
    private List<Double> hourlyBaseload; // Hushållsförbrukning per timme
    private double batteryPercentage; // Nuvarande batterinivå i %
    private double chargingPower; // Laddstationens effekt i kW (vi har 7.4 kW)
    private double batteryCapacity; // Batteriets totala kapacitet i kWh
    private double currentLoad; // Nuvarande laddningsnivå i kWh
    private InfoResponse infoResponse; // Information om simulerad tid och batteristatistik

    public void updateBatteryLoad(double energyAdded) {
        this.currentLoad += energyAdded;
        this.currentLoad = Math.min(this.currentLoad, this.batteryCapacity); // Begränsa till maxkapacitet
        this.batteryPercentage = (this.currentLoad / this.batteryCapacity) * 100;
    }
}
