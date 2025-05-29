package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.dto.SolarPanelStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Hanterar solpanelens status, produktion och optimering
 */
@Component
public class SolarPanelManager {

    private final LaddstationApiClient apiClient;
    
    // Produktionsgränser och konstanter
    private static final double LOW_PRODUCTION_PERCENT = 15.0;
    private static final double NORMAL_PRODUCTION_PERCENT = 50.0;
    private static final double HIGH_PRODUCTION_PERCENT = 80.0;
    private static final double DAILY_PRODUCTION_HOURS = 12.0; // Genomsnittliga productionsdagar per dag

    public SolarPanelManager(LaddstationApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Hämtar aktuell solpanelstatus från Python servern
     */
    public SolarPanelStatus getSolarPanelStatus() {
        InfoResponse info = apiClient.getInfo();
        if (info == null) {
            throw new RuntimeException("Failed to fetch solar panel info from server");
        }

        double productionPercent = (info.getSolarProductionKwh() / info.getSolarMaxCapacityKwh()) * 100;
        String productionStatus = determineProductionStatus(productionPercent);
        double dailyEstimate = calculateDailyProductionEstimate(info);
        double energySurplus = calculateEnergySurplus(info);
        String[] optimizationTips = generateOptimizationTips(info, productionPercent, energySurplus);

        return new SolarPanelStatus(
            info.getSolarProductionKwh(),
            info.getSolarMaxCapacityKwh(),
            Math.round(productionPercent * 10.0) / 10.0, // Avrunda till 1 decimal
            info.getNetHouseholdLoadKwh(),
            productionStatus,
            dailyEstimate,
            energySurplus,
            energySurplus > 0,
            optimizationTips
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
     * Beräkna uppskattad daglig produktion
     */
    private double calculateDailyProductionEstimate(InfoResponse info) {
        // Förenklad beräkning baserad på aktuell produktion och genomsnittliga produktionsdagar
        double currentHour = info.getSimTimeHour();
        
        // Under natten (22-06) eller tidigt/sent på dagen (06-08, 18-22)
        if (currentHour >= 22 || currentHour < 6 || (currentHour >= 18 && currentHour < 22) || (currentHour >= 6 && currentHour < 8)) {
            return info.getSolarMaxCapacityKwh() * DAILY_PRODUCTION_HOURS * 0.4; // 40% av teoretisk max
        }
        
        // Under dagen - extrapolera från aktuell produktion
        double currentProductionRate = info.getSolarProductionKwh();
        return currentProductionRate * DAILY_PRODUCTION_HOURS * 0.7; // 70% av teoretisk baserat på nuvarande
    }

    /**
     * Beräkna energiöverskott tillgängligt för laddning
     */
    private double calculateEnergySurplus(InfoResponse info) {
        // Överskott = solproduktion - hushållsförbrukning
        double surplus = info.getSolarProductionKwh() - info.getHouseholdLoadKwh();
        return Math.max(0, surplus); // Returnera bara positiva värden
    }

    /**
     * Generera optimeringstips baserat på solproduktion
     */
    public String[] generateOptimizationTips(InfoResponse info, double productionPercent, double energySurplus) {
        List<String> tips = new ArrayList<>();
        
        if (productionPercent > HIGH_PRODUCTION_PERCENT && energySurplus > 0) {
            tips.add("☀️ Hög solproduktion - perfekt tid för EV-laddning!");
        }
        
        if (energySurplus > 5.0) {
            tips.add("⚡ Stort energiöverskott - använd för att ladda husbatteriet");
        }
        
        if (info.getNetHouseholdLoadKwh() > 0 && productionPercent < LOW_PRODUCTION_PERCENT) {
            tips.add("🌙 Låg solproduktion - överväg att använda husbatteriet");
        }
        
        if (productionPercent == 0 && info.getSimTimeHour() >= 6 && info.getSimTimeHour() <= 18) {
            tips.add("☁️ Inga soltimmar - kontrollera solpanelernas status");
        }
        
        return tips.toArray(new String[0]);
    }

    /**
     * Kontrollera om det är optimal tid för soldriven laddning
     */
    public boolean isOptimalSolarChargingTime(InfoResponse info) {
        SolarPanelStatus status = getSolarPanelStatus();
        return status.isSurplusAvailable() && 
               status.getEnergySurplus() >= 3.0 && // Minst 3 kW överskott
               status.getProductionPercent() > NORMAL_PRODUCTION_PERCENT;
    }

    /**
     * Beräkna hur mycket EV-laddning som kan göras med endast solenergi
     */
    public double calculateSolarOnlyChargingCapacity(InfoResponse info) {
        SolarPanelStatus status = getSolarPanelStatus();
        if (!status.isSurplusAvailable()) {
            return 0;
        }
        
        // Antag att vi kan använda 80% av överskottet för EV-laddning (20% som säkerhetsmarginal)
        return status.getEnergySurplus() * 0.8;
    }
} 