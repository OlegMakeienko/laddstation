package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.dto.HomeBatteryResponse;
import org.springframework.stereotype.Component;

/**
 * Hanterar husbatteriets status och laddning
 */
@Component
public class HomeBatteryManager {

    private final LaddstationApiClient apiClient;

    public HomeBatteryManager(LaddstationApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Hämtar aktuell husbatteristatus
     */
    public HomeBatteryResponse getHomeBatteryStatus() {
        InfoResponse info = apiClient.getInfo();
        if (info == null) {
            throw new RuntimeException("Failed to fetch battery info from server");
        }

        // Försök ladda batteriet om det finns solproduktion
        double currentCapacity = info.getHomeBattCapacityKwh();
        if (info.getSolarProductionKwh() > 0 && info.getHomeBattCapacityPercent() < 95.0) {
            currentCapacity = chargeHomeBattery();
        }

        return new HomeBatteryResponse(
            info.getHomeBattCapacityPercent(),
            currentCapacity,
            info.getHomeBattMaxCapacityKwh(),
            info.getHomeBattMinCapacityKwh(),
            info.getHomeBatteryMode()
        );
    }

    /**
     * Ladda husbatteriet från solpaneler
     */
    public double chargeHomeBattery() {
        try {
            InfoResponse info = apiClient.getInfo();
            if (info == null) {
                System.err.println("HomeBatteryManager: Failed to fetch info for home battery charging");
                return 0.0;
            }
            
            double homeBatteryPercent = info.getHomeBattCapacityPercent();
            double solarProduction = info.getSolarProductionKwh();
            double homeBatteryCapacity = info.getHomeBattCapacityKwh();
            double homeBattMaxCapacityKwh = info.getHomeBattMaxCapacityKwh();
            double currentCapacity = homeBatteryCapacity;
            
            // Kontrollera om vi har solproduktion och batteriet inte är fullt
            if (solarProduction > 0 && homeBatteryPercent < 95.0) {
                System.out.println("HomeBatteryManager: Starting solar charging of home battery");
                
                // Beräkna hur mycket vi kan ladda
                double availableSpace = homeBattMaxCapacityKwh - homeBatteryCapacity;
                double chargeAmount = Math.min(solarProduction, availableSpace);
                
                // Uppdatera kapaciteten
                currentCapacity = homeBatteryCapacity + chargeAmount;
                
                // Uppdatera procenten
                homeBatteryPercent = (currentCapacity / homeBattMaxCapacityKwh) * 100;
                
                System.out.printf("HomeBatteryManager: Charged %.2f kWh, new capacity: %.2f kWh (%.1f%%)\n", 
                    chargeAmount, currentCapacity, homeBatteryPercent);
            }

            return currentCapacity;
            
        } catch (Exception e) {
            System.err.println("HomeBatteryManager: Failed to charge home battery: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Ladda ur husbatteriet till 10%
     */
    public void dischargeBattery() {
        try {
            apiClient.dischargeHomeBatteryTo10();
        } catch (Exception e) {
            System.err.println("HomeBatteryManager: Failed to discharge battery: " + e.getMessage());
        }
    }
} 