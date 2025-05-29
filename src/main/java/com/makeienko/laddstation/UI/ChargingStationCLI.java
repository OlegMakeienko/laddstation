package com.makeienko.laddstation.UI;

import com.makeienko.laddstation.service.ChargingService;
import com.makeienko.laddstation.service.ChargingServiceImpl;
import com.makeienko.laddstation.service.LaddstationApiClient;
import com.makeienko.laddstation.service.BatteryManager;
import com.makeienko.laddstation.service.ChargingHourOptimizer;
import com.makeienko.laddstation.service.HomeBatteryManager;
import com.makeienko.laddstation.service.SolarPanelManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Scanner;

@Component
public class ChargingStationCLI {

    private final ChargingService chargingService;

    public ChargingStationCLI(ChargingService chargingService) {
        this.chargingService = chargingService;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("==== Laddstation - Huvudmeny ====");
            System.out.println("1. Visa batteri- och laddningsstatus");
            System.out.println("2. Visa hushållets energiförbrukning");
            System.out.println("3. Visa elpriser per timme");
            System.out.println("4. Starta laddning direkt");
            System.out.println("5. Starta laddning när när price år lägst och total energiförbrukning" +
                    "skall understiga 11kW");
            System.out.println("6. Starta laddning när när hushållets förbrukning är som lägst och total energiförbrukning" +
                    "skall understiga 11kW");
            System.out.println("7. Avsluta");
            System.out.println("8. Ladda ur batteriet till 20%");
            System.out.print("Välj ett alternativ: ");

            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    chargingService.displayInfoResponse();
                    break;
                case 2:
                    chargingService.fetchAndDisplayBaseload();
                    break;
                case 3:
                    chargingService.fetchAndDisplayPriceForElZone();
                    break;
                case 4:
                    chargingService.chargeBatteryDirect();
                    break;
                case 5:
                    chargingService.chargingSessionOnOptimalChargingHoursPrice();
                    break;
                case 6:
                    chargingService.chargingSessionOnOptimalChargingHours();
                    break;
                case 7:
                    System.out.println("Avslutar programmet...");
                    System.exit(0);
                    break;
                case 8:
                    chargingService.dischargeBatteryTo20();
                    break;
                default:
                    System.out.println("Ogiltigt val, försök igen.");
            }
        }

        scanner.close();
    }

    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();
        LaddstationApiClient apiClient = new LaddstationApiClient(restTemplate);
        BatteryManager batteryManager = new BatteryManager(apiClient);
        ChargingHourOptimizer chargingHourOptimizer = new ChargingHourOptimizer(apiClient);
        HomeBatteryManager homeBatteryManager = new HomeBatteryManager(apiClient);
        SolarPanelManager solarPanelManager = new SolarPanelManager(apiClient);
        ChargingService chargingService = new ChargingServiceImpl(apiClient, batteryManager, chargingHourOptimizer, homeBatteryManager, solarPanelManager);
        ChargingStationCLI cli = new ChargingStationCLI(chargingService);
        cli.start();
    }
}

