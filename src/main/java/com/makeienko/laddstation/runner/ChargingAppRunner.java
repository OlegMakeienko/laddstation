package com.makeienko.laddstation.runner;

import com.makeienko.laddstation.dto.ChargingSession;
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
        System.out.println("Starting Charging App...");

        ChargingSession session = chargingService.fetchData();
        chargingService.optimizeCharging(session);
        chargingService.startCharging();

        System.out.println("Charging process completed!");
    }
}
