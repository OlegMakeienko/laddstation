package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.InfoResponse;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Klass för att beräkna optimala timmar för batteriladdning
 * baserat på priser och säkerhetsgränser
 */
@Component
public class ChargingHourOptimizer {
    
    private static final double MAX_TOTAL_LOAD = 11.0; // kW
    private static final double CHARGING_POWER = 7.4;  // kW
    
    private final LaddstationApiClient apiClient;

    public ChargingHourOptimizer(LaddstationApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    /**
     * Inre klass för att representera en timme med pris
     */
    public static class HourPrice {
        private final int hour;
        private final double price;
        private final double totalLoad;
        
        public HourPrice(int hour, double price, double totalLoad) {
            this.hour = hour;
            this.price = price;
            this.totalLoad = totalLoad;
        }
        
        public int getHour() { return hour; }
        public double getPrice() { return price; }
        public double getTotalLoad() { return totalLoad; }
        public double getCost() { return price * totalLoad; }
    }
    
    /**
     * Hitta optimala timmar baserat på förbrukning (lägsta förbrukning först)
     */
    public List<Double> findOptimalHoursByConsumption() {
        double[] hourlyBaseload = apiClient.getBaseload();
        int hoursNeeded = calculateHoursNeededToChargeFrom20To80();
        List<HourPrice> safeHours = getSafeHours(hourlyBaseload, null);
        
        // Sortera efter total förbrukning (lägst först)
        safeHours.sort(Comparator.comparing(HourPrice::getTotalLoad));
        
        return selectBestHours(safeHours, hoursNeeded);
    }
    
    /**
     * Hitta optimala timmar baserat på pris (lägsta kostnad först)
     */
    public List<Double> findOptimalHoursByPrice() {
        double[] hourlyBaseload = apiClient.getBaseload();
        double[] hourlyPrices = apiClient.getHourlyPrices();
        int hoursNeeded = calculateHoursNeededToChargeFrom20To80();
        List<HourPrice> safeHours = getSafeHours(hourlyBaseload, hourlyPrices);
        
        // Sortera efter total kostnad (lägst först)
        safeHours.sort(Comparator.comparing(HourPrice::getCost));
        
        return selectBestHours(safeHours, hoursNeeded);
    }
    
    /**
     * Beräkna hur många timmar som behövs för laddning från 20% till 80%
     */
    private int calculateHoursNeededToChargeFrom20To80() {
        InfoResponse info = apiClient.getInfo(); // Hämta aktuell batteriinfo
        // Förutsätter att vi alltid vill ladda till 80%
        double targetChargeKWh = info.getBatteryCapacityKWh() * 0.8;
        double energyNeeded = targetChargeKWh - info.getBaseCurrentLoad();
        return Math.max(1, (int) Math.ceil(energyNeeded / CHARGING_POWER));
    }
    
    /**
     * Hitta alla säkra timmar (under 11 kW total förbrukning)
     */
    private List<HourPrice> getSafeHours(double[] hourlyBaseload, double[] hourlyPrices) {
        List<HourPrice> safeHours = new ArrayList<>();
        
        for (int hour = 0; hour < hourlyBaseload.length; hour++) {
            double totalLoad = hourlyBaseload[hour] + CHARGING_POWER;
            
            if (totalLoad <= MAX_TOTAL_LOAD) {
                double price = (hourlyPrices != null) ? hourlyPrices[hour] : 0.0;
                safeHours.add(new HourPrice(hour, price, totalLoad));
                
                System.out.println("Hour " + hour + " is safe for charging with total load: " 
                    + String.format("%.2f", totalLoad) + " kW"
                    + (hourlyPrices != null ? ", price: " + String.format("%.2f", price) + " kr/kWh" : ""));
            }
        }
        
        return safeHours;
    }
    
    /**
     * Välj de bästa timmarna från en sorterad lista
     */
    private List<Double> selectBestHours(List<HourPrice> sortedHours, int hoursNeeded) {
        List<Double> optimalHours = new ArrayList<>();
        
        int hoursToSelect = Math.min(hoursNeeded, sortedHours.size());
        
        System.out.println("Selecting " + hoursToSelect + " optimal hours out of " 
            + sortedHours.size() + " safe hours:");
        
        for (int i = 0; i < hoursToSelect; i++) {
            HourPrice hourPrice = sortedHours.get(i);
            optimalHours.add((double) hourPrice.getHour());
            
            System.out.println("Selected hour " + hourPrice.getHour() 
                + " - Total load: " + String.format("%.2f", hourPrice.getTotalLoad()) + " kW"
                + ", Cost: " + String.format("%.2f", hourPrice.getCost()) + " kr");
        }
        
        return optimalHours;
    }
}
