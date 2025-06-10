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
    private final EVBatteryManager batteryManager;
    private final ChargingHourOptimizer chargingHourOptimizer;

    public ChargingServiceImpl(LaddstationApiClient apiClient, EVBatteryManager batteryManager, ChargingHourOptimizer chargingHourOptimizer) {
        this.apiClient = apiClient;
        this.batteryManager = batteryManager;
        this.chargingHourOptimizer = chargingHourOptimizer;
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
            System.out.println("--------------------------------");
            System.out.println("Simulated Time: " + infoResponse.getSimTimeHour() + " hours, " + infoResponse.getSimTimeMin() + " minutes");
            System.out.println("Household Load: " + infoResponse.getHouseholdLoadKwh() + " kW");
            System.out.println("EV Battery Energy: " + infoResponse.getEvBatteryEnergyKwh() + " kWh");
            System.out.println("EV Battery Charge Start/Stop: " + (infoResponse.isEvBatteryChargeStartStopp() ? "Start" : "Stop"));
            System.out.println("EV Battery Max Capacity: " + infoResponse.getEvBattMaxCapacityKwh() + " kWh");
            System.out.println("--------------------------------");
        } else {
            System.out.println("Failed to fetch and deserialize InfoResponse.");
        }
    }

    @Override
    public void fetchAndDisplayPriceForElZone() {
        try {
            // Hämta priser från API-klient
            double[] hourlyPrices = apiClient.getHourlyPrices();

            // Skriv ut priserna för varje timme
            System.out.println("--------------------------------");
            System.out.println("Elpriser för elområde (Stockholm):");
            for (int i = 0; i < hourlyPrices.length; i++) {
                System.out.printf("Timme %d: %.2f öre/kWh%n", i, hourlyPrices[i]);
            }
            System.out.println("--------------------------------");
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
            System.out.println("--------------------------------");
            System.out.println("Hushållets energiförbrukning (kWh per timme):");
            double totalConsumption = 0;
            for (int i = 0; i < hourlyBaseload.length; i++) {
                System.out.printf("Timme %d: %.2f kWh%n", i, hourlyBaseload[i]);
                totalConsumption += hourlyBaseload[i];
            }
            // Skriv ut total förbrukning under dygnet
            System.out.printf("Total förbrukning för dygnet: %.2f kWh%n", totalConsumption);
            System.out.println("--------------------------------");
        } catch (Exception e) {
            System.err.println("Fel vid hämtning av baseload-information: " + e.getMessage());
        }
    }

    @Override
    public void chargeEVBatteryDirect() {
        System.out.println("--------------------------------");
        System.out.println("Starting direct charge to 80%.");
        
        // Display initial battery info before starting
        InfoResponse initialInfo = apiClient.getInfo();
        if (initialInfo != null) {
            double currentPercentage = (initialInfo.getEvBatteryEnergyKwh() / initialInfo.getEvBattMaxCapacityKwh()) * 100;
            currentPercentage = Math.round(currentPercentage * 10.0) / 10.0;
            System.out.println("Initial EV Battery Energy: " + initialInfo.getEvBatteryEnergyKwh() + " kWh");
            System.out.println("Initial EV Battery Max Capacity: " + initialInfo.getEvBattMaxCapacityKwh() + " kWh");
            System.out.println("Initial EV Battery Level: " + currentPercentage + "%");
        } else {
            System.err.println("Could not fetch initial battery info before direct charge.");
        }

        batteryManager.startChargingApi(); // Tala om för servern att börja ladda
        boolean targetReached = batteryManager.chargeEVBatteryUntilTarget();
        if (targetReached) {
            System.out.println("Direct charge completed, target reached.");
        }
        // Stoppa alltid laddningen på servern efteråt, oavsett om målet nåddes eller det avbröts.
        // Om chargeBatteryUntilTarget avbröts (returnerade false) har den redan försökt stoppa.
        System.out.println("Ensuring charging is stopped on server after direct charge attempt.");
        batteryManager.stopChargingApi(); 
        System.out.println("--------------------------------");
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
            System.out.println("--------------------------------");
            System.out.println("Starting smart charging session.");
            List<Double> optimalHours = strategy.findOptimalHours();

            if (optimalHours.isEmpty()) {
                System.out.println("No optimal hours found. Cannot start charging session.");
                return;
            }

            boolean isCurrentlyCharging = false;

            while (!batteryManager.isEVBatterySufficient()) {
                InfoResponse infoResponse = apiClient.getInfo();
                if (infoResponse == null) {
                    System.err.println("Failed to fetch info. Retrying in 10 sec...");
                    Thread.sleep(10000);
                    continue;
                }

                double currentHour = infoResponse.getSimTimeHour();
                double currentMinute = infoResponse.getSimTimeMin();
                boolean isCurrentHourOptimal = isOptimalHour(currentHour, optimalHours);

                if (isCurrentHourOptimal) {
                    if (!isCurrentlyCharging) {
                        System.out.println("Optimal hour (" + currentHour + ") started. Starting charging on server.");
                        batteryManager.startChargingApi();
                        isCurrentlyCharging = true;
                    }
                    System.out.println("Charging during optimal hour " + currentHour + ". Simulating 15 min charge period.");
                    batteryManager.simulateChargingPeriod(15); // Servern laddar i 15 sim-minuter
                } else {
                    if (isCurrentlyCharging) {
                        System.out.println("Optimal hour ended. Current non-optimal hour: " + currentHour + ". Stopping charging on server.");
                        batteryManager.stopChargingApi();
                        isCurrentlyCharging = false;
                    }
                    System.out.println("Current hour (" + currentHour + ") is not optimal. Waiting until the next simulated hour begins.");
                    waitUntilNextHour(currentMinute);
                }
            }

            // Batteriet är tillräckligt laddat
            if (isCurrentlyCharging) {
                System.out.println("Target battery level reached. Stopping charging on server.");
                batteryManager.stopChargingApi();
            }
            System.out.println("Smart charging session complete. Battery is sufficiently charged.");
            System.out.println("--------------------------------");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Smart charging session was interrupted.");
            batteryManager.stopChargingApi(); // Försök stoppa laddningen om tråden avbryts
        } catch (Exception e) {
            System.err.println("Error in smart charging session: " + e.getMessage());
            batteryManager.stopChargingApi(); // Försök stoppa laddningen vid fel
            throw new ChargingServiceException("Error in smart charging session with strategy.", e);
        }
    }

    @Override
    public void dischargeEVBatteryTo20() {
        System.out.println("Initiating discharge to 20%.");
        batteryManager.dischargeEVBatteryTo20Api();
        // We might want to poll here until 20% is confirmed, or trust the server handles it.
        // For now, just calling the API and printing a message.
        System.out.println("Discharge command sent to server.");
        // Optionally, display battery status after a short delay
        try {
            Thread.sleep(2000); // Wait 2 seconds for server to process
            displayInfoResponse();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while waiting to display info after discharge command.");
        }
    }
}
