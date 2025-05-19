package com.makeienko.laddstation.service.strategy;


import java.util.ArrayList;
import java.util.List;

import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PriceBasedStrategy implements OptimalHoursStrategy {
    private RestTemplate restTemplate;

    public PriceBasedStrategy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<Double> findOptimalHours() throws JsonProcessingException {
        List<Double> optimalHours = new ArrayList<>();

        //Laddstationens effekt + hushållförbrukning är mindre än 11 kW
        //Hämta JSON som en lista
        String jsonResponse1 = restTemplate.getForObject("http://127.0.0.1:5001/price", String.class);
        String jsonResponse2 = restTemplate.getForObject("http://127.0.0.1:5001/baseload", String.class);

        ObjectMapper objectMapper = new ObjectMapper();

        //Deserialisera JSON till en lista av priser
        double[] hourlyPrices = objectMapper.readValue(jsonResponse1, double[].class);
        //Deserialisera JSON till en lista av förbrukningsvärden
        double[] hourlyBaseload = objectMapper.readValue(jsonResponse2, double[].class);


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
