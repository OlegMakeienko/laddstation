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

        // Hämta och visa elpriser för SE3
        chargingService.fetchAndDisplayPriceForElZone();
    }
}