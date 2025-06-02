package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.ChargingSession;
import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.exception.ChargingServiceException;
import org.springframework.stereotype.Component;

@Component
public class BatteryManager {
    
    private final LaddstationApiClient apiClient;
    private static final long POLLING_INTERVAL_MS = 1000; // Kolla servern varje sekund
    private static final double TARGET_BATTERY_PERCENTAGE = 80.0;

    public BatteryManager(LaddstationApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    /**
     * Startar laddning på servern och övervakar tills batteriet når 80%,
     * eller tills ett externt avbrott sker (t.ex. om timmen inte längre är optimal).
     * Denna metod ANTAGER att det är OK att ladda just nu.
     * Den returnerar true om målet nåddes, false om den avbröts av en anledning
     * som gör att den yttre logiken (ChargingServiceImpl) bör stoppa laddningen.
     */
    public boolean chargeBatteryUntilTarget() {
        System.out.println("BatteryManager: Initiating charge cycle to " + TARGET_BATTERY_PERCENTAGE + "%");
        apiClient.startCharging(); 

        try {
            while (true) {
                InfoResponse currentInfo = apiClient.getInfo();
                if (currentInfo == null) {
                    System.err.println("BatteryManager: Failed to get info from server during charge. Stopping.");
                    apiClient.stopCharging();
                    return false; // Indikerar problem
                }

                // Use ev_batt_max_capacity_kwh from InfoResponse
                double maxCapacityKwh = currentInfo.getEvBattMaxCapacityKwh(); 
                if (maxCapacityKwh <= 0) { // Basic sanity check
                    System.err.println("BatteryManager: Invalid maxCapacityKwh from server: " + maxCapacityKwh + ". Using default 46.3");
                    maxCapacityKwh = 46.3; // Fallback, though this indicates a server data issue
                }
                double currentPercentage = (currentInfo.getBatteryEnergyKwh() / maxCapacityKwh) * 100;
                // Avrundning för att matcha serverns precision om nödvändigt
                currentPercentage = Math.round(currentPercentage * 10.0) / 10.0;
                
                System.out.println("BatteryManager: Current server battery level: " + currentPercentage + "%");

                if (currentPercentage >= TARGET_BATTERY_PERCENTAGE) {
                    System.out.println("BatteryManager: Target " + TARGET_BATTERY_PERCENTAGE + "% reached.");
                    // Stoppa INTE laddningen här. Den som kallade på oss (t.ex. performChargingSessionWithStrategy)
                    // kanske vill fortsätta ladda om timmen fortfarande är optimal och en annan policy gäller.
                    // Den yttre logiken ansvarar för att anropa apiClient.stopCharging() när den är helt klar.
                    return true; // Målet nått
                }
                
                // Kontrollera om servern av någon anledning slutat ladda (t.ex. manuellt via /charge off)
                // Detta är en extra säkerhetskoll, även om ChargingServiceImpl bör hantera start/stopp primärt.
                if (!currentInfo.isEvBatteryChargeStartStopp()) {
                    System.out.println("BatteryManager: Server reports charging has stopped. Aborting charge cycle.");
                    return false; // Laddningen avbröts på servern
                }

                Thread.sleep(POLLING_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            System.err.println("BatteryManager: Charging interrupted. Stopping charge on server.");
            apiClient.stopCharging();
            Thread.currentThread().interrupt();
            return false; // Indikerar avbrott
        } catch (Exception e) {
            System.err.println("BatteryManager: Error during charge cycle. Stopping charge on server: " + e.getMessage());
            apiClient.stopCharging();
            e.printStackTrace();
            return false; // Indikerar problem
        }
    }

    /**
     * Väntar en specificerad mängd simulerad tid. Under denna tid antas servern ladda batteriet
     * (om laddningen är påslagen av den anropande koden, t.ex. ChargingServiceImpl).
     * Denna metod startar eller stoppar inte laddningen på servern själv.
     * @param simulatedMinutesToWait Antal simulerade minuter att vänta.
     */
    public void simulateChargingPeriod(int simulatedMinutesToWait) {
        // Servern har seconds_per_hour = 4. Det betyder 15 simulerade minuter = 1 verklig sekund.
        long realMillisecondsToWait = (long) (simulatedMinutesToWait / 15.0 * 1000.0);
        if (realMillisecondsToWait <= 0) realMillisecondsToWait = 100; // Vänta åtminstone lite för att undvika tight loop

        System.out.println("BatteryManager: Simulating charging for " + simulatedMinutesToWait 
            + " simulated minutes (waiting " + realMillisecondsToWait + " ms real time).");
        try {
            Thread.sleep(realMillisecondsToWait);
            InfoResponse currentInfo = apiClient.getInfo();
            if (currentInfo != null) {
                // Use ev_batt_max_capacity_kwh from InfoResponse
                double maxCapacityKwh = currentInfo.getEvBattMaxCapacityKwh();
                if (maxCapacityKwh <= 0) { 
                    System.err.println("BatteryManager: Invalid maxCapacityKwh in simulateChargingPeriod: " + maxCapacityKwh + ". Using default 46.3");
                    maxCapacityKwh = 46.3;
                }
                 double currentPercentage = (currentInfo.getBatteryEnergyKwh() / maxCapacityKwh) * 100;
                 currentPercentage = Math.round(currentPercentage * 10.0) / 10.0;
                System.out.println("BatteryManager: Battery level after simulated period: " + currentPercentage + "%");
            }
        } catch (InterruptedException e) {
            System.err.println("BatteryManager: Wait period interrupted.");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Uppdaterar status för en laddningssession baserat på data från servern.
     * Denna metod är mest användbar om man hanterar ett lokalt ChargingSession-objekt,
     * vilket vi nu gör mindre av för själva laddningslogiken.
     */
    public void updateBatteryStatus(ChargingSession chargingSession, InfoResponse infoResponse) {
        // Use ev_batt_max_capacity_kwh from InfoResponse
        double maxCapacityKwh = infoResponse.getEvBattMaxCapacityKwh();
        if (maxCapacityKwh <= 0) { 
            System.err.println("BatteryManager: Invalid maxCapacityKwh in updateBatteryStatus: " + maxCapacityKwh + ". Using default 46.3");
            maxCapacityKwh = 46.3;
        }
        double currentBatteryEnergy = infoResponse.getBatteryEnergyKwh();

        chargingSession.setCurrentLoad(currentBatteryEnergy); // This setter might need a rename if it's meant for energy
        chargingSession.setBatteryCapacity(maxCapacityKwh); // Represents max capacity

        double batteryPercentage = (currentBatteryEnergy / maxCapacityKwh) * 100;
        batteryPercentage = Math.min(batteryPercentage, 100);
        batteryPercentage = Math.round(batteryPercentage * 10.0) / 10.0;
        chargingSession.setBatteryPercentage(batteryPercentage);

        System.out.println("BatteryManager (updateStatus): Capacity: " + maxCapacityKwh + " kWh, Current Load: " 
            + currentBatteryEnergy + " kWh, Level: " + batteryPercentage + "%");
    }

    /**
     * Kontrollerar om batteriet är tillräckligt laddat (>= TARGET_BATTERY_PERCENTAGE%) baserat på serverns info.
     */
    public boolean isBatterySufficient() {
        try {
            InfoResponse infoResponse = apiClient.getInfo();
            if (infoResponse != null) {
                // Use ev_batt_max_capacity_kwh from InfoResponse
                double maxCapacityKwh = infoResponse.getEvBattMaxCapacityKwh();
                if (maxCapacityKwh <= 0) { 
                    System.err.println("BatteryManager: Invalid maxCapacityKwh in isBatterySufficient: " + maxCapacityKwh + ". Using default 46.3");
                    maxCapacityKwh = 46.3;
                }
                double batteryPercentage = (infoResponse.getBatteryEnergyKwh() / maxCapacityKwh) * 100;
                return batteryPercentage >= TARGET_BATTERY_PERCENTAGE;
            }
        } catch (Exception e) {
            System.err.println("BatteryManager: Error checking if battery is sufficient: " + e.getMessage());
            // e.printStackTrace(); // Kan vara för mycket loggar i normal drift
        }
        return false; // Anta att laddning behövs om vi inte kan hämta status eller vid fel
    }

    /**
     * Anropar API för att starta laddning.
     */
    public void startChargingApi() {
        try {
            String response = apiClient.startCharging();
            System.out.println("BatteryManager: Called API to start charging. Response: " + response);
        } catch (Exception e) {
            // Kasta vidare eller logga felet mer utförligt
            throw new ChargingServiceException("BatteryManager: Failed to call API to start charging: " + e.getMessage(), e);
        }
    }

    /**
     * Anropar API för att stoppa laddning.
     */
    public void stopChargingApi() {
        try {
            String response = apiClient.stopCharging();
            System.out.println("BatteryManager: Called API to stop charging. Response: " + response);
        } catch (Exception e) {
            throw new ChargingServiceException("BatteryManager: Failed to call API to stop charging: " + e.getMessage(), e);
        }
    }
    
    /**
     * Urladdning av EV batteriet till 20% via API
     */
    public void dischargeEVBatteryTo20Api() {
        try {
            System.out.println("BatteryManager: Sending command to discharge EV battery to 20%...");
            String response = apiClient.dischargeEVBattery();
            System.out.println("BatteryManager: Server response: " + response);
        } catch (Exception e) {
            System.err.println("BatteryManager: Failed to discharge EV battery: " + e.getMessage());
        }
    }
} 