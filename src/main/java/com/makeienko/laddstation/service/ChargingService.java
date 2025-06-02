package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.service.strategy.OptimalHoursStrategy;

import org.springframework.stereotype.Service;

@Service
public interface ChargingService {

    InfoResponse fetchAndDeserializeInfo();
    void displayInfoResponse();
    void displayHomeBatteryStatus();
    void fetchAndDisplayPriceForElZone();
    void fetchAndDisplayBaseload();
    void fetchAndDisplaySolarProductionPerHour();
    void chargeBatteryDirect();
    void chargingSessionOnOptimalChargingHoursPrice();
    void chargingSessionOnOptimalChargingHours();
    void performChargingSessionWithStrategy(OptimalHoursStrategy strategy);
    void dischargeBatteryTo20();
    void dischargeHomeBatteryTo10();
}
