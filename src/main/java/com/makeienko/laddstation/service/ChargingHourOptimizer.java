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
     * Hitta optimala timmar baserat på förbrukning.
     * ÄNDRAD: Returnerar nu en lista med ALLA timmar under dygnet där
     * den totala lasten (hushåll + billaddning) är <= MAX_TOTAL_LOAD.
     * Priset ignoreras helt.
     */
    public List<Double> findOptimalHoursByConsumption() {
        double[] hourlyBaseload = apiClient.getBaseload();
        List<Double> safeHours = new ArrayList<>();

        for (int hour = 0; hour < hourlyBaseload.length; hour++) {
            double totalLoad = hourlyBaseload[hour] + CHARGING_POWER;
            if (totalLoad <= MAX_TOTAL_LOAD) {
                safeHours.add((double) hour);
            }
        }

        if (safeHours.isEmpty()) {
            System.out.println("No safe hours found for consumption-based strategy.");
        } else {
            // Sortera listan för konsekvent loggning och om ChargingServiceImpl skulle förvänta sig det någon gång.
            // För `contains`-anropet i ChargingServiceImpl spelar ordningen ingen roll.
            safeHours.sort(Comparator.naturalOrder());
        }
        return safeHours;
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
        // Use ev_batt_max_capacity_kwh from InfoResponse
        double maxCapacityKwh = info.getEvBattMaxCapacityKwh();
        if (maxCapacityKwh <= 0) { // Basic sanity check
            System.err.println("ChargingHourOptimizer: Invalid maxCapacityKwh from server: " + maxCapacityKwh + ". Using default 46.3");
            maxCapacityKwh = 46.3; // Fallback
        }
        double targetChargeKWh = maxCapacityKwh * 0.8;
        double energyNeeded = targetChargeKWh - info.getEvBatteryEnergyKwh();
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
            }
        }
        
        return safeHours;
    }
    
    /**
     * Väljer de X timmar som behövs för laddning från en lista av HourPrice-objekt.
     * Listan förväntas vara sorterad enligt en specifik strategi (t.ex. lägst total förbrukning eller lägst kostnad).
     */
    private List<Double> selectBestHours(List<HourPrice> sortedHours, int hoursNeeded) {
        List<Double> optimalHours = new ArrayList<>();
        
        int hoursToSelect = Math.min(hoursNeeded, sortedHours.size());
        
        for (int i = 0; i < hoursToSelect; i++) {
            HourPrice hourPrice = sortedHours.get(i);
            optimalHours.add((double) hourPrice.getHour());
        }
        
        return optimalHours;
    }
}
