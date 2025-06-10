package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.ChargingSession;
import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.exception.ChargingServiceException;
import org.springframework.stereotype.Component;

@Component
public class EVBatteryManager {
    
    private final LaddstationApiClient apiClient;
    private static final long POLLING_INTERVAL_MS = 1000; // Kolla servern varje sekund
    private static final double TARGET_EVBATTERY_PERCENTAGE = 80.0;

    public EVBatteryManager(LaddstationApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    /**
     * Startar laddning på servern och övervakar tills batteriet når 80%,
     * eller tills ett externt avbrott sker (t.ex. om timmen inte längre är optimal).
     * Denna metod ANTAGER att det är OK att ladda just nu.
     * Den returnerar true om målet nåddes, false om den avbröts av en anledning
     * som gör att den yttre logiken (ChargingServiceImpl) bör stoppa laddningen.
     */
    public boolean chargeEVBatteryUntilTarget() {
        System.out.println("Initiating charge cycle to " + TARGET_EVBATTERY_PERCENTAGE + "%");
        apiClient.startCharging(); 

        try {
            while (true) {
                InfoResponse currentInfo = apiClient.getInfo();
                if (currentInfo == null) {
                    System.err.println("Failed to get info from server during charge. Stopping.");
                    apiClient.stopCharging();
                    return false; // Indikerar problem
                }

                // Use ev_batt_max_capacity_kwh from InfoResponse
                double maxCapacityKwh = currentInfo.getEvBattMaxCapacityKwh(); 
                if (maxCapacityKwh <= 0) { // Basic sanity check
                    System.err.println("Invalid maxCapacityKwh from server: " + maxCapacityKwh + ". Using default 46.3");
                    maxCapacityKwh = 46.3; // Fallback, though this indicates a server data issue
                }
                double currentPercentage = (currentInfo.getEvBatteryEnergyKwh() / maxCapacityKwh) * 100;
                // Avrundning för att matcha serverns precision om nödvändigt
                currentPercentage = Math.round(currentPercentage * 10.0) / 10.0;
                
                System.out.println("Current server battery level: " + currentPercentage + "%");

                if (currentPercentage >= TARGET_EVBATTERY_PERCENTAGE) {
                    System.out.println("Target " + TARGET_EVBATTERY_PERCENTAGE + "% reached.");
                    // Stoppa INTE laddningen här. Den som kallade på oss (t.ex. performChargingSessionWithStrategy)
                    // kanske vill fortsätta ladda om timmen fortfarande är optimal och en annan policy gäller.
                    // Den yttre logiken ansvarar för att anropa apiClient.stopCharging() när den är helt klar.
                    return true; // Målet nått
                }
                
                // Kontrollera om servern av någon anledning slutat ladda (t.ex. manuellt via /charge off)
                // Detta är en extra säkerhetskoll, även om ChargingServiceImpl bör hantera start/stopp primärt.
                if (!currentInfo.isEvBatteryChargeStartStopp()) {
                    System.out.println("Server reports charging has stopped. Aborting charge cycle.");
                    return false; // Laddningen avbröts på servern
                }

                Thread.sleep(POLLING_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            System.err.println("Charging interrupted. Stopping charge on server.");
            apiClient.stopCharging();
            Thread.currentThread().interrupt();
            return false; // Indikerar avbrott
        } catch (Exception e) {
            System.err.println("Error during charge cycle. Stopping charge on server: " + e.getMessage());
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

        System.out.println("Simulating charging for " + simulatedMinutesToWait 
            + " simulated minutes (waiting " + realMillisecondsToWait + " ms real time).");
        try {
            Thread.sleep(realMillisecondsToWait);
            InfoResponse currentInfo = apiClient.getInfo();
            if (currentInfo != null) {
                // Use ev_batt_max_capacity_kwh from InfoResponse
                double maxCapacityKwh = currentInfo.getEvBattMaxCapacityKwh();
                if (maxCapacityKwh <= 0) { 
                    System.err.println("Invalid maxCapacityKwh in simulateChargingPeriod: " + maxCapacityKwh + ". Using default 46.3");
                    maxCapacityKwh = 46.3;
                }
                 double currentPercentage = (currentInfo.getEvBatteryEnergyKwh() / maxCapacityKwh) * 100;
                 currentPercentage = Math.round(currentPercentage * 10.0) / 10.0;
                System.out.println("Battery level after simulated period: " + currentPercentage + "%");
            }
        } catch (InterruptedException e) {
            System.err.println("Wait period interrupted.");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Uppdaterar status för en laddningssession baserat på data från servern.
     * Denna metod är mest användbar om man hanterar ett lokalt ChargingSession-objekt,
     * vilket vi nu gör mindre av för själva laddningslogiken.
     */
    public void updateEVBatteryStatus(ChargingSession chargingSession, InfoResponse infoResponse) {
        // Use ev_batt_max_capacity_kwh from InfoResponse
        double maxCapacityKwh = infoResponse.getEvBattMaxCapacityKwh();
        if (maxCapacityKwh <= 0) { 
            System.err.println("Invalid maxCapacityKwh in updateEVBatteryStatus: " + maxCapacityKwh + ". Using default 46.3");
            maxCapacityKwh = 46.3;
        }
        double currentEVBatteryEnergy = infoResponse.getEvBatteryEnergyKwh();

        chargingSession.setCurrentLoad(currentEVBatteryEnergy); // This setter might need a rename if it's meant for energy
        chargingSession.setBatteryCapacity(maxCapacityKwh); // Represents max capacity

        double evBatteryPercentage = (currentEVBatteryEnergy / maxCapacityKwh) * 100;
        evBatteryPercentage = Math.min(evBatteryPercentage, 100);
        evBatteryPercentage = Math.round(evBatteryPercentage * 10.0) / 10.0;
        chargingSession.setBatteryPercentage(evBatteryPercentage);

        System.out.println("EVBatteryManager (updateStatus): Capacity: " + maxCapacityKwh + " kWh, Current Load: " 
            + currentEVBatteryEnergy + " kWh, Level: " + evBatteryPercentage + "%");
    }

    /**
     * Kontrollerar om batteriet är tillräckligt laddat (>= TARGET_EVBATTERY_PERCENTAGE%) baserat på serverns info.
     */
    public boolean isEVBatterySufficient() {
        try {
            InfoResponse infoResponse = apiClient.getInfo();
            if (infoResponse != null) {
                // Use ev_batt_max_capacity_kwh from InfoResponse
                double maxCapacityKwh = infoResponse.getEvBattMaxCapacityKwh();
                if (maxCapacityKwh <= 0) { 
                    System.err.println("Invalid maxCapacityKwh in isEVBatterySufficient: " + maxCapacityKwh + ". Using default 46.3");
                    maxCapacityKwh = 46.3;
                }
                double evBatteryPercentage = (infoResponse.getEvBatteryEnergyKwh() / maxCapacityKwh) * 100;
                return evBatteryPercentage >= TARGET_EVBATTERY_PERCENTAGE;
            }
        } catch (Exception e) {
            System.err.println("Error checking if battery is sufficient: " + e.getMessage());
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
            System.out.println("Called API to start charging. Response: " + response);
        } catch (Exception e) {
            // Kasta vidare eller logga felet mer utförligt
            throw new ChargingServiceException("Failed to call API to start charging: " + e.getMessage(), e);
        }
    }

    /**
     * Anropar API för att stoppa laddning.
     */
    public void stopChargingApi() {
        try {
            String response = apiClient.stopCharging();
            System.out.println("Called API to stop charging. Response: " + response);
        } catch (Exception e) {
            throw new ChargingServiceException("Failed to call API to stop charging: " + e.getMessage(), e);
        }
    }
    
    /**
     * Anropar API för att urladda batteriet till 20%.
     */
    public void dischargeEVBatteryTo20Api() {
        try {
            String response = apiClient.dischargeBattery();
            System.out.println("Called API to discharge battery to 20%. Response: " + response);
        } catch (Exception e) {
            System.err.println("Error calling API to discharge battery: " + e.getMessage());
        }
    }
} 