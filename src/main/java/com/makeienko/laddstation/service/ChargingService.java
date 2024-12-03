package com.makeienko.laddstation.service;

import com.makeienko.laddstation.dto.ChargingSession;
import org.springframework.stereotype.Service;

@Service
public interface ChargingService {
    ChargingSession fetchData(); // Hämta data från API
    void optimizeCharging(ChargingSession session); // Optimera laddningsschemat
    void startCharging(); // Starta laddning
    void stopCharging(); // Stoppa laddning
}
