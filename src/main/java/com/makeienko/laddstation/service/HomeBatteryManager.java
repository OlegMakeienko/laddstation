package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.dto.HomeBatteryResponse;
import org.springframework.stereotype.Component;

/**
 * Hanterar husbatteriets status, säkerhet och funktioner
 */
@Component
public class HomeBatteryManager {

    private final LaddstationApiClient apiClient;
    
    // Säkerhetsgränser och konstanter
    private static final double LOW_BATTERY_WARNING_PERCENT = 20.0;
    private static final double CRITICAL_BATTERY_PERCENT = 15.0;
    private static final double OPTIMAL_CHARGE_PERCENT = 80.0;

    public HomeBatteryManager(LaddstationApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Hämtar aktuell husbatteristatus från Python servern
     */
    public HomeBatteryResponse getHomeBatteryStatus() {
        InfoResponse info = apiClient.getInfo();
        if (info == null) {
            throw new RuntimeException("Failed to fetch battery info from server");
        }

        return new HomeBatteryResponse(
            info.getHomeBattCapacityPercent(),
            info.getHomeBattCapacityKwh(),
            info.getHomeBattMaxCapacityKwh(),
            info.getHomeBattMinCapacityKwh(),
            info.getHomeBatteryMode(),
            determineHealthStatus(info),
            calculateReserveHours(info),
            0.0, // totalAvailableEnergy - set to 0 for now
            new String[0], // warnings - empty array for now
    isLowBatteryWarning(info.getHomeBattCapacityPercent()),
            isCriticalBattery(info.getHomeBattCapacityPercent())
        );
    }

    /**
     * Beräkna hur många timmar husbatteriet kan förse huset med ström
     */
    private double calculateReserveHours(InfoResponse info) {
        double availableEnergy = info.getHomeBattCapacityKwh() - info.getHomeBattMinCapacityKwh();
        double currentLoad = info.getHouseholdLoadKwh();
        
        if (currentLoad <= 0) {
            return Double.MAX_VALUE; // Ingen förbrukning = oändlig tid
        }
        
        return Math.max(0, availableEnergy / currentLoad);
    }

    /**
     * Bestäm batteriets hälsostatus
     */
    private String determineHealthStatus(InfoResponse info) {
        double percent = info.getHomeBattCapacityPercent();
        
        if (percent >= OPTIMAL_CHARGE_PERCENT) {
            return "Optimal";
        } else if (percent >= LOW_BATTERY_WARNING_PERCENT) {
            return "Bra";
        } else if (percent >= CRITICAL_BATTERY_PERCENT) {
            return "Låg - Varning";
        } else {
            return "Kritisk";
        }
    }

    /**
     * Kontrollera om låg batteri varning ska visas
     */
    private boolean isLowBatteryWarning(double percent) {
        return percent <= LOW_BATTERY_WARNING_PERCENT;
    }

    /**
     * Kontrollera om kritisk batterinivå
     */
    private boolean isCriticalBattery(double percent) {
        return percent <= CRITICAL_BATTERY_PERCENT;
    }

    /**
     * Beräkna om batteriet är säkert för V2H (Vehicle-to-Home) användning
     */
    public boolean isSafeForV2H(double evBatteryPercent, double homeBatteryPercent) {
        return evBatteryPercent >= 30.0 && homeBatteryPercent >= 15.0;
    }

    /**
     * Beräkna total tillgänglig energi för hemmet (EV + husbatteri)
     */
    public double calculateTotalAvailableEnergy(InfoResponse info) {
        // Tillgänglig energi från husbatteriet
        double homeAvailable = Math.max(0, info.getHomeBattCapacityKwh() - info.getHomeBattMinCapacityKwh());
        
        // Tillgänglig energi från elbilen (behåll 30% för transport)
        double evUsablePercent = Math.max(0, (info.getBatteryEnergyKwh() / info.getEvBattMaxCapacityKwh() * 100) - 30);
        double evAvailable = (evUsablePercent / 100) * info.getEvBattMaxCapacityKwh();
        
        return homeAvailable + evAvailable;
    }

    /**
     * Generera säkerhetsvarningar
     */
    public String[] generateSafetyWarnings(InfoResponse info) {
        java.util.List<String> warnings = new java.util.ArrayList<>();
        
        if (isCriticalBattery(info.getHomeBattCapacityPercent())) {
            warnings.add("⚠️ Kritisk batterinivå - Ladda husbatteriet omedelbart");
        } else if (isLowBatteryWarning(info.getHomeBattCapacityPercent())) {
            warnings.add("⚠️ Låg batterinivå - Överväg laddning");
        }
        
        double reserveHours = calculateReserveHours(info);
        if (reserveHours < 2.0) {
            warnings.add("⚠️ Mindre än 2 timmar reservkraft kvar");
        }
        
        if (!isSafeForV2H(info.getBatteryEnergyKwh() / info.getEvBattMaxCapacityKwh() * 100, 
                         info.getHomeBattCapacityPercent())) {
            warnings.add("⚠️ Inte säkert för V2H-användning");
        }
        
        return warnings.toArray(new String[0]);
    }

    /**
     * Ladda ur husbatteriet till 10% (för testning av off-grid scenarion)
     */
    public void dischargeHomeBatteryTo10Api() {
        try {
            System.out.println("HomeBatteryManager: Sending command to discharge home battery to 10%...");
            String response = apiClient.dischargeHomeBatteryTo10();
            System.out.println("HomeBatteryManager: Server response: " + response);
        } catch (Exception e) {
            System.err.println("HomeBatteryManager: Failed to discharge home battery: " + e.getMessage());
        }
    }
} 