package com.makeienko.laddstation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptimalChargingResponse {
    private List<Double> optimalHours;
    private String strategy;
    private String timeRange;
} 