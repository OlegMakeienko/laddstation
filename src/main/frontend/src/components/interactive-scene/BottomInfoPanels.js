import React, { useState, useEffect } from 'react';
import { householdService, timeService, batteryService } from '../../services/api';
import './PanelStyles.css';
import './BottomInfoPanels.css';

const BottomInfoPanels = () => {
  const [householdData, setHouseholdData] = useState({
    currentConsumption: 0,
    dailyTotal: 0,
    baseloadArray: []
  });

  const [batteryData, setBatteryData] = useState({
    percentage: 0,
    currentEnergyKwh: 0,
    maxCapacityKwh: 0,
    isCharging: false
  });

  const fetchHouseholdData = async () => {
    try {
      const baseload = await householdService.getBaseload();
      const timeData = await timeService.getCurrentTime();
      
      const currentConsumption = householdService.getCurrentConsumption(baseload, timeData.hour);
      const dailyTotal = householdService.getTotalDailyConsumption(baseload);
      
      setHouseholdData({
        currentConsumption: currentConsumption.toFixed(2),
        dailyTotal: dailyTotal.toFixed(1),
        baseloadArray: baseload
      });
    } catch (error) {
      console.error('Failed to fetch household data:', error);
    }
  };

  const fetchBatteryData = async () => {
    try {
      const battery = await batteryService.getBatteryStatus();
      setBatteryData(battery);
    } catch (error) {
      console.error('Failed to fetch battery data:', error);
    }
  };

  // Hämta data när komponenten laddas och sedan varje sekund
  useEffect(() => {
    fetchHouseholdData();
    fetchBatteryData();
    
    const interval = setInterval(() => {
      fetchHouseholdData();
      fetchBatteryData();
    }, 1000); // Uppdatera varje sekund
    
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="bottom-info-panels">
      <div className="info-panel horizontal-panel house-panel">
        <div className="info-panel-header">
          <h3>🏠 Smart Hus</h3>
        </div>
        <div className="info-panel-content">
          <div className="house-info">
            <span className="current-consumption">{householdData.currentConsumption} kWh</span>
            <span className="consumption-status">Aktuell förbrukning</span>
          </div>
        </div>
      </div>

      <div className="info-panel horizontal-panel car-panel">
        <div className="info-panel-header">
          <h3>🚗 Elbil</h3>
        </div>
        <div className="info-panel-content">
          <div className="battery-info">
            <span className="current-battery">{batteryData.percentage}%</span>
            <span className="battery-status">EV Batteristatus</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default BottomInfoPanels; 