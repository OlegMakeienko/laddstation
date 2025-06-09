package com.makeienko.laddstation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.service.strategy.OptimalHoursStrategy;

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
        expectedResponse.setHouseholdLoadKwh(5.0);
        expectedResponse.setEvBatteryEnergyKwh(9.26);
        expectedResponse.setEvBattMaxCapacityKwh(46.3);
        expectedResponse.setSimTimeHour(14);
        expectedResponse.setSimTimeMin(30);
        expectedResponse.setEvBatteryChargeStartStopp(true);

        //konvertera till JSON-sträng för återanvändnig
        expectedJsonResponse = objectMapper.writeValueAsString(expectedResponse);
    }

    @Test
    void testFetchAndDeserializeInfo() {

        /* //konfigera mock
        when(restTemplate.getForObject("http://127.0.0.1:5001/info", String.class)).thenReturn(expectedJsonResponse);

        //exekvera metoden
        InfoResponse actualResponse = chargingService.fetchAndDeserializeInfo();

        //verifiera resultaten
        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getHouseholdLoadKwh(), actualResponse.getHouseholdLoadKwh());
        assertEquals(expectedResponse.getEvBatteryEnergyKwh(), actualResponse.getEvBatteryEnergyKwh());
        assertEquals(expectedResponse.getEvBattMaxCapacityKwh(), actualResponse.getEvBattMaxCapacityKwh());
        assertEquals(expectedResponse.getSimTimeHour(), actualResponse.getSimTimeHour());
        assertEquals(expectedResponse.getSimTimeMin(), actualResponse.getSimTimeMin());
        assertEquals(expectedResponse.isEvBatteryChargeStartStopp(), actualResponse.isEvBatteryChargeStartStopp());

        //verifiera att RestTemplate anropades
        verify(res tTemplate, times(1)).getForObject("http://127.0.0.1:5001/info", String.class);*/
    }   

    @Test
    void testChargeBatteryDirect() throws Exception {
        
        /* //konfiguera RestTemplate mock attt returera olika svar i sekvens
        when(restTemplate.getForObject("http://127.0.0.1:5001/info", String.class)).thenReturn(expectedJsonResponse);
        
        // Fånga konsoloutput för verifiering
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            // Exekvera metoden
            chargingService.chargeBatteryDirect();

            //verifiera att metoden anropar RestTemplate 3 gånger
            verify(restTemplate, times(1)).getForObject("http://127.0.0.1:5001/info", String.class);

            //verifiera konsoloutput
            String consoleOutput = outputStream.toString();
            assertTrue(consoleOutput.contains("Household Load: 5.0 kW"));
            assertTrue(consoleOutput.contains("EV Battery Energy: 9.26 kWh"));
            assertTrue(consoleOutput.contains("EV Battery Max Capacity: 46.3 kWh"));
            assertTrue(consoleOutput.contains("Current server battery level: 20.0%"));
        } finally {
            System.setOut(originalOut);
        } */
    }

    @Test
    void testChargingSessionOnOptimalChargingHours() throws Exception {

        //Skapa mock för optimala timmar för OptimalHoursStrategy
        OptimalHoursStrategy mockStrategy = mock(OptimalHoursStrategy.class);
        List<Double> optimalHours = Arrays.asList(1.0, 2.0, 13.0, 14.0, 22.0, 23.0, 24.0);
        when(mockStrategy.findOptimalHours()).thenReturn(optimalHours);
        


    }

    @Test
    void testChargingSessionOnOptimalChargingHoursPrice() {
        


    }

    @Test
    void testPerformChargingSessionWithStrategy() throws Exception {
        //Skapa mock för optimala timmar för OptimalHoursStrategy
        OptimalHoursStrategy mockStrategy = mock(OptimalHoursStrategy.class);
        List<Double> optimalHours = Arrays.asList(1.0, 2.0, 13.0, 14.0, 22.0, 23.0, 24.0);
        when(mockStrategy.findOptimalHours()).thenReturn(optimalHours);

        //konfiguera InfoResponse att returera timme 2 (optimal)
        //expectedResponse.setSimTimeHour(14.0);
        when(restTemplate.getForObject("http://127.0.0.1:5001/info", String.class)).thenReturn(expectedJsonResponse);

        //konfiguera svar för startCharging och stopCharging
        when(restTemplate.postForObject(eq("http://127.0.0.1:5001/charge"), any(Map.class), eq(String.class))).thenReturn("Charging status changed");

        //fånga konsoluotput
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        /* try {
            //kör metoden
            chargingService.performChargingSessionWithStrategy(mockStrategy);

            //verifiera anrop
            verify(mockStrategy).findOptimalHours();
            verify(restTemplate, atLeastOnce()).getForObject("http://127.0.0.1:5001/info", String.class);
            verify(restTemplate, times(2)).postForObject(eq("http://127.0.0.1:5001/charge"), any(Map.class), eq(String.class));

            //verifiera loggade meddelandden
            String output = outputStream.toString();
            assertTrue(output.contains("Current hour (14.0) is optimal for charging"));
        } finally {
            System.setOut(originalOut);
        } */
    }

    @Test
    void testIsOptimalHour() {
        List<Double> optimalHours = Arrays.asList(1.0, 5.0, 23.0);
        
        // Test positiva fall
        assertTrue(chargingService.isOptimalHour(1.0, optimalHours));
        assertTrue(chargingService.isOptimalHour(5.0, optimalHours));
        
        // Test negativa fall
        assertFalse(chargingService.isOptimalHour(2.0, optimalHours));
        assertFalse(chargingService.isOptimalHour(0.0, optimalHours));
    }
}
