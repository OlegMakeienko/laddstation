package com.makeienko.laddstation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.makeienko.laddstation.dto.InfoResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class InfoService {

    // HTTP-anrop till /info och hämtar data
    public InfoResponse getInfoFromApi() {
        RestTemplate restTemplate = new RestTemplate();
        String infoUrl = "http://127.0.0.1:5000/info";

        // Förfrågan till /info
        HttpHeaders infoHeaders = new HttpHeaders();
        infoHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> infoRequest = new HttpEntity<>("{}", infoHeaders);

        ResponseEntity<String> infoResponse = restTemplate.exchange(infoUrl, HttpMethod.POST, infoRequest, String.class);

        if (infoResponse.getStatusCode() == HttpStatus.OK) {
            // Deserialisera JSON och returnera som InfoResponse
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(infoResponse.getBody(), InfoResponse.class);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;  // Returnera null om något går fel
    }
}