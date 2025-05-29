import React, { useState, useEffect } from 'react';
import InfoPanel from './InfoPanel';
import { timeService } from '../../services/api';
import './Scene.css';

const Scene = () => {
  const [selectedObject, setSelectedObject] = useState(null);
  const [chargingStatus, setChargingStatus] = useState(false);
  const [currentTime, setCurrentTime] = useState('--:--');

  const handleObjectClick = (objectType) => {
    setSelectedObject(objectType);
  };

  const toggleCharging = () => {
    setChargingStatus(!chargingStatus);
  };

  // Hämta tid från backend
  const fetchTime = async () => {
    try {
      const timeData = await timeService.getCurrentTime();
      const formattedTime = timeService.formatTime(timeData.hour, timeData.minute);
      setCurrentTime(formattedTime);
    } catch (error) {
      console.error('Failed to fetch time:', error);
    }
  };

  // Hämta tid när komponenten laddas och sedan varje sekund
  useEffect(() => {
    fetchTime();
    const interval = setInterval(fetchTime, 1000); // Uppdatera varje sekund
    
    return () => clearInterval(interval);
  }, []);

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
            className="clickable-area house-area"
            onClick={() => handleObjectClick('house')}
          />
          
          {/* Car clickable area - center */}
          <rect 
            x="330" 
            y="225" 
            width="180" 
            height="120" 
            fill="transparent"
            className="clickable-area car-area"
            onClick={() => handleObjectClick('car')}
          />
          
          {/* Charging station clickable area - right side */}
          <rect 
            x="542" 
            y="185" 
            width="50" 
            height="120" 
            fill="transparent"
            className="clickable-area station-area"
            onClick={() => handleObjectClick('chargingStation')}
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
                cx="380" 
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
        <div className="top-info-panels">
          <div className="info-panel horizontal-panel price-panel">
            <div className="info-panel-header">
              <h3>💰 Pris per kWh</h3>
            </div>
            <div className="info-panel-content">
              <div className="price-info">
                <span className="current-price">2.50 kr/kWh</span>
                <span className="price-status">Normalpris</span>
              </div>
            </div>
          </div>

          <div className="info-panel horizontal-panel clock-panel">
            <div className="info-panel-header">
              <h3>🕐 Klockan är</h3>
            </div>
            <div className="info-panel-content">
              <div className="clock-info">
                <span className="current-time">{currentTime}</span>
              </div>
            </div>
          </div>

          <div className="info-panel horizontal-panel time-panel">
            <div className="info-panel-header">
              <h3>⏰ Bra tid för laddning</h3>
            </div>
            <div className="info-panel-content">
              <div className="time-info">
                <span className="best-time">22:00 - 06:00</span>
                <span className="time-status">Låg förbrukning</span>
              </div>
            </div>
          </div>
        </div>

        {/* Info Panel */}
        {selectedObject && (
          <InfoPanel 
            objectType={selectedObject}
            chargingStatus={chargingStatus}
            onToggleCharging={toggleCharging}
            onClose={() => setSelectedObject(null)}
          />
        )}
      </div>
    </div>
  );
};

export default Scene; 