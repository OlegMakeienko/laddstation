package com.makeienko.laddstation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class PricePerHourResponse {
    @JsonProperty("area_prices")
    private Map<String, double[]> areaPrices; // Nycklar är SE1, SE2, SE3, SE4
}
