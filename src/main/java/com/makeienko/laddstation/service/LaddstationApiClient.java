package com.makeienko.laddstation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.exception.ChargingServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class LaddstationApiClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String BASE_URL = "http://127.0.0.1:5001";
    
    public LaddstationApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Hämtar information om batteristatus och simulerad tid
     */
    public InfoResponse getInfo() {
        try {
            String jsonResponse = restTemplate.getForObject(BASE_URL + "/info", String.class);
            if (jsonResponse == null) {
                throw new ChargingServiceException("Received null response from info endpoint");
            }

            InfoResponse infoResponse = objectMapper.readValue(jsonResponse, InfoResponse.class);
            
            if (infoResponse == null) {
                throw new ChargingServiceException("Failed to deserialize response to InfoResponse object");
            }
            
            return infoResponse;
        } catch (RestClientException e) {
            throw new ChargingServiceException("Failed to fetch data from info endpoint: " + e.getMessage(), e);
        } catch (JsonProcessingException e) {
            throw new ChargingServiceException("Failed to process JSON response: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ChargingServiceException("Unexpected error while fetching info: " + e.getMessage(), e);
        }
    }
    
    /**
     * Startar laddning av batteriet
     */
    public String startCharging() {
        try {
            String response = restTemplate.postForObject(
                    BASE_URL + "/charge",
                    Map.of("charging", "on"),
                    String.class
            );
            return response;
        } catch (Exception e) {
            throw new ChargingServiceException("Failed to start charging: " + e.getMessage(), e);
        }
    }
    
    /**
     * Stoppar laddning av batteriet
     */
    public String stopCharging() {
        try {
            String response = restTemplate.postForObject(
                    BASE_URL + "/charge",
                    Map.of("charging", "off"),
                    String.class
            );
            return response;
        } catch (Exception e) {
            throw new ChargingServiceException("Failed to stop charging: " + e.getMessage(), e);
        }
    }
    
    /**
     * Hämtar elpriser per timme
     */
    public double[] getHourlyPrices() {
        try {
            String jsonResponse = restTemplate.getForObject(BASE_URL + "/priceperhour", String.class);
            if (jsonResponse == null) {
                throw new ChargingServiceException("Received null response from priceperhour endpoint");
            }
            
            return objectMapper.readValue(jsonResponse, double[].class);
        } catch (JsonProcessingException e) {
            throw new ChargingServiceException("Failed to process JSON response from priceperhour: " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new ChargingServiceException("Failed to fetch hourly prices: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ChargingServiceException("Unexpected error while fetching hourly prices: " + e.getMessage(), e);
        }
    }
    
    /**
     * Hämtar hushållets basförbrukning per timme
     */
    public double[] getBaseload() {
        try {
            String jsonResponse = restTemplate.getForObject(BASE_URL + "/baseload", String.class);
            if (jsonResponse == null) {
                throw new ChargingServiceException("Received null response from baseload endpoint");
            }
            
            return objectMapper.readValue(jsonResponse, double[].class);
        } catch (JsonProcessingException e) {
            throw new ChargingServiceException("Failed to process JSON response from baseload: " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new ChargingServiceException("Failed to fetch baseload data: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ChargingServiceException("Unexpected error while fetching baseload: " + e.getMessage(), e);
        }
    }
    
    /**
     * Hämtar solpanelproduktion per timme
     */
    public double[] getSolarProductionPerHour() {
        try {
            String jsonResponse = restTemplate.getForObject(BASE_URL + "/solarproduction", String.class);
            if (jsonResponse == null) {
                throw new ChargingServiceException("Received null response from solarproduction endpoint");
            }
            
            return objectMapper.readValue(jsonResponse, double[].class);
        } catch (JsonProcessingException e) {
            throw new ChargingServiceException("Failed to process JSON response from solarproduction: " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new ChargingServiceException("Failed to fetch solar production data: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ChargingServiceException("Unexpected error while fetching solar production: " + e.getMessage(), e);
        }
    }
    
    /**
     * Urladdning av batteriet till 20%
     */
    public String dischargeBattery() {
        try {
            String response = restTemplate.postForObject(
                    BASE_URL + "/discharge",
                    Map.of("discharging", "on"),
                    String.class
            );
            return response;
        } catch (Exception e) {
            throw new ChargingServiceException("Failed to discharge battery: " + e.getMessage(), e);
        }
    }

    /**
     * Ladda ur husbatteriet till 10%
     */
    public String dischargeHomeBatteryTo10() {
        try {
            String response = restTemplate.postForObject(
                    BASE_URL + "/discharge-home-battery",
                    Map.of("discharging", "on"),
                    String.class
            );
            return response;
        } catch (Exception e) {
            throw new ChargingServiceException("Failed to discharge home battery: " + e.getMessage(), e);
        }
    }
} 