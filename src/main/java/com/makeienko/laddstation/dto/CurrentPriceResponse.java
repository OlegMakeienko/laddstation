package com.makeienko.laddstation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrentPriceResponse {
    private double currentPrice;
    private int currentHour;
    private double[] hourlyPrices;
} 