import React, { useState, useEffect } from 'react';
import StaticInfoPanels from './StaticInfoPanels';
import BottomInfoPanels from './BottomInfoPanels';
import { batteryService } from '../../services/api';
import './Scene.css';

const Scene = () => {
  const [chargingStatus, setChargingStatus] = useState(false);
  
  // Hämta initial laddningsstatus från backend
  useEffect(() => {
    const fetchChargingStatus = async () => {
      try {
        const batteryData = await batteryService.getBatteryStatus();
        setChargingStatus(batteryData.isCharging);
      } catch (error) {
        console.error('Error fetching initial charging status:', error);
      }
    };
    
    fetchChargingStatus();
    
    // Uppdatera laddningsstatus varje 5 sekunder
    const interval = setInterval(fetchChargingStatus, 5000);
    
    return () => clearInterval(interval);
  }, []);

  const toggleCharging = async () => {
    try {
      // Anropa backend för att starta eller stoppa laddningen
      const endpoint = chargingStatus 
        ? 'http://localhost:8080/api/charge/stop' 
        : 'http://localhost:8080/api/charge/start';
        
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        // Uppdatera lokal state bara om anropet lyckades
        setChargingStatus(!chargingStatus);
      } else {
        console.error('Failed to toggle charging status');
      }
    } catch (error) {
      console.error('Error toggling charging:', error);
    }
  };

  return (
    <div className="scene-container">
      <div className="scene-wrapper">
        <div className="background-image"></div>
        
        <svg 
          width="100%" 
          height="100%" 
          viewBox="0 0 600 400" 
          xmlns="http://www.w3.org/2000/svg"
          className="interactive-overlay"
        >
          {/* House clickable area - left side */}
          <rect 
            x="20" 
            y="80" 
            width="250" 
            height="250" 
            fill="transparent"
            //className="clickable-area"
          />
          
          {/* Car clickable area - center */}
          <rect 
            x="330" 
            y="225" 
            width="180" 
            height="120" 
            fill="transparent"
            //className="clickable-area"
          />
          
          {/* Charging station clickable area - right side */}
          <rect 
            x="520" 
            y="190" 
            width="50" 
            height="120" 
            fill="transparent"
            className="clickable-area"
            onClick={toggleCharging}
          />

          {/* Visual indicators when charging */}
          {chargingStatus && (
            <g className="charging-indicators">
              {/* Charging cable glow effect */}
              <path 
                d="M520 220 Q480 240 420 260" 
                stroke="#00ff00" 
                strokeWidth="3" 
                fill="none" 
                className="charging-cable-glow"
                opacity="0.8"
              />
              
              {/* Charging station indicator */}
              <circle 
                cx="530" 
                cy="180" 
                r="8" 
                fill="#00ff00" 
                className="charging-indicator pulse"
              />
              
              {/* Car charging indicator */}
              <circle 
                cx="320" 
                cy="240" 
                r="6" 
                fill="#00ff00" 
                className="charging-indicator pulse"
              />
            </g>
          )}

          {/* Interactive hints */}
        </svg>

        {/* Static info panels at top */}
        <StaticInfoPanels />

        {/* Static info panels at bottom */}
        <BottomInfoPanels />

      </div>
    </div>
  );
};

export default Scene; 