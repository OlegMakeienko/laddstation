package com.makeienko.laddstation;

import com.makeienko.laddstation.UI.ChargingStationCLI;
import com.makeienko.laddstation.UI.HomeBatteryCLI;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ChargingAppRunner implements CommandLineRunner {
    private final ChargingStationCLI chargingStationCLI;
    private final HomeBatteryCLI homeBatteryCLI;

    public ChargingAppRunner(ChargingStationCLI chargingStationCLI, HomeBatteryCLI homeBatteryCLI) {
        this.chargingStationCLI = chargingStationCLI;
        this.homeBatteryCLI = homeBatteryCLI;
    }

    @Override
    public void run(String... args) {
        System.out.println("Välkommen till Laddstationen!");
        System.out.println("1. Hantera EV Laddning");
        System.out.println("2. Hantera Husbatteri");
        System.out.print("Välj ett alternativ (1-2): ");

        java.util.Scanner scanner = new java.util.Scanner(System.in);
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                chargingStationCLI.start();
                break;
            case "2":
                homeBatteryCLI.start();
                break;
            default:
                System.out.println("Ogiltigt val. Avslutar...");
        }
    }
} 