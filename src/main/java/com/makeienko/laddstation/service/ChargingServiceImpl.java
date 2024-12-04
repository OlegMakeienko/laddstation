package com.makeienko.laddstation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.makeienko.laddstation.dto.ChargingSession;
import com.makeienko.laddstation.dto.InfoResponse;import com.makeienko.laddstation.dto.PricePerHourResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class ChargingServiceImpl implements ChargingService {

    private final RestTemplate restTemplate;

    public ChargingServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public InfoResponse fetchAndDeserializeInfo() {
        try {
            // Hämta JSON som String
            String jsonResponse = restTemplate.getForObject("http://127.0.0.1:5000/info", String.class);

            // Skapa en ObjectMapper för att deserialisera JSON
            ObjectMapper objectMapper = new ObjectMapper();

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

    @Override
    public void fetchAndDisplayBaseload() {
        try {
            // Hämta JSON från /baseload
            String jsonResponse = restTemplate.getForObject("http://127.0.0.1:5000/baseload", String.class);

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
    public void manageChargingSession() {
        try {
            // Starta laddningen (anropa /charge för att starta)
            String startChargingResponse = restTemplate.postForObject("http://127.0.0.1:5000/charge", null, String.class);
            System.out.println("Charging started: " + startChargingResponse);

            // Skapa en ChargingSession för att hålla koll på batteristatus
            ChargingSession chargingSession = new ChargingSession();

            // Börja en loop för att övervaka laddning
            while (chargingSession.getBatteryPercentage() < 80) {
                // Hämta batteriinformation via /info
                InfoResponse infoResponse = fetchAndDeserializeInfo();

                if (infoResponse != null) {

                    // Uppdatera ChargingSession med batteriprocenten
                    double currentBatteryLevelKWh = infoResponse.getBatteryCapacityKWh();
                    // Hämta den aktuella batteriladdningen från infoResponse
                    double currentBatteryLoad = infoResponse.getBaseCurrentLoad();

                    double batteryPercentage = (currentBatteryLoad / currentBatteryLevelKWh) * 100;
                    chargingSession.setBatteryPercentage(batteryPercentage);

                    // Säkerställ att batteriprocenten ligger inom intervallet 0-100
                    batteryPercentage = Math.min(Math.max(batteryPercentage, 0), 100);
                    chargingSession.setBatteryPercentage(batteryPercentage);

                    // Skriv ut nuvarande batteriprocent
                    System.out.println("Current battery level: " + chargingSession.getBatteryPercentage() + "%");
                }

                // Vänta innan vi kontrollerar batteristatusen igen (t.ex. vänta 5 sekunder)
                Thread.sleep(5000); // Vänta 5 sekunder
            }

            // När batteriet når 80%, stoppa laddningen
            String stopChargingResponse = restTemplate.postForObject("http://127.0.0.1:5000/charge", null, String.class);
            System.out.println("Charging stopped: " + stopChargingResponse);

        } catch (Exception e) {
            e.printStackTrace();  // Hantera eventuella fel vid laddning
        }
    }

    public void manageChargingFrom20To80() {
        // Skapa en ChargingSession för att hålla koll på batteristatus
        ChargingSession chargingSession = new ChargingSession();

        try {
            // Hämta JSON från /baseload
            String jsonResponse = restTemplate.getForObject("http://127.0.0.1:5000/baseload", String.class);

            // Deserialisera JSON till en lista av förbrukningsvärden
            ObjectMapper objectMapper = new ObjectMapper();
            double[] hourlyBaseload = objectMapper.readValue(jsonResponse, double[].class);

            // Hitta den timme när förbrukningen är som lägst
            int lowestLoadHour = getLowestLoadHour(hourlyBaseload);  // Funktion som hittar timme med lägst förbrukning

            // Hämta den aktuella timmen för att avgöra om vi är på rätt tid att ladda
            double currentHouseholdLoad = hourlyBaseload[lowestLoadHour];

            // Kontrollera om den totala förbrukningen plus laddstationens effekt är mindre än 11 kW
            double totalLoad = currentHouseholdLoad + 7.4; // Laddstationen ger 7.4 kW
            if (totalLoad <= 11.0) {
                System.out.println("Total load is under 11kW, starting charging process...");

                // Starta laddningen (anropa /charge för att starta)
                String startChargingResponse = restTemplate.postForObject("http://127.0.0.1:5000/charge", null, String.class);
                System.out.println("Charging started: " + startChargingResponse);

                // Ladda batteriet tills den når 80%
                while (chargingSession.getBatteryPercentage() < 80) {
                    InfoResponse infoResponse = fetchAndDeserializeInfo(); // Hämta aktuell batteriinformation

                    if (infoResponse != null) {
                        // Hämta aktuell batteriladdning
                        double currentBatteryLoad = infoResponse.getBaseCurrentLoad();
                        double batteryCapacity = infoResponse.getBatteryCapacityKWh();

                        // Beräkna batteriprocent
                        double batteryPercentage = (currentBatteryLoad / batteryCapacity) * 100;
                        batteryPercentage = Math.min(Math.max(batteryPercentage, 0), 100); // Säkerställ att det är mellan 0 och 100

                        chargingSession.setBatteryPercentage(batteryPercentage);
                        System.out.println("Current battery level: " + chargingSession.getBatteryPercentage() + "%");

                        // Vänta 5 sekunder innan vi kollar igen
                        Thread.sleep(5000);
                    }
                }

                // När batteriet når 80%, stoppa laddningen
                String stopChargingResponse = restTemplate.postForObject("http://127.0.0.1:5000/charge", null, String.class);
                System.out.println("Charging stopped: " + stopChargingResponse);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Hantera eventuella fel
        }
    }

    public void chargeWhenLowestPrice() {
        ChargingSession chargingSession = new ChargingSession();

        try {
            // Hämta JSON från /baseload och /priceperhour
            String baseloadResponse = restTemplate.getForObject("http://127.0.0.1:5000/baseload", String.class);
            String priceResponse = restTemplate.getForObject("http://127.0.0.1:5000/priceperhour", String.class);

            // Deserialisera JSON till arrays av förbrukningsvärden och priser
            ObjectMapper objectMapper = new ObjectMapper();
            double[] hourlyBaseload = objectMapper.readValue(baseloadResponse, double[].class);
            double[] hourlyPrices = objectMapper.readValue(priceResponse, double[].class);

            // Hitta den timme när elpriset är som lägst
            int lowestPriceHour = getLowestPriceHour(hourlyPrices); // Funktion som hittar timme med lägsta elpriset

            // Hämta hushållets förbrukning vid den timmen
            double currentHouseholdLoad = hourlyBaseload[lowestPriceHour];

            // Kontrollera om den totala förbrukningen plus laddstationens effekt är mindre än 11 kW
            double totalLoad = currentHouseholdLoad + 7.4; // Laddstationen ger 7.4 kW
            if (totalLoad <= 11.0) {
                System.out.println("Total load is under 11kW, and price is lowest. Starting charging process...");

                // Starta laddningen (anropa /charge för att starta)
                String startChargingResponse = restTemplate.postForObject("http://127.0.0.1:5000/charge", null, String.class);
                System.out.println("Charging started: " + startChargingResponse);

                // Ladda batteriet tills det når 80%
                while (chargingSession.getBatteryPercentage() < 80) {
                    InfoResponse infoResponse = fetchAndDeserializeInfo(); // Hämta aktuell batteriinformation

                    if (infoResponse != null) {
                        // Hämta aktuell batteriladdning
                        double currentBatteryLoad = infoResponse.getBaseCurrentLoad();
                        double batteryCapacity = infoResponse.getBatteryCapacityKWh();

                        // Beräkna batteriprocent
                        double batteryPercentage = (currentBatteryLoad / batteryCapacity) * 100;
                        batteryPercentage = Math.min(Math.max(batteryPercentage, 0), 100); // Säkerställ att det är mellan 0 och 100

                        chargingSession.setBatteryPercentage(batteryPercentage);
                        System.out.println("Current battery level: " + chargingSession.getBatteryPercentage() + "%");

                        // Vänta 5 sekunder innan vi kollar igen
                        Thread.sleep(5000);
                    }
                }

                // När batteriet når 80%, stoppa laddningen
                String stopChargingResponse = restTemplate.postForObject("http://127.0.0.1:5000/charge", null, String.class);
                System.out.println("Charging stopped: " + stopChargingResponse);
            } else {
                System.out.println("Total load exceeds 11kW. Charging not started.");
            }
        } catch (Exception e) {
            e.printStackTrace(); // Hantera eventuella fel
        }
    }

    private int getLowestPriceHour(double[] prices) {
        int lowestIndex = 0;
        double lowestValue = prices[0];

        for (int i = 1; i < prices.length; i++) {
            if (prices[i] < lowestValue) {
                lowestValue = prices[i];
                lowestIndex = i;
            }
        }
        return lowestIndex;
    }

    private int getLowestLoadHour(double[] baseload) {
        int lowestIndex = 0; // Starta med första indexet
        double lowestValue = baseload[0]; // Starta med första värdet

        // Iterera genom arrayen
        for (int i = 1; i < baseload.length; i++) {
            if (baseload[i] < lowestValue) {
                lowestValue = baseload[i]; // Uppdatera det lägsta värdet
                lowestIndex = i;          // Uppdatera indexet för det lägsta värdet
            }
        }
        return lowestIndex; // Returnera indexet för timmen med lägst förbrukning
    }
}
