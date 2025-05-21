package com.makeienko.laddstation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.makeienko.laddstation.dto.InfoResponse;

public class ChargingServiceImplTest {
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ChargingServiceImpl chargingService;
    private InfoResponse expectedResponse;
    private ObjectMapper objectMapper = new ObjectMapper();
    private String expectedJsonResponse;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        MockitoAnnotations.openMocks(this);

        //skapa en förväntat InfoResponse objekt
        expectedResponse = new InfoResponse();
        expectedResponse.setBaseCurrentLoad(2.0);
        expectedResponse.setBatteryCapacityKWh(20.0);
        expectedResponse.setSimTimeHour(14);
        expectedResponse.setSimTimeMin(30);
        expectedResponse.setEvBatteryChargeStartStopp(true);

        //konvertera till JSON-sträng för återanvändnig
        expectedJsonResponse = objectMapper.writeValueAsString(expectedResponse);
    }

    @Test
    void testFetchAndDeserializeInfo() {

        //konfigera mock
        when(restTemplate.getForObject("http://127.0.0.1:5001/info", String.class)).thenReturn(expectedJsonResponse);

        //exekvera metoden
        InfoResponse actualResponse = chargingService.fetchAndDeserializeInfo();

        //verifiera resultaten
        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getBaseCurrentLoad(), actualResponse.getBaseCurrentLoad());
        assertEquals(expectedResponse.getBatteryCapacityKWh(), actualResponse.getBatteryCapacityKWh());
        assertEquals(expectedResponse.getSimTimeHour(), actualResponse.getSimTimeHour());
        assertEquals(expectedResponse.getSimTimeMin(), actualResponse.getSimTimeMin());
        assertEquals(expectedResponse.isEvBatteryChargeStartStopp(), actualResponse.isEvBatteryChargeStartStopp());

        //verifiera att RestTemplate anropades
        verify(restTemplate, times(1)).getForObject("http://127.0.0.1:5001/info", String.class);
    }   

    @Test
    void testChargeBatteryDirect() {

        
        //konfiguera RestTemplate mock attt returera den simulerade Infooresponse
        when(restTemplate.getForObject("http://127.0.0.1:5001/info", String.class)).thenReturn(expectedJsonResponse);
        // Exekvera metoden
        chargingService.chargeBatteryDirect();

        //verifiera att metoden anropar RestTemplate
        verify(restTemplate, atLeastOnce()).getForObject("http://127.0.0.1:5001/info", String.class);
        
    }

    @Test
    void testChargingSessionOnOptimalChargingHours() {

    }

    @Test
    void testChargingSessionOnOptimalChargingHoursPrice() {

    }

    @Test
    void testDisplayInfoResponse() {

    }


    @Test
    void testFetchAndDisplayBaseload() {

    }

    @Test
    void testFetchAndDisplayPriceForElZone() {

    }

    @Test
    void testPerformChargingSessionWithStrategy() {

    }
}
