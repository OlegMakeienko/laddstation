package com.makeienko.laddstation.controller;

import com.makeienko.laddstation.dto.ChargingSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChargingStationController {

    //post mapping skapar en ny laddning session
    @RequestMapping("/start-session")
    public ResponseEntity<String> startSession(@RequestBody ChargingSession session) {
        // Logik för att hantera laddningssessionen (mockad för nu)
        System.out.println("Startar session: " + session);

        // Returnera svar
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Laddningssession startad för station: " + session.getStationId());

    }

    //get mapping kontrollerar status
    public ResponseEntity<String> getStatus() {
        // Mockat svar
        String status = "Laddstation är aktiv.";
        return ResponseEntity.ok(status);
    }
}
