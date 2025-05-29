package com.makeienko.laddstation.runner;

import com.makeienko.laddstation.UI.ChargingStationCLI;
import com.makeienko.laddstation.service.ChargingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ChargingAppRunner implements CommandLineRunner {

    private final ChargingService chargingService;
    private final ChargingStationCLI chargingStationCLI;


    public ChargingAppRunner(ChargingService chargingService, ChargingStationCLI chargingStationCLI) {
        this.chargingService = chargingService;
        this.chargingStationCLI = chargingStationCLI;
    }

    @Override
    public void run(String... args) throws Exception {

        //chargingService.displayInfoResponse();

        // 1.Hämta och visa elpriser för SE3
        //chargingService.fetchAndDisplayPriceForElZone();

        // 2.Hämta och visa hushållets energiförbrukning
        //chargingService.fetchAndDisplayBaseload();

        //3. Skicka kommando för att starta och stoppa laddningen av EVs batteri.
        //under laddning skall batteriets laddning avläsas och omvandlas till antal procent.
        //chargingService.manageChargingSession3();

        //4. Batteriet skall laddas från 20% till 80%
        //Batteriet skall laddas när hushållets förbrukning är som lägst och total energiförbrukning
        //skall understiga 11kW (3 fas , 16 A)
        //chargingService.manageChargingFrom20To80();

        //5. Batteriet skall laddas från 20% till 80%
        // Batteriet skall laddas när elpriset är som lägst och total energiförbrukning för inte
        //överstiga 11 kW (3 fas, 16A)
        //chargingService.chargeWhenLowestPrice();

        //6. Klienten skall visa tidpunkter på dygnet och den totala energiåtgången samt visa på vilket sätt
        // laddningen är optimerad.
        //chargingService.displayEnergyConsumptionAndOptimization();

        //7. Skapa ett GUI eller använd ett terminalfönster(kommandoprompt) för att kommunicera med den simulerade laddstationen.

        // Console interface disabled - using React frontend instead
        chargingStationCLI.start();

        System.out.println("Laddstation REST API is running on http://localhost:8080");
        System.out.println("React frontend available on http://localhost:3000");
        System.out.println("Console interface has been disabled in favor of web interface.");

        //chargingService.chargeBatteryDirect();
    }
}
