package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.ChargingSession;
import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.exception.ChargingServiceException;
import org.springframework.stereotype.Component;

@Component
public class BatteryManager {
    
    private final LaddstationApiClient apiClient;
    
    public BatteryManager(LaddstationApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    /**
     * Laddar batteriet tills det når 80%
     */
    public void chargeBattery() {
        ChargingSession chargingSession = new ChargingSession();
        chargingSession.setChargingPower(7.4); // Laddstationens effekt i kW

        try {
            // Hämta och logga batterinivå från servern
            InfoResponse infoResponse = apiClient.getInfo();
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

    /**
     * Laddar en del av batteriet (motsvarar cirka 15 minuter simulerad tid)
     */
    public void chargeBatteryPartial() {
        try {
            //Kortare laddningtid för mer frekvent kontroll
            ChargingSession session = new ChargingSession();
            session.setChargingPower(7.4);

            InfoResponse info = apiClient.getInfo();
            updateBatteryStatus(session, info);

            //Ladda bara en kort period (motsvarande cirka 15 min simulerade tid)
            double energyToAdd = session.getChargingPower() * 15 / 60; // 1.85 kWh för 15 min
            session.updateBatteryLoad(energyToAdd);

            System.out.println("Partial charge completed. Currently battery level: " + session.getBatteryPercentage() + "%");
            Thread.sleep(1000);
        } catch (Exception e) {
            throw new ChargingServiceException("Error during partial charge: ", e);
        }
    }

    /**
     * Uppdaterar status för en laddningssession baserat på data från servern
     */
    public void updateBatteryStatus(ChargingSession chargingSession, InfoResponse infoResponse) {
        double currentBatteryLoad = infoResponse.getBaseCurrentLoad();
        double batteryCapacity = infoResponse.getBatteryCapacityKWh();

        chargingSession.setCurrentLoad(currentBatteryLoad);
        chargingSession.setBatteryCapacity(batteryCapacity);

        // Beräkna och uppdatera batteriprocent
        double batteryPercentage = (currentBatteryLoad / batteryCapacity) * 100;
        batteryPercentage = Math.min(batteryPercentage, 100); // Begränsa till max 100%
        //avrunda till 1 decimal
        batteryPercentage = Math.round(batteryPercentage * 10.0) / 10.0;
        chargingSession.setBatteryPercentage(batteryPercentage);

        // Logga information
        System.out.println("Battery Capacity (kWh): " + batteryCapacity);
        System.out.println("Current Load (kWh): " + currentBatteryLoad);
        System.out.println("Current battery Level: " + batteryPercentage + "%");
    }

    /**
     * Kontrollerar om batteriet är tillräckligt laddat (>=80%)
     */
    public boolean isBatterySufficient() {
        try {
            InfoResponse infoResponse = apiClient.getInfo();
            if (infoResponse != null) {
                double batteryPercentage = (infoResponse.getBaseCurrentLoad() / infoResponse.getBatteryCapacityKWh()) * 100;
                System.out.println("Current Battery Level Before Charging: " + batteryPercentage + "%");
                return batteryPercentage >= 80;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // Anta att laddning behövs om vi inte kan hämta status
    }

    /**
     * Startar laddning via API-anrop
     */
    public void startCharging() {
        try {
            String response = apiClient.startCharging();
            System.out.println("Charging started: " + response);
        } catch (Exception e) {
            throw new ChargingServiceException("Failed to start charging: " + e.getMessage(), e);
        }
    }

    /**
     * Stoppar laddning via API-anrop
     */
    public void stopCharging() {
        try {
            String response = apiClient.stopCharging();
            System.out.println("Charging stopped: " + response);
        } catch (Exception e) {
            throw new ChargingServiceException("Failed to stop charging: " + e.getMessage(), e);
        }
    }
    
    /**
     * Urladdning av batteriet till 20%
     */
    public void dischargeBatteryTo20() {
        try {
            String response = apiClient.dischargeBattery();
            System.out.println("Battery reset to 20%: " + response);
        } catch (Exception e) {
            System.err.println("Error resetting battery: " + e.getMessage());
        }
    }
} 