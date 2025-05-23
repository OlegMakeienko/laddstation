package com.makeienko.laddstation.service.strategy;

import java.util.ArrayList;
import java.util.List;

import com.makeienko.laddstation.service.LaddstationApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;

public class PriceBasedStrategy implements OptimalHoursStrategy {
    private LaddstationApiClient apiClient;

    public PriceBasedStrategy(LaddstationApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<Double> findOptimalHours() throws JsonProcessingException {
        List<Double> optimalHours = new ArrayList<>();

        //Laddstationens effekt + hushållförbrukning är mindre än 11 kW
        //Hämta data från API-klient
        double[] hourlyPrices = apiClient.getHourlyPrices();
        double[] hourlyBaseload = apiClient.getBaseload();

        double optimalHour = 0;
        //Hitta den timme som har det lägsta priset
        double loowestCost = Double.MAX_VALUE;

        //iterera gemon varje timme för att hitta den bästa timmen för laddningen
        for(int hour = 0; hour < hourlyPrices.length; hour++) {
            //kontrollera om totalal förbrukningen (inklusive laddstation) är under 11 kW
            double totalLoad = hourlyBaseload[hour] + 7.4; //laddstationens effekt + hushållsförbrukning
            if(totalLoad <= 11) {
                //Logga timmen om den är optimal för ladning
                System.out.println("Hour " + hour + " is optimal for charging with total load: " 
                                    + String.format("%.2f", totalLoad) + " kW");
                
                //logga priset för den optimala timmen
                System.out.println("Price for hour " + hour + ": " + String.format("%.2f", hourlyPrices[hour]) + " kr per kWh");
                

                optimalHour = hour;

                //Beräkna kostnaden för att ladda vid denna timme
                double cost = hourlyPrices[hour] * totalLoad;
                if(cost < loowestCost) {
                    loowestCost = cost;
                    optimalHours.add(optimalHour);
                }
            }   
        }

        return optimalHours;  
    }
}
