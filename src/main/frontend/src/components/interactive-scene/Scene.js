import React, { useState, useEffect } from 'react';
import InfoPanel from './InfoPanel';
import { timeService, priceService, chargingOptimizationService, homeBatteryService } from '../../services/api';
import './Scene.css';

const Scene = () => {
  const [selectedObject, setSelectedObject] = useState(null);
  const [chargingStatus, setChargingStatus] = useState(false);
  const [currentTime, setCurrentTime] = useState('--:--');
  // const [priceData, setPriceData] = useState({
  //   currentPrice: 2.50,
  //   priceStatus: 'Normalpris'
  // });
  const [homeBatteryData, setHomeBatteryData] = useState({
    batteryLevel: 85,
    healthStatus: 'Optimal',
    reserveHours: 12.5,
    v2hSafe: true
  });
  const [optimalChargingData, setOptimalChargingData] = useState({
    timeRange: '22:00 - 06:00',
    strategy: 'Låg förbrukning'
  });

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

  // Hämta prisdata från backend (kommenterad)
  // const fetchPriceData = async () => {
  //   try {
  //     const priceInfo = await priceService.getCurrentPrice();
  //     const priceStatus = priceService.getPriceStatus(priceInfo.currentPrice, priceInfo.hourlyPrices);
  //     
  //     setPriceData({
  //       currentPrice: priceInfo.currentPrice,
  //       priceStatus: priceStatus
  //     });
  //   } catch (error) {
  //     console.error('Failed to fetch price data:', error);
  //   }
  // };

  // Hämta husbatteri data från backend
  const fetchHomeBatteryData = async () => {
    try {
      const batteryInfo = await homeBatteryService.getHomeBatteryStatus();
      
      setHomeBatteryData({
        batteryLevel: batteryInfo.batteryLevel,
        healthStatus: batteryInfo.healthStatus,
        reserveHours: batteryInfo.reserveHours,
        v2hSafe: batteryInfo.v2hSafe
      });
    } catch (error) {
      console.error('Failed to fetch home battery data:', error);
    }
  };

  // Hämta optimala laddningstider från backend
  const fetchOptimalChargingData = async () => {
    try {
      const chargingInfo = await chargingOptimizationService.getOptimalChargingHours();
      
      setOptimalChargingData({
        timeRange: chargingInfo.timeRange,
        strategy: chargingInfo.strategy
      });
    } catch (error) {
      console.error('Failed to fetch optimal charging data:', error);
    }
  };

  // Hämta alla data när komponenten laddas och sedan varje sekund
  useEffect(() => {
    fetchTime();
    // fetchPriceData(); // Kommenterad
    fetchHomeBatteryData();
    fetchOptimalChargingData();
    
    const interval = setInterval(() => {
      fetchTime();
      // fetchPriceData(); // Kommenterad
      fetchHomeBatteryData();
      fetchOptimalChargingData();
    }, 1000); // Uppdatera varje sekund
    
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
              <h3>🔋 Husbatteri Status</h3>
            </div>
            <div className="info-panel-content">
              <div className="price-info">
                {/* Kommenterad prisdata:
                <span className="current-price">{priceService.formatPrice(priceData.currentPrice)}</span>
                <span className="price-status">{priceData.priceStatus}</span> */}
                <span className="current-price">
                  {homeBatteryService.formatBatteryLevel(homeBatteryData?.batteryLevel)}
                </span>
                <span className="price-status">
                  {homeBatteryService.getHealthIcon(homeBatteryData?.healthStatus)} {homeBatteryData?.healthStatus || 'Laddar...'}
                </span>
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
                <span className="best-time">{optimalChargingData.timeRange}</span>
                <span className="time-status">{optimalChargingData.strategy}</span>
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