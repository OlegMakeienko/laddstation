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

            ObjectMapper objectMapper = new ObjectMapper();

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
            ObjectMapper objectMapper = new ObjectMapper();
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
            ObjectMapper objectMapper = new ObjectMapper();
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

    @Override
    public void chargingSessionOnOptimalChargingHours() {
        OptimalHoursStrategy strategy = new ConsumptionBasedStrategy(restTemplate);
        performChargingSessionWithStrategy(strategy);
    }

    private List<Double> findOptimalChargingHoursWhenConsumptionIsLow() throws JsonProcessingException {
        List<Double> optimalHours = new ArrayList<>();
        // Laddstationens effekt + hushållsförbrukning mindre än 11 kW
        // Hämta JSON som en lista
        String jsonResponse = restTemplate.getForObject("http://127.0.0.1:5001/baseload", String.class);

        ObjectMapper objectMapper = new ObjectMapper();

        // Deserialisera JSON till en lista av förbrukningsvärden
        double[] hourlyBaseload = objectMapper.readValue(jsonResponse, double[].class);

        double optimalHour;

        // Iterera genom varje timme för att hitta den bästa timmen för laddning
        for (int hour = 0; hour < hourlyBaseload.length; hour++) {
            // Kontrollera om totala förbrukningen (inklusive laddstation) är under 11 kW
            double totalLoad = hourlyBaseload[hour] + 7.4; // Laddstationens effekt + hushållsförbrukning
            if (totalLoad <= 11.0) {
                // Logga timmen om den är optimal för laddning
                System.out.println("Hour " + hour + " is optimal for charging with total load: "
                        + String.format("%.2f", totalLoad) + " kW");

                optimalHour = hour;
                optimalHours.add(optimalHour);
            }
        }
        return optimalHours;
    }

    private List<Double> findOptimalChargingHourGroundLowPrice() throws JsonProcessingException {
        List<Double> optimalHours = new ArrayList<>();
        // Laddstationens effekt + hushållsförbrukning mindre än 11 kW
        // Hämta JSON som en lista
        String jsonResponse1 = restTemplate.getForObject("http://127.0.0.1:5001/priceperhour", String.class);
        String jsonResponse2 = restTemplate.getForObject("http://127.0.0.1:5001/baseload", String.class);

        ObjectMapper objectMapper = new ObjectMapper();

        // Deserialisera JSON till en lista av priser
        double[] hourlyPrices = objectMapper.readValue(jsonResponse1, double[].class);
        // Deserialisera JSON till en lista av förbrukningsvärden
        double[] hourlyBaseload = objectMapper.readValue(jsonResponse2, double[].class);

        double optimalHour;
        double lowestCost = Double.MAX_VALUE;

        // Iterera genom varje timme för att hitta den bästa timmen för laddning
        for (int hour = 0; hour < hourlyBaseload.length; hour++) {
            // Kontrollera om totala förbrukningen (inklusive laddstation) är under 11 kW
            double totalLoad = hourlyBaseload[hour] + 7.4; // Laddstationens effekt + hushållsförbrukning
            if (totalLoad <= 11.0) {
                // Logga timmen om den är optimal för laddning
                System.out.println("Hour " + hour + " is optimal for charging with total load: "
                        + String.format("%.2f", totalLoad) + " kW");
                // Logga priset för den optimala timmen
                System.out.println("Price for Hour :" + String.format("%.2f", hourlyPrices[hour]) + " per kWh");

                optimalHour = hour;

                // Beräkna kostnaden för denna timme
                double cost = hourlyPrices[hour];
                if (cost < lowestCost) {
                    lowestCost = cost;
                    optimalHours.add(optimalHour);
                }
            }
        }

        return optimalHours;
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
