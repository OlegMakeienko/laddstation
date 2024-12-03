package com.makeienko.laddstation.dto;

import lombok.Data;

@Data
public class InfoResponse {
    private int zone;
    private double price;
    private double totalConsumption;
}