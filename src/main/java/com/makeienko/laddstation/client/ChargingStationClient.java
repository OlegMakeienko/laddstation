package com.makeienko.laddstation.client;

import com.makeienko.laddstation.dto.ChargingSession;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;



public class ChargingStationClient {
    public void startSession() {
        //skapar en ny laddsession
        ChargingSession session = new ChargingSession();
        session.setStationId("station_12345");
        session.setSessionId("session_123");
        session.setUserId("user_1");
        session.setCurrentPower(7.4);
        session.setTotalEnergy(22);
        session.setStatus("Active");

        //konverterar till JSON och skickar till API me restTemplate
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChargingSession> request = new HttpEntity<>(session, headers);
        String apiUrl = "http://localhost:8080/api/start-session";

        //skickar POST-förfrågan
        String response = restTemplate.postForObject(apiUrl, request, String.class);
        System.out.println("Svar från servern" + response);
    }
}
