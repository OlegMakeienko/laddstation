package com.makeienko.laddstation.UI;

import com.makeienko.laddstation.service.HomeBatteryManager;
import com.makeienko.laddstation.dto.HomeBatteryResponse;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class HomeBatteryCLI {
    private final HomeBatteryManager homeBatteryManager;
    private final Scanner scanner;

    public HomeBatteryCLI(HomeBatteryManager homeBatteryManager) {
        this.homeBatteryManager = homeBatteryManager;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        boolean running = true;
        while (running) {
            displayMenu();
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    displayBatteryStatus();
                    break;
                case "2":
                    chargeBattery();
                    break;
                case "3":
                    dischargeBattery();
                    break;
                case "4":
                    running = false;
                    break;
                default:
                    System.out.println("Ogiltigt val. Försök igen.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n=== HUSBATTERI KONTROLL ===");
        System.out.println("1. Visa batteristatus");
        System.out.println("2. Ladda batteri (om sol tillgänglig)");
        System.out.println("3. Ladda ur batteri till 10%");
        System.out.println("4. Avsluta");
        System.out.print("Välj ett alternativ (1-4): ");
    }

    private void displayBatteryStatus() {
        try {
            HomeBatteryResponse status = homeBatteryManager.getHomeBatteryStatus();
            System.out.println("\n=== HUSBATTERI STATUS ===");
            System.out.println("Batterinivå: " + status.getBatteryLevel() + "%");
            System.out.println("Aktuell energi: " + status.getCapacityKwh() + " kWh");
            System.out.println("Max kapacitet: " + status.getMaxCapacityKwh() + " kWh");
            System.out.println("Min säker nivå: " + status.getMinCapacityKwh() + " kWh");
            System.out.println("Läge: " + status.getBatteryMode());
            System.out.println("========================");
        } catch (Exception e) {
            System.err.println("Fel vid hämtning av batteristatus: " + e.getMessage());
        }
    }

    private void chargeBattery() {
        try {
            System.out.println("\nFörsöker ladda batteriet från solpaneler...");
            double newCapacity = homeBatteryManager.chargeHomeBattery();
            System.out.println("Laddning slutförd. Ny kapacitet: " + newCapacity + " kWh");
        } catch (Exception e) {
            System.err.println("Fel vid laddning av batteri: " + e.getMessage());
        }
    }

    private void dischargeBattery() {
        try {
            System.out.println("\nLaddar ur batteriet till 10%...");
            homeBatteryManager.dischargeBattery();
            System.out.println("Urladdning påbörjad.");
        } catch (Exception e) {
            System.err.println("Fel vid urladdning av batteri: " + e.getMessage());
        }
    }
} 