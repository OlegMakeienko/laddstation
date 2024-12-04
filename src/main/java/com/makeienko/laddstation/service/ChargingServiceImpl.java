package com.makeienko.laddstation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.makeienko.laddstation.dto.InfoResponse;import com.makeienko.laddstation.dto.PricePerHourResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ChargingServiceImpl implements ChargingService {

    private final RestTemplate restTemplate;

    public ChargingServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public InfoResponse fetchAndDeserializeInfo() {
        // Hämta JSON som String
        String jsonResponse = restTemplate.getForObject("http://127.0.0.1:5000/info", String.class);

        // Skapa en ObjectMapper för att deserialisera JSON
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Deserialisera JSON-strängen till InfoResponse
            InfoResponse infoResponse = objectMapper.readValue(jsonResponse, InfoResponse.class);
            //System.out.println(infoResponse.toString());
            return infoResponse;
        } catch (Exception e) {
            e.printStackTrace();  // Hantera eventuella fel vid deserialisering
            return null;
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
            String jsonResponse = restTemplate.getForObject("http://127.0.0.1:5000/priceperhour", String.class);

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
}
