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
            <div className="info-item">
              <span className="info-label">Aktuell förbrukning:</span>
              <span className="info-value">{householdData.currentConsumption} kWh</span>
            </div>
            <div className="info-item">
              <span className="info-label">Dagens totala förbrukning:</span>
              <span className="info-value">{householdData.dailyTotal} kWh</span>
            </div>
          </div>
        </div>
      </div>

      <div className="info-panel horizontal-panel car-panel">
        <div className="info-panel-header">
          <h3>🚗 Elbil</h3>
        </div>
        <div className="info-panel-content">
          <div className="battery-info">
            <div className="info-item">
              <span className="info-label">EV Batteristatus:</span>
              <span className="info-value">{batteryData.percentage}%</span>
            </div>
            <div className="info-item">
              <span className="info-label">Batterikapacitet:</span>
              <span className="info-value">{batteryData.currentEnergyKwh}/{batteryData.maxCapacityKwh} kWh</span>
            </div>
            <div className="info-item">
              <span className="info-label">Status:</span>
              <span className="info-value">{batteryData.isCharging ? 'Laddar aktiv' : 'Redo för laddning'}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default BottomInfoPanels; 