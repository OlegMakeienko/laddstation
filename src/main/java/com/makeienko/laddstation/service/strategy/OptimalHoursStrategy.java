package com.makeienko.laddstation.service.strategy;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface OptimalHoursStrategy {
    List<Double> findOptimalHours() throws JsonProcessingException;
}
