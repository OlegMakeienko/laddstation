package com.makeienko.laddstation.service.strategy;

import java.util.List;

import com.makeienko.laddstation.service.LaddstationApiClient;
import com.makeienko.laddstation.service.ChargingHourOptimizer;
import com.fasterxml.jackson.core.JsonProcessingException;

public class ConsumptionBasedStrategy implements OptimalHoursStrategy {
    private final LaddstationApiClient apiClient; // Behålls om strategin behöver annan API-data i framtiden
    private final ChargingHourOptimizer optimizer;

    public ConsumptionBasedStrategy(LaddstationApiClient apiClient, ChargingHourOptimizer optimizer) {
        this.apiClient = apiClient;
        this.optimizer = optimizer;
    }

    @Override
    public List<Double> findOptimalHours() throws JsonProcessingException {
        // ChargingHourOptimizer hämtar nu själv nödvändig data (baseload, info)
        return optimizer.findOptimalHoursByConsumption();
    }
}
