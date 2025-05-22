package com.makeienko.laddstation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.makeienko.laddstation.dto.ChargingSession;
import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.exception.ChargingServiceException;
import com.makeienko.laddstation.service.strategy.OptimalHoursStrategy;
import com.makeienko.laddstation.service.strategy.PriceBasedStrategy;
import com.makeienko.laddstation.service.strategy.ConsumptionBasedStrategy;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ChargingServiceImpl implements ChargingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChargingServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public InfoResponse fetchAndDeserializeInfo() {
        try {
            String jsonResponse = restTemplate.getForObject("http://127.0.0.1:5001/info", String.class);
            if (jsonResponse == null) {
                throw new ChargingServiceException("Received null response from info endpoint");
            }

            // Deserialisera JSON-strängen till InfoResponse
            InfoResponse infoResponse = objectMapper.readValue(jsonResponse, InfoResponse.class);
            
            if (infoResponse == null) {
                throw new ChargingServiceException("Failed to deserialize response to InfoResponse object");
            }
            
            return infoResponse;
        } catch (RestClientException e) {
            throw new ChargingServiceException("Failed to fetch data from info endpoint: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new ChargingServiceException("Failed to process JSON response: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ChargingServiceException("Unexpected error while fetching info: " + e.getMessage(), e);
        }
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
            // Hämta JSON som en lista från /priceperhour
            String jsonResponse = restTemplate.getForObject("http://127.0.0.1:5001/priceperhour", String.class);

            // Deserialisera JSON till en lista av priser
            double[] hourlyPrices = objectMapper.readValue(jsonResponse, double[].class);

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
            // Hämta JSON från /baseload
            String jsonResponse = restTemplate.getForObject("http://127.0.0.1:5001/baseload", String.class);

            // Deserialisera JSON till en lista av förbrukningsvärden
            double[] hourlyBaseload = objectMapper.readValue(jsonResponse, double[].class);

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
        chargeBattery();
    }

    private boolean isBatterySufficient() {
        try {
            InfoResponse infoResponse = fetchAndDeserializeInfo();
            if (infoResponse != null) {
                double batteryPercentage = (infoResponse.getBaseCurrentLoad() / infoResponse.getBatteryCapacityKWh()) * 100;
                //System.out.println("Current Battery Level Before Charging: " + batteryPercentage + "%");
                return batteryPercentage >= 80;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // Anta att laddning behövs om vi inte kan hämta status
    }

    private void startCharging() throws Exception {
        String response = restTemplate.postForObject(
                "http://127.0.0.1:5001/charge",
                Map.of("charging", "on"),
                String.class
        );
        System.out.println("Charging started: " + response);
    }

    private void stopCharging() throws Exception {
        String response = restTemplate.postForObject(
                "http://127.0.0.1:5001/charge",
                Map.of("charging", "off"),
                String.class
        );
        System.out.println("Charging stopped: " + response);
    }
    @Override
    public void chargingSessionOnOptimalChargingHoursPrice() {
        OptimalHoursStrategy strategy = new PriceBasedStrategy(restTemplate);
        performChargingSessionWithStrategy(strategy);
    }

    private void chargeBattery() {
        ChargingSession chargingSession = new ChargingSession();
        chargingSession.setChargingPower(7.4); // Laddstationens effekt i kW

            try {
                // Hämta och logga batterinivå från servern
                InfoResponse infoResponse = fetchAndDeserializeInfo();
                if (infoResponse != null) {
                    updateBatteryStatus(chargingSession, infoResponse);

                    while (chargingSession.getBatteryPercentage() < 80) {
                        // Energi som laddas per iteration (15 minuter simulerad tid)
                        double energyAddedPerIteration = 7.4 * 15 / 60; // 1.85 kWh

                        // Uppdatera batterinivån
                        chargingSession.updateBatteryLoad(energyAddedPerIteration);

                        // Logga nuvarande status
                        System.out.println("Charging: currently battery Level: " + chargingSession.getBatteryPercentage() + "%");

                        // Vänta 1 verklig sekund
                        Thread.sleep(1000);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    private void updateBatteryStatus(ChargingSession chargingSession, InfoResponse infoResponse) {
        double currentBatteryLoad = infoResponse.getBaseCurrentLoad();
        double batteryCapacity = infoResponse.getBatteryCapacityKWh();

        chargingSession.setCurrentLoad(currentBatteryLoad);
        chargingSession.setBatteryCapacity(batteryCapacity);

        // Beräkna och uppdatera batteriprocent
        double batteryPercentage = (currentBatteryLoad / batteryCapacity) * 100;
        batteryPercentage = Math.min(batteryPercentage, 100); // Begränsa till max 100%
        //allrunda till 1 decimail
        batteryPercentage = Math.round(batteryPercentage * 10.0) / 10.0;
        chargingSession.setBatteryPercentage(batteryPercentage);

        // Logga information
        System.out.println("Battery Capacity (kWh): " + batteryCapacity);
        System.out.println("Current Load (kWh): " + currentBatteryLoad);
        System.out.println("Current battery Level: " + batteryPercentage + "%");
    }

    private void dischargeBatteryTo20() {
        try {
            String response = restTemplate.postForObject(
                    "http://127.0.0.1:5001/discharge",
                    Map.of("discharging", "on"),
                    String.class
            );
            System.out.println("Battery reset to 20%: " + response);
        } catch (Exception e) {
            System.err.println("Error resetting battery: " + e.getMessage());
        }
    }

    private boolean isOptimalHour(double currentHour, List<Double> optimalHours) {
        for (double hour : optimalHours) {
            if (currentHour == hour) {
                return true;
            }
        }
        return false;
    }

    private void waitUntilNextHour(double currentMinute) throws InterruptedException {
        double minutesToWait = 60 - currentMinute; // Beräkna minuter kvar till nästa timme
        double simulatedTimeInSeconds = minutesToWait / 15; // Konvertera minuter till simulerad tid i sekunder
        System.out.println("Waiting for " + minutesToWait + " simulated minutes ("
                + simulatedTimeInSeconds + " real seconds) until the next hour...");
        Thread.sleep((long) (simulatedTimeInSeconds * 1000L)); // Vänta den beräknade tiden i verkliga sekunder
    }

    private Double findNextOptimalChargingHour(int currentHour, List<Double> optimalHours) {
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
    public void chargingSessionOnOptimalChargingHours() {
        try {
            //Skapa strategi för att hitta optimala timmar
            OptimalHoursStrategy strategy = new ConsumptionBasedStrategy(restTemplate);
            List<Double> optimalHours = strategy.findOptimalHours();

            boolean isCharging = false;
            boolean targetReached = false;

            System.out.println("Starting smart charging sessioon with consumption-based optimization");
            System.out.println("Optimal hours for charging: " + optimalHours);

            while (!targetReached) {
                InfoResponse infoResponse = fetchAndDeserializeInfo();
                if (infoResponse == null) {
                    System.out.println("Faile to fetch information. Retrying in 10 sec...");
                    Thread.sleep(10000);
                    continue;
                }

                double currentHour = infoResponse.getSimTimeHour();
                double currentMinute = infoResponse.getSimTimeMin(); // Needed for waitUntilNextHour
                boolean isCurrentHourOptimal = isOptimalHour(currentHour, optimalHours);

                //Kontrollera om vi har nått målladdningsnivå (ca 80%)
                if (isBatterySufficient()) {
                    if (isCharging) {
                        System.out.println("Battery reached target level (>=80%) while charging. Stopping charging.");
                        stopCharging();
                        isCharging = false;
                    } else {
                        System.out.println("Battery already at target level (>=80%). No charging needed.");
                    }
                    System.out.println("Charging complete or not needed.");
                    targetReached = true;
                    continue; // Exit the while loop
                }

                //Hantera ladning baserat på om timmen är optimal
                if (isCurrentHourOptimal) {
                    if (!isCharging) {
                        System.out.println("Optimal hour (" + currentHour + ") started. Starting charging.");
                        startCharging();
                        isCharging = true;
                    }
                    // Whether we just started or were already charging, charge for a bit
                    System.out.println("Charging during optimal hour " + currentHour + ".");
                    chargeBatteryPartial(); // This method contains a sleep corresponding to its charge duration (15 sim minutes)
                } else { // Current hour is NOT optimal
                    if (isCharging) {
                        // Attempt to get the previous hour for logging, handling wrap-around from 0 to 23
                        double previousHour = (currentHour == 0) ? 23 : currentHour -1;
                        System.out.println("Optimal hour (" + previousHour + ") ended. Current non-optimal hour: " + currentHour + ". Stopping charging.");
                        stopCharging();
                        isCharging = false;
                    }
                    // Whether we just stopped or were already waiting, wait until the next hour
                    System.out.println("Current hour (" + currentHour + ") is not optimal. Waiting until the next simulated hour begins.");
                    waitUntilNextHour(currentMinute);
                }
            } // end while
        } catch (Exception e) {
            // Ensure InterruptedException is handled correctly if thrown by Thread.sleep or waitUntilNextHour
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

    //laddar en del batteriet och returerar när en mindre laddning är klar
    private void chargeBatteryPartial() {
        try {
            //Kortare laddningtid för mer frekvent koontroll
            ChargingSession session = new ChargingSession();
            session.setChargingPower(7.4);

            InfoResponse info = fetchAndDeserializeInfo();
            updateBatteryStatus(session, info);

            //Ladda bara en kort period (motsvarande cirka 10-15 min simularede tid)
            double energyToAdd = session.getChargingPower() * 15 / 60; // 1.85 kWh för 15 min
            session.updateBatteryLoad(energyToAdd);

            System.out.println("Partial charge completed. Currently battery level: " + session.getBatteryPercentage() + "%");
            Thread.sleep(1000);
        } catch (Exception e) {
            throw new ChargingServiceException("Error during partial charge: ", e);
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
                    startCharging();

                    if (!isBatterySufficient()) {
                        chargeBattery();
                    }

                    stopCharging();
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
