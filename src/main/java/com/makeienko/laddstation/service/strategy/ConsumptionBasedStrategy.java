package com.makeienko.laddstation.service.strategy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConsumptionBasedStrategy implements OptimalHoursStrategy {
    private RestTemplate restTemplate;

    public ConsumptionBasedStrategy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<Double> findOptimalHours() throws JsonProcessingException {
        List<Double> optimalHours = new ArrayList<>();
        
        //Laddstationens effekt + hushållförbrukning är mindre än 11 kW
        //Hämta JSON som en lista
        String jsonResponse = restTemplate.getForObject("http://127.0.0.1:5001/baseload", String.class);

        ObjectMapper objectMapper = new ObjectMapper();

        double[] hourlyBaseload = objectMapper.readValue(jsonResponse, double[].class);

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
