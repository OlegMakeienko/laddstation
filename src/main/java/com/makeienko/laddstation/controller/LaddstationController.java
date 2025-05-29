package com.makeienko.laddstation.controller;

import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.service.LaddstationApiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class LaddstationController {

    private final LaddstationApiClient apiClient;

    public LaddstationController(LaddstationApiClient apiClient) {
        this.apiClient = apiClient;
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
     * DTO för batteristatus
     */
    public static class BatteryStatusResponse {
        private double percentage;
        private double currentEnergyKwh;
        private double maxCapacityKwh;
        private boolean isCharging;

        public BatteryStatusResponse(double percentage, double currentEnergyKwh, double maxCapacityKwh, boolean isCharging) {
            this.percentage = percentage;
            this.currentEnergyKwh = currentEnergyKwh;
            this.maxCapacityKwh = maxCapacityKwh;
            this.isCharging = isCharging;
        }

        public double getPercentage() { return percentage; }
        public double getCurrentEnergyKwh() { return currentEnergyKwh; }
        public double getMaxCapacityKwh() { return maxCapacityKwh; }
        public boolean isCharging() { return isCharging; }
        
        public void setPercentage(double percentage) { this.percentage = percentage; }
        public void setCurrentEnergyKwh(double currentEnergyKwh) { this.currentEnergyKwh = currentEnergyKwh; }
        public void setMaxCapacityKwh(double maxCapacityKwh) { this.maxCapacityKwh = maxCapacityKwh; }
        public void setCharging(boolean charging) { isCharging = charging; }
    }

    /**
     * DTO för att returnera bara tid-information
     */
    public static class TimeResponse {
        private double hour;
        private double minute;

        public TimeResponse(double hour, double minute) {
            this.hour = hour;
            this.minute = minute;
        }

        public double getHour() { return hour; }
        public double getMinute() { return minute; }
        public void setHour(double hour) { this.hour = hour; }
        public void setMinute(double minute) { this.minute = minute; }
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
     * DTO för aktuellt timpris
     */
    public static class CurrentPriceResponse {
        private double currentPrice;
        private int currentHour;
        private double[] hourlyPrices;

        public CurrentPriceResponse(double currentPrice, int currentHour, double[] hourlyPrices) {
            this.currentPrice = currentPrice;
            this.currentHour = currentHour;
            this.hourlyPrices = hourlyPrices;
        }

        public double getCurrentPrice() { return currentPrice; }
        public int getCurrentHour() { return currentHour; }
        public double[] getHourlyPrices() { return hourlyPrices; }
        
        public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
        public void setCurrentHour(int currentHour) { this.currentHour = currentHour; }
        public void setHourlyPrices(double[] hourlyPrices) { this.hourlyPrices = hourlyPrices; }
    }
} 