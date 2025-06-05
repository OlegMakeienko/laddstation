package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.dto.SolarPanelStatus;
import org.springframework.stereotype.Component;

/**
 * Hanterar solpanelens status och produktion
 */
@Component
public class SolarPanelManager {

    private final LaddstationApiClient apiClient;
    
    // Produktionsgränser
    private static final double LOW_PRODUCTION_PERCENT = 15.0;
    private static final double NORMAL_PRODUCTION_PERCENT = 50.0;
    private static final double HIGH_PRODUCTION_PERCENT = 80.0;

    public SolarPanelManager(LaddstationApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Hämtar aktuell solpanelstatus
     */
    public SolarPanelStatus getSolarPanelStatus() {
        InfoResponse info = apiClient.getInfo();
        if (info == null) {
            throw new RuntimeException("Failed to fetch solar panel info from server");
        }

        double productionPercent = (info.getSolarProductionKwh() / info.getSolarMaxCapacityKwh()) * 100;
        String productionStatus = determineProductionStatus(productionPercent);
        double energySurplus = calculateEnergySurplus(info);

        return new SolarPanelStatus(
            info.getSolarProductionKwh(),
            info.getSolarMaxCapacityKwh(),
            Math.round(productionPercent * 10.0) / 10.0, // Avrunda till 1 decimal
            info.getNetHouseholdLoadKwh(),
            productionStatus,
            energySurplus,
            energySurplus > 0
        );
    }

    /**
     * Bestäm produktionsstatus baserat på procent av max kapacitet
     */
    private String determineProductionStatus(double productionPercent) {
        if (productionPercent == 0) {
            return "Ingen produktion";
        } else if (productionPercent < LOW_PRODUCTION_PERCENT) {
            return "Låg produktion";
        } else if (productionPercent < NORMAL_PRODUCTION_PERCENT) {
            return "Normal produktion";
        } else if (productionPercent < HIGH_PRODUCTION_PERCENT) {
            return "Hög produktion";
        } else {
            return "Max produktion";
        }
    }

    /**
     * Beräkna energiöverskott tillgängligt för laddning
     */
    private double calculateEnergySurplus(InfoResponse info) {
        // Överskott = solproduktion - hushållsförbrukning
        double surplus = info.getSolarProductionKwh() - info.getHouseholdLoadKwh();
        double result = Math.max(0, surplus); // Returnera bara positiva värden
        // Avrunda till 2 decimaler
        return Math.round(result * 100.0) / 100.0;
    }

    /**
     * Kontrollera om det är bra tid för soldriven laddning
     */
    public boolean isGoodForSolarCharging(InfoResponse info) {
        SolarPanelStatus status = getSolarPanelStatus();
        return status.isSurplusAvailable() && 
               status.getEnergySurplus() >= 3.0 && // Minst 3 kW överskott
               status.getProductionPercent() > NORMAL_PRODUCTION_PERCENT;
    }
} 