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
    private double batteryPercentage; // Nuvarande batterinivå
    private double chargingPower; // Laddstationens effekt i kW (vi har 7.4 kW)
    private InfoResponse infoResponse; // Information om simulerad tid och batteristatistik
}
