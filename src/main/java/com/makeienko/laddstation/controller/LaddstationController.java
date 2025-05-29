package com.makeienko.laddstation.controller;

import com.makeienko.laddstation.dto.InfoResponse;
import com.makeienko.laddstation.service.LaddstationApiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class LaddstationController {

    private final LaddstationApiClient apiClient;

    public LaddstationController(LaddstationApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Hämtar simulerad tid och batteristatus från Python servern
     */
    @GetMapping("/info")
    public ResponseEntity<InfoResponse> getInfo() {
        try {
            InfoResponse info = apiClient.getInfo();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint specifikt för att hämta bara tiden
     */
    @GetMapping("/time")
    public ResponseEntity<TimeResponse> getCurrentTime() {
        try {
            InfoResponse info = apiClient.getInfo();
            TimeResponse timeResponse = new TimeResponse(
                info.getSimTimeHour(), 
                info.getSimTimeMin()
            );
            return ResponseEntity.ok(timeResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Hämtar hushållets basförbrukning per timme
     */
    @GetMapping("/baseload")
    public ResponseEntity<double[]> getBaseload() {
        try {
            double[] baseload = apiClient.getBaseload();
            return ResponseEntity.ok(baseload);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DTO för att returnera bara tid-information
     */
    public static class TimeResponse {
        private double hour;
        private double minute;

        public TimeResponse(double hour, double minute) {
            this.hour = hour;
            this.minute = minute;
        }

        public double getHour() { return hour; }
        public double getMinute() { return minute; }
        public void setHour(double hour) { this.hour = hour; }
        public void setMinute(double minute) { this.minute = minute; }
    }
} 