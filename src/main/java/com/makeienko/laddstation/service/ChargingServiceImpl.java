package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.ChargingSession;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class ChargingServiceImpl implements ChargingService {

    private final RestTemplate restTemplate;

    public ChargingServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ChargingSession fetchData() {
        // API-anrop för att hämta data
        List<Double> hourlyPrices = Arrays.asList(
                restTemplate.getForObject("http://127.0.0.1:5000/priceperhour", Double[].class)
        );

        List<Double> hourlyBaseload = Arrays.asList(
                restTemplate.getForObject("http://127.0.0.1:5000/baseload", Double[].class)
        );

        double batteryPercentage = restTemplate.getForObject("http://127.0.0.1:5000/info", Double.class);
        double chargingPower = 7.4; // Fast värde för laddstationens effekt

        return new ChargingSession(hourlyPrices, hourlyBaseload, batteryPercentage, chargingPower);
    }

    @Override
    public void optimizeCharging(ChargingSession session) {

    }

    @Override
    public void startCharging() {

    }

    @Override
    public void stopCharging() {

    }
}
