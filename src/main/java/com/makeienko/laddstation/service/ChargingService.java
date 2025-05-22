package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.service.strategy.OptimalHoursStrategy;

import org.springframework.stereotype.Service;

@Service
public interface ChargingService {

    InfoResponse fetchAndDeserializeInfo();
    void displayInfoResponse();
    void fetchAndDisplayPriceForElZone();
    void fetchAndDisplayBaseload();
    void chargeBatteryDirect();
    void chargingSessionOnOptimalChargingHoursPrice();
    void chargingSessionOnOptimalChargingHours();
    void performChargingSessionWithStrategy(OptimalHoursStrategy strategy);
}
