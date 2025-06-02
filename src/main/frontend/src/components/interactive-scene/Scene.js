import React, { useState, useEffect } from 'react';
import InfoPanel from './InfoPanel';
import { timeService, priceService, chargingOptimizationService, homeBatteryService, solarPanelService, batteryService } from '../../services/api';
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
  const [solarData, setSolarData] = useState({
    currentProductionKwh: 0,
    productionStatus: 'Ingen produktion',
    productionPercent: 0,
    energySurplus: 0,
    optimizationTips: []
  });
  const [optimalChargingData, setOptimalChargingData] = useState({
    timeRange: '22:00 - 06:00',
    strategy: 'Låg förbrukning'
  });

  // Batteristatus för visuella indikatorer
  const [evBatteryData, setEvBatteryData] = useState({
    percentage: 20,
    energyKwh: 9.26,
    maxCapacityKwh: 46.3,
    isCharging: false
  });

  const [homeBatteryDisplayData, setHomeBatteryDisplayData] = useState({
    percentage: 85,
    energyKwh: 11.5,
    maxCapacityKwh: 13.5,
    healthStatus: 'Optimal'
  });

  const handleObjectClick = (objectType) => {
    setSelectedObject(objectType);
  };

  const toggleCharging = () => {
    setChargingStatus(!chargingStatus);
  };

  // Funktioner för att ladda ur batterier
  const dischargeEVBattery = async () => {
    try {
      const response = await fetch('http://localhost:5001/discharge-ev-battery', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ discharging: 'on' })
      });
      
      if (response.ok) {
        console.log('EV battery discharge initiated');
        // Uppdatera data direkt efter kommando
        setTimeout(() => {
          fetchEvBatteryData();
        }, 1000);
      }
    } catch (error) {
      console.error('Failed to discharge EV battery:', error);
    }
  };

  const dischargeHomeBattery = async () => {
    try {
      const response = await fetch('http://localhost:5001/discharge-home-battery', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ discharging: 'on' })
      });
      
      if (response.ok) {
        console.log('Home battery discharge initiated');
        // Uppdatera data direkt efter kommando
        setTimeout(() => {
          fetchHomeBatteryData();
        }, 1000);
      }
    } catch (error) {
      console.error('Failed to discharge home battery:', error);
    }
  };

  // Hjälpfunktioner för batterifärger
  const getBatteryColor = (percentage) => {
    if (percentage >= 80) return '#27ae60'; // Grön
    if (percentage >= 60) return '#f1c40f'; // Gul
    if (percentage >= 40) return '#f39c12'; // Orange
    if (percentage >= 20) return '#e67e22'; // Mörkare orange
    return '#e74c3c'; // Röd
  };

  const getBatteryStatusStyle = (percentage, isCharging = false) => {
    let color = getBatteryColor(percentage);
    let backgroundColor = `${color}20`; // 20% opacity
    let borderColor = `${color}60`; // 60% opacity
    
    if (isCharging) {
      color = '#27ae60';
      backgroundColor = 'rgba(76, 175, 80, 0.1)';
      borderColor = 'rgba(76, 175, 80, 0.3)';
    }
    
    return {
      color,
      backgroundColor,
      borderColor: `1px solid ${borderColor}`
    };
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

      // Uppdatera också display-data för visual indikator
      setHomeBatteryDisplayData({
        percentage: batteryInfo.batteryLevel || 85,
        energyKwh: batteryInfo.capacityKwh || 11.5,
        maxCapacityKwh: batteryInfo.maxCapacityKwh || 13.5,
        healthStatus: batteryInfo.healthStatus || 'Optimal'
      });
    } catch (error) {
      console.error('Failed to fetch home battery data:', error);
    }
  };

  // Hämta EV batteri data från backend
  const fetchEvBatteryData = async () => {
    try {
      const batteryInfo = await batteryService.getBatteryStatus();
      
      setEvBatteryData({
        percentage: batteryInfo.percentage || 20,
        energyKwh: batteryInfo.currentEnergyKwh || 9.26,
        maxCapacityKwh: batteryInfo.maxCapacityKwh || 46.3,
        isCharging: batteryInfo.isCharging || false
      });

      // Uppdatera charging status baserat på EV batteristatus
      setChargingStatus(batteryInfo.isCharging || false);
    } catch (error) {
      console.error('Failed to fetch EV battery data:', error);
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

  // Hämta solpaneldata från backend
  const fetchSolarData = async () => {
    try {
      const solarStatus = await solarPanelService.getSolarPanelStatus();
      
      setSolarData({
        currentProductionKwh: solarStatus.currentProductionKwh,
        productionStatus: solarStatus.productionStatus,
        productionPercent: solarStatus.productionPercent,
        energySurplus: solarStatus.energySurplus,
        optimizationTips: solarStatus.optimizationTips || []
      });
    } catch (error) {
      console.error('Failed to fetch solar data:', error);
    }
  };

  // Hämta alla data när komponenten laddas och sedan varje sekund
  useEffect(() => {
    fetchTime();
    // fetchPriceData(); // Kommenterad
    fetchHomeBatteryData();
    fetchEvBatteryData();
    fetchOptimalChargingData();
    fetchSolarData();
    
    const interval = setInterval(() => {
      fetchTime();
      // fetchPriceData(); // Kommenterad
      fetchHomeBatteryData();
      fetchEvBatteryData();
      fetchOptimalChargingData();
      fetchSolarData();
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
              <h3>☀️ Solproduktion</h3>
            </div>
            <div className="info-panel-content">
              <div className="price-info">
                <span className="current-price">
                  {solarPanelService.formatProduction(solarData?.currentProductionKwh)}
                </span>
                <span className="price-status">
                  {solarPanelService.getProductionIcon(solarData?.productionStatus)} {solarData?.productionStatus || 'Laddar...'}
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

        {/* Batteristatus-indikatorer på sidorna */}
        <div className="battery-indicators">
          {/* Husbatteri - vänster sida */}
          <div className="battery-indicator home-battery">
            <div className="battery-header">
              <h4>🏠 Husbatteri</h4>
            </div>
            <div className="battery-visual">
              <div className="battery-shell">
                <div 
                  className="battery-fill home-battery-fill"
                  style={{height: `${homeBatteryDisplayData.percentage}%`}}
                ></div>
              </div>
              <div className="battery-tip"></div>
            </div>
            <div className="battery-info">
              <div className="battery-percentage" style={{color: getBatteryColor(homeBatteryDisplayData.percentage)}}>{homeBatteryDisplayData.percentage}%</div>
              <div className="battery-capacity">{homeBatteryDisplayData.energyKwh.toFixed(1)}/{homeBatteryDisplayData.maxCapacityKwh} kWh</div>
              <div className="battery-status" style={getBatteryStatusStyle(homeBatteryDisplayData.percentage)}>{homeBatteryDisplayData.healthStatus}</div>
              <button className="battery-discharge-btn" onClick={dischargeHomeBattery}>
                ⚡ Ladda ur till 10%
              </button>
            </div>
          </div>

          {/* EV Batteri - höger sida */}
          <div className="battery-indicator ev-battery">
            <div className="battery-header">
              <h4>🚗 EV Batteri</h4>
            </div>
            <div className="battery-visual">
              <div className="battery-shell">
                <div 
                  className={`battery-fill ev-battery-fill ${evBatteryData.isCharging ? 'charging' : ''}`}
                  style={{height: `${evBatteryData.percentage}%`}}
                ></div>
              </div>
              <div className="battery-tip"></div>
            </div>
            <div className="battery-info">
              <div className="battery-percentage" style={{color: getBatteryColor(evBatteryData.percentage)}}>{evBatteryData.percentage}%</div>
              <div className="battery-capacity">{evBatteryData.energyKwh.toFixed(1)}/{evBatteryData.maxCapacityKwh} kWh</div>
              <div className="battery-status" style={getBatteryStatusStyle(evBatteryData.percentage, evBatteryData.isCharging)}>{evBatteryData.isCharging ? '⚡ Laddar' : 'Redo'}</div>
              <button className="battery-discharge-btn" onClick={dischargeEVBattery}>
                ⚡ Ladda ur till 20%
              </button>
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