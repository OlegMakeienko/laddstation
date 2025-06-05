package com.makeienko.laddstation.controller;

import com.makeienko.laddstation.dto.*;
import com.makeienko.laddstation.service.LaddstationApiClient;
import com.makeienko.laddstation.service.ChargingHourOptimizer;
import com.makeienko.laddstation.service.HomeBatteryManager;
import com.makeienko.laddstation.service.SolarPanelManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class LaddstationController {

    private final LaddstationApiClient apiClient;
    private final ChargingHourOptimizer chargingHourOptimizer;
    private final HomeBatteryManager homeBatteryManager;
    private final SolarPanelManager solarPanelManager;

    public LaddstationController(LaddstationApiClient apiClient, ChargingHourOptimizer chargingHourOptimizer, HomeBatteryManager homeBatteryManager, SolarPanelManager solarPanelManager) {
        this.apiClient = apiClient;
        this.chargingHourOptimizer = chargingHourOptimizer;
        this.homeBatteryManager = homeBatteryManager;
        this.solarPanelManager = solarPanelManager;
    }

    /**
     * Hämtar simulerad tid och batteristatus från Python servern
     */
    @GetMapping("/info")
    public ResponseEntity<InfoResponse> getInfo() {
        try {
            InfoResponse info = apiClient.getInfo();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint specifikt för att hämta bara tiden
     */
    @GetMapping("/time")
    public ResponseEntity<TimeResponse> getCurrentTime() {
        try {
            InfoResponse info = apiClient.getInfo();
            TimeResponse timeResponse = new TimeResponse(
                info.getSimTimeHour(), 
                info.getSimTimeMin()
            );
            return ResponseEntity.ok(timeResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint för batteristatus med beräknad procent
     */
    @GetMapping("/battery")
    public ResponseEntity<BatteryStatusResponse> getBatteryStatus() {
        try {
            InfoResponse info = apiClient.getInfo();
            
            double maxCapacityKwh = info.getEvBattMaxCapacityKwh();
            double currentPercentage = (info.getBatteryEnergyKwh() / maxCapacityKwh) * 100;
            currentPercentage = Math.round(currentPercentage * 10.0) / 10.0; // Avrunda till 1 decimal
            
            BatteryStatusResponse batteryStatus = new BatteryStatusResponse(
                currentPercentage,
                info.getBatteryEnergyKwh(),
                maxCapacityKwh,
                info.isEvBatteryChargeStartStopp()
            );
            
            return ResponseEntity.ok(batteryStatus);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Hämtar hushållets basförbrukning per timme
     */
    @GetMapping("/baseload")
    public ResponseEntity<double[]> getBaseload() {
        try {
            double[] baseload = apiClient.getBaseload();
            return ResponseEntity.ok(baseload);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Hämtar aktuellt timpris baserat på simulerad tid
     */
    @GetMapping("/current-price")
    public ResponseEntity<CurrentPriceResponse> getCurrentPrice() {
        try {
            InfoResponse info = apiClient.getInfo();
            double[] hourlyPrices = apiClient.getHourlyPrices();
            
            int currentHour = (int) Math.floor(info.getSimTimeHour()) % 24;
            double currentPrice = hourlyPrices[currentHour];
            
            CurrentPriceResponse priceResponse = new CurrentPriceResponse(
                currentPrice,
                currentHour,
                hourlyPrices
            );
            
            return ResponseEntity.ok(priceResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Hämtar optimala laddningstider baserat på förbrukning
     */
    @GetMapping("/optimal-charging-hours")
    public ResponseEntity<OptimalChargingResponse> getOptimalChargingHours() {
        try {
            List<Double> optimalHours = chargingHourOptimizer.findOptimalHoursByConsumption();
            
            OptimalChargingResponse response = new OptimalChargingResponse(
                optimalHours,
                "Låg förbrukning",
                formatOptimalHoursRange(optimalHours)
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Hämtar husbatteriets status och säkerhetsinformation
     */
    @GetMapping("/home-battery")
    public ResponseEntity<HomeBatteryResponse> getHomeBatteryStatus() {
        try {
            HomeBatteryResponse status = homeBatteryManager.getHomeBatteryStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/solar-panel")
    public ResponseEntity<SolarPanelStatus> getSolarPanelStatus() {
        try {
            SolarPanelStatus status = solarPanelManager.getSolarPanelStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Formatera optimala timmar till ett läsbart tidsintervall
     */
    private String formatOptimalHoursRange(List<Double> optimalHours) {
        if (optimalHours.isEmpty()) {
            return "Ingen optimal tid";
        }
        
        // Konvertera till integers och sortera
        List<Integer> hours = optimalHours.stream()
            .mapToInt(Double::intValue)
            .sorted()
            .boxed()
            .toList();
        
        if (hours.size() == 1) {
            return String.format("%02d:00", hours.get(0));
        }
        
        // Hitta det längsta sammanhängande intervallet som inkluderar nattetid
        List<Integer> nightHours = new ArrayList<>();
        List<Integer> dayHours = new ArrayList<>();
        
        for (int hour : hours) {
            if (hour >= 22 || hour <= 6) {
                nightHours.add(hour);
            } else {
                dayHours.add(hour);
            }
        }
        
        // Prioritera nattetid om det finns fler än 3 nattetimmar
        if (nightHours.size() >= 3) {
            int startHour = nightHours.stream().filter(h -> h >= 22).min(Integer::compareTo).orElse(22);
            int endHour = nightHours.stream().filter(h -> h <= 6).max(Integer::compareTo).orElse(6);
            
            if (startHour >= 22 && endHour <= 6) {
                return String.format("%02d:00 - %02d:00", startHour, endHour + 1);
            }
        }
        
        // Annars visa bästa sammanhängande intervall
        List<List<Integer>> intervals = findContiguousIntervals(hours);
        List<Integer> longestInterval = intervals.stream()
            .max((a, b) -> Integer.compare(a.size(), b.size()))
            .orElse(hours);
        
        if (longestInterval.size() >= 2) {
            int start = longestInterval.get(0);
            int end = longestInterval.get(longestInterval.size() - 1);
            return String.format("%02d:00 - %02d:00", start, end + 1);
        }
        
        // Fallback: visa antal timmar
        return hours.size() + " optimala timmar";
    }
    
    /**
     * Hitta sammanhängande tidsintervall
     */
    private List<List<Integer>> findContiguousIntervals(List<Integer> hours) {
        List<List<Integer>> intervals = new ArrayList<>();
        List<Integer> currentInterval = new ArrayList<>();
        
        for (int i = 0; i < hours.size(); i++) {
            if (currentInterval.isEmpty()) {
                currentInterval.add(hours.get(i));
            } else if (hours.get(i) == currentInterval.get(currentInterval.size() - 1) + 1) {
                currentInterval.add(hours.get(i));
            } else {
                intervals.add(new ArrayList<>(currentInterval));
                currentInterval.clear();
                currentInterval.add(hours.get(i));
            }
        }
        
        if (!currentInterval.isEmpty()) {
            intervals.add(currentInterval);
        }
        
        return intervals;
    }
} 