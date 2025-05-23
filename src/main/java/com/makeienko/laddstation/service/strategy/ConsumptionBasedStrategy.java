package com.makeienko.laddstation.service.strategy;

import java.util.ArrayList;
import java.util.List;

import com.makeienko.laddstation.service.LaddstationApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;

public class ConsumptionBasedStrategy implements OptimalHoursStrategy {
    private LaddstationApiClient apiClient;

    public ConsumptionBasedStrategy(LaddstationApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public List<Double> findOptimalHours() throws JsonProcessingException {
        List<Double> optimalHours = new ArrayList<>();
        
        //Laddstationens effekt + hushållförbrukning är mindre än 11 kW
        //Hämta data från API-klient
        double[] hourlyBaseload = apiClient.getBaseload();

        double optimalHour;

        //Integtrera genom varje timme för att hitta den bästa timmen för ladningen
        for(int hour = 0; hour < hourlyBaseload.length; hour++) {
            //Kontrolera om totala förbrukningen (inklusive laddstation) är under 11 kW
            double totalLoad = hourlyBaseload[hour] + 7.4; //laddstationens effekt + hushållsförbrukning
            if(totalLoad <= 11) {
                // Logga timmen om dedn är optimal för ladning
                System.out.println("Hour " + hour + " is optimal for charging with total load: " 
                                        + String.format("%.2f", totalLoad) + " kW");
                
                optimalHour = hour;
                optimalHours.add(optimalHour);
            }
        }

        return optimalHours;
    }
}
