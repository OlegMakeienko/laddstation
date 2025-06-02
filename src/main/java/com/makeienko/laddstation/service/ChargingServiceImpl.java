package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.exception.ChargingServiceException;
import com.makeienko.laddstation.service.strategy.OptimalHoursStrategy;
import com.makeienko.laddstation.service.strategy.PriceBasedStrategy;
import com.makeienko.laddstation.service.strategy.ConsumptionBasedStrategy;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChargingServiceImpl implements ChargingService {

    private final LaddstationApiClient apiClient;
    private final BatteryManager batteryManager;
    private final ChargingHourOptimizer chargingHourOptimizer;
    private final HomeBatteryManager homeBatteryManager;
    private final SolarPanelManager solarPanelManager;

    public ChargingServiceImpl(LaddstationApiClient apiClient, BatteryManager batteryManager, ChargingHourOptimizer chargingHourOptimizer, HomeBatteryManager homeBatteryManager, SolarPanelManager solarPanelManager) {
        this.apiClient = apiClient;
        this.batteryManager = batteryManager;
        this.chargingHourOptimizer = chargingHourOptimizer;
        this.homeBatteryManager = homeBatteryManager;
        this.solarPanelManager = solarPanelManager;
    }

    @Override
    public InfoResponse fetchAndDeserializeInfo() {
        return apiClient.getInfo();
    }

    @Override
    public void displayInfoResponse() {
        // Använd fetchAndDeserializeInfo för att hämta InfoResponse
        InfoResponse infoResponse = fetchAndDeserializeInfo();

        // Kontrollera att data hämtades korrekt
        if (infoResponse != null) {
            // Skriv ut objektets data i ett strukturerat format
            System.out.println("Simulated Time: " + infoResponse.getSimTimeHour() + " hours, " + infoResponse.getSimTimeMin() + " minutes");
            System.out.println("Household Load: " + infoResponse.getHouseholdLoadKwh() + " kW");
            
            // EV Battery information
            System.out.println("EV Battery Energy: " + infoResponse.getBatteryEnergyKwh() + " kWh");
            System.out.println("EV Battery Charge Start/Stop: " + (infoResponse.isEvBatteryChargeStartStopp() ? "Start" : "Stop"));
            System.out.println("EV Battery Max Capacity: " + infoResponse.getEvBattMaxCapacityKwh() + " kWh");
            
            // Home Battery information
            System.out.println("Home Battery Energy: " + infoResponse.getHomeBattCapacityKwh() + " kWh");
            System.out.println("Home Battery Capacity: " + infoResponse.getHomeBattCapacityPercent() + "%");
            System.out.println("Home Battery Max Capacity: " + infoResponse.getHomeBattMaxCapacityKwh() + " kWh");
            System.out.println("Home Battery Min Capacity: " + infoResponse.getHomeBattMinCapacityKwh() + " kWh");
            System.out.println("Home Battery Mode: " + infoResponse.getHomeBatteryMode());
            
            // Solar Panel information
            try {
                com.makeienko.laddstation.dto.SolarPanelStatus solarStatus = solarPanelManager.getSolarPanelStatus();
                System.out.println("\n=== SOLPANEL STATUS ===");
                System.out.println("Solar Production: " + solarStatus.getCurrentProductionKwh() + " kWh");
                System.out.println("Solar Max Capacity: " + solarStatus.getMaxCapacityKwh() + " kWh");
                System.out.println("Solar Production Percent: " + solarStatus.getProductionPercent() + "%");
                System.out.println("Solar Production Status: " + solarStatus.getProductionStatus());
                System.out.println("Energy Surplus: " + solarStatus.getEnergySurplus() + " kWh");
                System.out.println("Daily Production Estimate: " + String.format("%.2f", solarStatus.getDailyProductionEstimate()) + " kWh");
                System.out.println("Net Household Load: " + solarStatus.getNetHouseholdLoadKwh() + " kWh");
                
                if (solarStatus.getOptimizationTips().length > 0) {
                    System.out.println("\n💡 OPTIMERINGSTIPS:");
                    for (String tip : solarStatus.getOptimizationTips()) {
                        System.out.println("  " + tip);
                    }
                }
                System.out.println("========================");
            } catch (Exception e) {
                System.err.println("Error getting solar panel status: " + e.getMessage());
            }
        } else {
            System.out.println("Failed to fetch and deserialize InfoResponse.");
        }
    }

    @Override
    public void displayHomeBatteryStatus() {
        try {
            InfoResponse info = apiClient.getInfo();
            if (info == null) {
                System.out.println("Failed to fetch home battery info from server.");
                return;
            }

            System.out.println("\n=== HUSBATTERI STATUS ===");
            System.out.println("Batterinivå: " + info.getHomeBattCapacityPercent() + "%");
            System.out.println("Aktuell energi: " + info.getHomeBattCapacityKwh() + " kWh");
            System.out.println("Max kapacitet: " + info.getHomeBattMaxCapacityKwh() + " kWh");
            System.out.println("Min säker nivå: " + info.getHomeBattMinCapacityKwh() + " kWh");
            System.out.println("Läge: " + info.getHomeBatteryMode());

            // Hämta detaljerad status med säkerhetsinformation
            try {
                com.makeienko.laddstation.dto.HomeBatteryStatus status = homeBatteryManager.getHomeBatteryStatus();
                System.out.println("Hälsostatus: " + status.getHealthStatus());
                System.out.println("Reservkraft: " + String.format("%.1f", status.getReserveHours()) + " timmar");

                // Visa total tillgänglig energi
                double totalEnergy = homeBatteryManager.calculateTotalAvailableEnergy(info);
                System.out.println("Total tillgänglig energi (EV + Hus): " + String.format("%.2f", totalEnergy) + " kWh");

                // Visa säkerhetsvarningar
                String[] warnings = homeBatteryManager.generateSafetyWarnings(info);
                if (warnings.length > 0) {
                    System.out.println("\n⚠️ SÄKERHETSVARNINGAR:");
                    for (String warning : warnings) {
                        System.out.println("  " + warning);
                    }
                } else {
                    System.out.println("✅ Inga varningar - systemet är säkert");
                }

                // V2H säkerhetsstatus
                double evPercent = (info.getBatteryEnergyKwh() / info.getEvBattMaxCapacityKwh()) * 100;
                boolean v2hSafe = homeBatteryManager.isSafeForV2H(evPercent, info.getHomeBattCapacityPercent());
                System.out.println("V2H (Vehicle-to-Home) säkerhet: " + (v2hSafe ? "✅ Säkert" : "⚠️ Inte säkert"));

            } catch (Exception e) {
                System.err.println("Error getting detailed home battery status: " + e.getMessage());
            }

            System.out.println("========================\n");

        } catch (Exception e) {
            System.err.println("Error displaying home battery status: " + e.getMessage());
        }
    }

    @Override
    public void fetchAndDisplayPriceForElZone() {
        try {
            // Hämta priser från API-klient
            double[] hourlyPrices = apiClient.getHourlyPrices();

            // Skriv ut priserna för varje timme
            System.out.println("Elpriser för elområde (Stockholm):");
            for (int i = 0; i < hourlyPrices.length; i++) {
                System.out.printf("Timme %d: %.2f öre/kWh%n", i, hourlyPrices[i]);
            }
        } catch (Exception e) {
            System.err.println("Fel vid hämtning av prisinformation: " + e.getMessage());
        }
    }

    @Override
    public void fetchAndDisplayBaseload() {
        try {
            // Hämta baseload från API-klient
            double[] hourlyBaseload = apiClient.getBaseload();

            // Skriv ut hushållets energiförbrukning per timme
            System.out.println("Hushållets energiförbrukning (kWh per timme):");
            double totalConsumption = 0;
            for (int i = 0; i < hourlyBaseload.length; i++) {
                System.out.printf("Timme %d: %.2f kWh%n", i, hourlyBaseload[i]);
                totalConsumption += hourlyBaseload[i];
            }
            // Skriv ut total förbrukning under dygnet
            System.out.printf("Total förbrukning för dygnet: %.2f kWh%n", totalConsumption);
        } catch (Exception e) {
            System.err.println("Fel vid hämtning av baseload-information: " + e.getMessage());
        }
    }

    @Override
    public void fetchAndDisplaySolarProductionPerHour() {
        try {
            // Hämta solpanelproduktion från API-klient
            double[] hourlySolarProduction = apiClient.getSolarProductionPerHour();

            // Skriv ut solpanelproduktion per timme
            System.out.println("Solpanelproduktion (kWh per timme):");
            double totalProduction = 0;
            for (int i = 0; i < hourlySolarProduction.length; i++) {
                System.out.printf("Timme %d: %.2f kWh%n", i, hourlySolarProduction[i]);
                totalProduction += hourlySolarProduction[i];
            }
            // Skriv ut total produktion under dygnet
            System.out.printf("Total produktion för dygnet: %.2f kWh%n", totalProduction);
            
            // Visa solpanelens maxkapacitet
            InfoResponse info = apiClient.getInfo();
            if (info != null) {
                System.out.printf("Solpanelens maxkapacitet: %.1f kW%n", info.getSolarMaxCapacityKwh());
                double maxTheoreticalDaily = info.getSolarMaxCapacityKwh() * 24;
                double efficiencyPercent = (totalProduction / maxTheoreticalDaily) * 100;
                System.out.printf("Daglig verkningsgrad: %.1f%% (av teoretisk max %.2f kWh)%n", 
                    efficiencyPercent, maxTheoreticalDaily);
            }
        } catch (Exception e) {
            System.err.println("Fel vid hämtning av solpanelproduktion: " + e.getMessage());
        }
    }

    @Override
    public void chargeBatteryDirect() {
        System.out.println("ChargingServiceImpl: Starting direct charge to 80%.");
        
        // Display initial battery info before starting
        InfoResponse initialInfo = apiClient.getInfo();
        if (initialInfo != null) {
            double currentPercentage = (initialInfo.getBatteryEnergyKwh() / initialInfo.getEvBattMaxCapacityKwh()) * 100;
            currentPercentage = Math.round(currentPercentage * 10.0) / 10.0;
            System.out.println("Initial EV Battery Energy: " + initialInfo.getBatteryEnergyKwh() + " kWh");
            System.out.println("Initial EV Battery Max Capacity: " + initialInfo.getEvBattMaxCapacityKwh() + " kWh");
            System.out.println("Initial EV Battery Level: " + currentPercentage + "%");
        } else {
            System.err.println("ChargingServiceImpl: Could not fetch initial battery info before direct charge.");
        }

        batteryManager.startChargingApi(); // Tala om för servern att börja ladda
        boolean targetReached = batteryManager.chargeBatteryUntilTarget();
        if (targetReached) {
            System.out.println("ChargingServiceImpl: Direct charge completed, target reached.");
        }
        // Stoppa alltid laddningen på servern efteråt, oavsett om målet nåddes eller det avbröts.
        // Om chargeBatteryUntilTarget avbröts (returnerade false) har den redan försökt stoppa.
        System.out.println("ChargingServiceImpl: Ensuring charging is stopped on server after direct charge attempt.");
        batteryManager.stopChargingApi(); 
    }

    boolean isOptimalHour(double currentHour, List<Double> optimalHours) {
        return optimalHours.contains(currentHour);
    }

    void waitUntilNextHour(double currentMinute) throws InterruptedException {
        double minutesToWait = 60 - currentMinute;
        long realMillisecondsToWait = (long) (minutesToWait / 15.0 * 1000.0);
         if (realMillisecondsToWait <= 0) realMillisecondsToWait = 100; 

        System.out.println("Waiting for " + minutesToWait + " simulated minutes ("
                + (realMillisecondsToWait / 1000.0) + " real seconds) until the next hour...");
        Thread.sleep(realMillisecondsToWait);
    }

    // Denna metod används inte längre aktivt av strategierna
    Double findNextOptimalChargingHour(int currentHour, List<Double> optimalHours) {
        Collections.sort(optimalHours);

        // Leta efter den första timmen i listan som är större än currentHour
        for (Double hour : optimalHours) {
            if (hour > currentHour) {
                return hour; // Returnera nästa optimala timme
            }
        }
        return optimalHours.isEmpty() ? null : optimalHours.get(0);
    }

    @Override
    public void chargingSessionOnOptimalChargingHoursPrice() {
        OptimalHoursStrategy strategy = new PriceBasedStrategy(apiClient, chargingHourOptimizer);
        performSmartChargingSession(strategy);
    }

    @Override
    public void chargingSessionOnOptimalChargingHours() {
        OptimalHoursStrategy strategy = new ConsumptionBasedStrategy(apiClient, chargingHourOptimizer);
        performSmartChargingSession(strategy);
    }

    private void performSmartChargingSession(OptimalHoursStrategy strategy) {
        try {
            System.out.println("ChargingServiceImpl: Starting smart charging session.");
            List<Double> optimalHours = strategy.findOptimalHours();

            if (optimalHours.isEmpty()) {
                System.out.println("ChargingServiceImpl: No optimal hours found. Cannot start charging session.");
                return;
            }

            boolean isCurrentlyCharging = false;

            while (!batteryManager.isBatterySufficient()) {
                InfoResponse infoResponse = apiClient.getInfo();
                if (infoResponse == null) {
                    System.err.println("ChargingServiceImpl: Failed to fetch info. Retrying in 10 sec...");
                    Thread.sleep(10000);
                    continue;
                }

                double currentHour = infoResponse.getSimTimeHour();
                double currentMinute = infoResponse.getSimTimeMin();
                boolean isCurrentHourOptimal = isOptimalHour(currentHour, optimalHours);

                if (isCurrentHourOptimal) {
                    if (!isCurrentlyCharging) {
                        System.out.println("ChargingServiceImpl: Optimal hour (" + currentHour + ") started. Starting charging on server.");
                        batteryManager.startChargingApi();
                        isCurrentlyCharging = true;
                    }
                    System.out.println("ChargingServiceImpl: Charging during optimal hour " + currentHour + ". Simulating 15 min charge period.");
                    batteryManager.simulateChargingPeriod(15); // Servern laddar i 15 sim-minuter
                } else {
                    if (isCurrentlyCharging) {
                        System.out.println("ChargingServiceImpl: Optimal hour ended. Current non-optimal hour: " + currentHour + ". Stopping charging on server.");
                        batteryManager.stopChargingApi();
                        isCurrentlyCharging = false;
                    }
                    System.out.println("ChargingServiceImpl: Current hour (" + currentHour + ") is not optimal. Waiting until the next simulated hour begins.");
                    waitUntilNextHour(currentMinute);
                }
            }

            // Batteriet är tillräckligt laddat
            if (isCurrentlyCharging) {
                System.out.println("ChargingServiceImpl: Target battery level reached. Stopping charging on server.");
                batteryManager.stopChargingApi();
            }
            System.out.println("ChargingServiceImpl: Smart charging session complete. Battery is sufficiently charged.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("ChargingServiceImpl: Smart charging session was interrupted.");
            batteryManager.stopChargingApi(); // Försök stoppa laddningen om tråden avbryts
        } catch (Exception e) {
            System.err.println("ChargingServiceImpl: Error in smart charging session: " + e.getMessage());
            batteryManager.stopChargingApi(); // Försök stoppa laddningen vid fel
            throw new ChargingServiceException("Error in smart charging session with strategy.", e);
        }
    }

    // Ersatt av performSmartChargingSession för en mer robust logik
    @Deprecated
    @Override
    public void performChargingSessionWithStrategy(OptimalHoursStrategy strategy) {
        System.out.println("DEPRECATED: performChargingSessionWithStrategy is called, redirecting to performSmartChargingSession");
        performSmartChargingSession(strategy);
    }

    @Override
    public void dischargeEVBatteryTo20() {
        System.out.println("ChargingServiceImpl: Initiating EV battery discharge to 20%.");
        batteryManager.dischargeEVBatteryTo20Api();
        // We might want to poll here until 20% is confirmed, or trust the server handles it.
        // For now, just calling the API and printing a message.
        System.out.println("ChargingServiceImpl: EV battery discharge command sent to server.");
        // Optionally, display battery status after a short delay
        try {
            Thread.sleep(2000); // Wait 2 seconds for server to process
            displayInfoResponse();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("ChargingServiceImpl: Interrupted while waiting to display info after EV battery discharge command.");
        }
    }

    @Override
    public void dischargeHomeBatteryTo10() {
        System.out.println("ChargingServiceImpl: Initiating home battery discharge to 10%.");
        homeBatteryManager.dischargeHomeBatteryTo10Api();
        System.out.println("ChargingServiceImpl: Home battery discharge command sent to server.");
        // Display battery status after a short delay
        try {
            Thread.sleep(2000); // Wait 2 seconds for server to process
            displayInfoResponse();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("ChargingServiceImpl: Interrupted while waiting to display info after home battery discharge command.");
        }
    }
}
