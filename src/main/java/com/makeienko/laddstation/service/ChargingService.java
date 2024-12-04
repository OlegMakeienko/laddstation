package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.InfoResponse;
import org.springframework.stereotype.Service;

@Service
public interface ChargingService {

    InfoResponse fetchAndDeserializeInfo();
    void displayInfoResponse();
    void fetchAndDisplayPriceForElZone();
    void fetchAndDisplayBaseload();
    void manageChargingSession();
    void manageChargingFrom20To80();
    void chargeWhenLowestPrice();

}
