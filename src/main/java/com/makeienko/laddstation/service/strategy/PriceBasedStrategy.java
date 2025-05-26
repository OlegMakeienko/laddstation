package com.makeienko.laddstation.service.strategy;

import java.util.List;

import com.makeienko.laddstation.service.LaddstationApiClient;
import com.makeienko.laddstation.service.ChargingHourOptimizer;
import com.fasterxml.jackson.core.JsonProcessingException;

public class PriceBasedStrategy implements OptimalHoursStrategy {
    private final LaddstationApiClient apiClient;
    private final ChargingHourOptimizer optimizer;

    public PriceBasedStrategy(LaddstationApiClient apiClient, ChargingHourOptimizer optimizer) {
        this.apiClient = apiClient;
        this.optimizer = optimizer;
    }

    @Override
    public List<Double> findOptimalHours() throws JsonProcessingException {
        // ChargingHourOptimizer hämtar nu själv nödvändig data (baseload, prices, info)
        return optimizer.findOptimalHoursByPrice();
    }
}
