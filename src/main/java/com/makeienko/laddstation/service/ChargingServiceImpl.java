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

    public ChargingServiceImpl(LaddstationApiClient apiClient, BatteryManager batteryManager) {
        this.apiClient = apiClient;
        this.batteryManager = batteryManager;
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
            System.out.println("Base Current Load: " + infoResponse.getBaseCurrentLoad() + " kW");
            System.out.println("Battery Capacity: " + infoResponse.getBatteryCapacityKWh() + " kWh");
            System.out.println("EV Battery Charge Start/Stop: " + (infoResponse.isEvBatteryChargeStartStopp() ? "Start" : "Stop"));
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
    public void chargeBatteryDirect() {
        batteryManager.chargeBattery();
    }

    boolean isOptimalHour(double currentHour, List<Double> optimalHours) {
        for (double hour : optimalHours) {
            if (currentHour == hour) {
                return true;
            }
        }
        return false;
    }

    void waitUntilNextHour(double currentMinute) throws InterruptedException {
        double minutesToWait = 60 - currentMinute; // Beräkna minuter kvar till nästa timme
        double simulatedTimeInSeconds = minutesToWait / 15; // Konvertera minuter till simulerad tid i sekunder
        System.out.println("Waiting for " + minutesToWait + " simulated minutes ("
                + simulatedTimeInSeconds + " real seconds) until the next hour...");
        Thread.sleep((long) (simulatedTimeInSeconds * 1000L)); // Vänta den beräknade tiden i verkliga sekunder
    }

    Double findNextOptimalChargingHour(int currentHour, List<Double> optimalHours) {
        // Sortera listan av optimala timmar (om den inte redan är sorterad)
        Collections.sort(optimalHours);

        // Leta efter den första timmen i listan som är större än currentHour
        for (Double hour : optimalHours) {
            if (hour > currentHour) {
                return hour; // Returnera nästa optimala timme
            }
        }

        // Om ingen timme är större än currentHour, återgå till den första timmen (cirkulärt)
        return optimalHours.get(0);
    }

    @Override
    public void chargingSessionOnOptimalChargingHoursPrice() {
        OptimalHoursStrategy strategy = new PriceBasedStrategy(apiClient);
        performChargingSessionWithStrategy(strategy);
    }

    @Override
    public void chargingSessionOnOptimalChargingHours() {
        try {
            //Skapa strategi för att hitta optimala timmar
            OptimalHoursStrategy strategy = new ConsumptionBasedStrategy(apiClient);
            List<Double> optimalHours = strategy.findOptimalHours();

            boolean isCharging = false;
            boolean targetReached = false;

            System.out.println("Starting smart charging session with consumption-based optimization");
            System.out.println("Optimal hours for charging: " + optimalHours);

            while (!targetReached) {
                InfoResponse infoResponse = fetchAndDeserializeInfo();
                if (infoResponse == null) {
                    System.out.println("Failed to fetch information. Retrying in 10 sec...");
                    Thread.sleep(10000);
                    continue;
                }

                double currentHour = infoResponse.getSimTimeHour();
                double currentMinute = infoResponse.getSimTimeMin(); // Needed for waitUntilNextHour
                boolean isCurrentHourOptimal = isOptimalHour(currentHour, optimalHours);

                //Kontrollera om vi har nått målladdningsnivå (ca 80%)
                if (batteryManager.isBatterySufficient()) {
                    if (isCharging) {
                        System.out.println("Battery reached target level (>=80%) while charging. Stopping charging.");
                        batteryManager.stopCharging();
                        isCharging = false;
                    } else {
                        System.out.println("Battery already at target level (>=80%). No charging needed.");
                    }
                    System.out.println("Charging complete or not needed.");
                    targetReached = true;
                    continue; // Exit the while loop
                }

                //Hantera ladding baserat på om timmen är optimal
                if (isCurrentHourOptimal) {
                    if (!isCharging) {
                        System.out.println("Optimal hour (" + currentHour + ") started. Starting charging.");
                        batteryManager.startCharging();
                        isCharging = true;
                    }
                    // Om vi just startade eller redan var på gång, ladda lite
                    System.out.println("Charging during optimal hour " + currentHour + ".");
                    batteryManager.chargeBatteryPartial(); // This method contains a sleep corresponding to its charge duration (15 sim minutes)
                } else { // Current hour is NOT optimal
                    if (isCharging) {
                        // Försök att hämta föregående timme för loggning, hantera wrap-around från 0 till 23
                        double previousHour = (currentHour == 0) ? 23 : currentHour -1;
                        System.out.println("Optimal hour (" + previousHour + ") ended. Current non-optimal hour: " + currentHour + ". Stopping charging.");
                        batteryManager.stopCharging();
                        isCharging = false;
                    }
                    // Vänta tills nästa timme börjar
                    System.out.println("Current hour (" + currentHour + ") is not optimal. Waiting until the next simulated hour begins.");
                    waitUntilNextHour(currentMinute);
                }
            } // end while
        } catch (Exception e) {
            // Se till att InterruptedException hanteras korrekt om den kastas av Thread.sleep eller waitUntilNextHour
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt(); // Preserve interrupt status
                System.err.println("Charging session was interrupted.");
            }
            throw new ChargingServiceException("Error in charging session with consumption strategy.", e);
        }
    }

    //Hjälpmetodd för att beräkna tid till nästa optimala timme
    private double calculateHoursToWait(double currentHour, double nextOptimalHour) {
        if (nextOptimalHour > currentHour) {
            return nextOptimalHour - currentHour;
        } else {
            //om nästa timmen kommer på nästa dag
            return 24 - currentHour + nextOptimalHour;
        }
    }

    @Override
    public void performChargingSessionWithStrategy(OptimalHoursStrategy strategy) {
        try {
            List<Double> optimalHours = strategy.findOptimalHours();

            while (true) {
                InfoResponse infoResponse = fetchAndDeserializeInfo();
                if(infoResponse == null) {
                    System.out.println("Failed to fetch current time from the server. Retrying...");
                    Thread.sleep(10000);
                    continue;
                }

                double currentHour = infoResponse.getSimTimeHour();

                if(isOptimalHour(currentHour, optimalHours)) {
                    System.out.println("Current hour (" + currentHour + ") is optimal for charging");
                    batteryManager.startCharging();

                    if (!batteryManager.isBatterySufficient()) {
                        batteryManager.chargeBattery();
                    }

                    batteryManager.stopCharging();
                    break;
                } else {
                    System.out.println("Current hour (" + currentHour + ") is not optimal for charging. Waiting...");
                    waitUntilNextHour(infoResponse.getSimTimeMin());
                }
            }
        } catch (Exception e) {
            throw new ChargingServiceException("Error during charging session", e);
        }
    }
}
