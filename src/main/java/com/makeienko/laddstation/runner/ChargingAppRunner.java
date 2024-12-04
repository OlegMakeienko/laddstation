package com.makeienko.laddstation.runner;

import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.service.ChargingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ChargingAppRunner implements CommandLineRunner {

    private final ChargingService chargingService;


    public ChargingAppRunner(ChargingService chargingService) {
        this.chargingService = chargingService;
    }

    @Override
    public void run(String... args) throws Exception {

        chargingService.displayInfoResponse();

        // 1.Hämta och visa elpriser för SE3
        //chargingService.fetchAndDisplayPriceForElZone();

        // 2.Hämta och visa hushållets energiförbrukning
        //chargingService.fetchAndDisplayBaseload();

        //3. Skicka kommando för att starta och stoppa laddningen av EVs batteri.
        //under laddning skall batteriets laddning avläsas och omvandlas till antal procent.
        //chargingService.manageChargingSession();

        //4. Batteriet skall laddas från 20% till 80%
        //Batteriet skall laddas när hushållets förbrukning är som lägst och total energiförbrukning
        //skall understiga 11kW (3 fas , 16 A)
        //chargingService.manageChargingFrom20To80();

    }
}
