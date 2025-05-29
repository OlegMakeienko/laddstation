import React, { useState, useEffect } from 'react';
import { householdService, timeService, batteryService } from '../../services/api';
import './InfoPanel.css';

const InfoPanel = ({ objectType, chargingStatus, onToggleCharging, onClose }) => {
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

  // Hämta data när panel öppnas
  useEffect(() => {
    if (objectType === 'house') {
      fetchHouseholdData();
    } else if (objectType === 'car') {
      fetchBatteryData();
    }
  }, [objectType]);

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

  const getObjectInfo = () => {
    switch (objectType) {
      case 'house':
        return {
          title: '🏠 Smart Hus',
          info: [
            `Aktuell förbrukning: ${householdData.currentConsumption} kWh`,
            `Dagens totala förbrukning: ${householdData.dailyTotal} kWh`,
            'Solpaneler: ',
            'Energiproduktion idag: ',
            'Nettoexport: ',
            'Status: '
          ]
        };
      case 'car':
        return {
          title: '🚗 Elbil',
          info: [
            `Batteristatus: ${batteryData.percentage}%`,
            `Batterikapacitet: ${batteryData.currentEnergyKwh}/${batteryData.maxCapacityKwh} kWh`,
            `Status: ${batteryData.isCharging ? 'Laddar aktiv' : 'Redo för laddning'}`
          ]
        };
      case 'chargingStation':
        return {
          title: '⚡ Laddstation',
          info: [
            'Modell: EVCharger Pro 11kW',
            'Maxeffekt: 11 kW',
            'Spänning: 400V 3-fas',
            'Pris: 2.50 kr/kWh',
            `Status: ${chargingStatus ? 'Aktivt laddning' : 'Ledig'}`
          ]
        };
      default:
        return { title: 'Information', info: [] };
    }
  };

  const { title, info } = getObjectInfo();

  const getPanelClass = () => {
    switch (objectType) {
      case 'house':
        return 'house-panel';
      case 'car':
        return 'car-panel';
      case 'chargingStation':
        return 'station-panel';
      default:
        return '';
    }
  };

  return (
    <div className={`info-panel ${getPanelClass()}`}>
      <div className="info-panel-header">
        <h3>{title}</h3>
        <button className="close-btn" onClick={onClose}>✕</button>
      </div>
      
      <div className="info-panel-content">
        <ul>
          {info.map((item, index) => (
            <li key={index}>{item}</li>
          ))}
        </ul>
        
        {objectType === 'chargingStation' && (
          <div className="control-buttons">
            <button 
              className={`charge-btn ${chargingStatus ? 'stop' : 'start'}`}
              onClick={onToggleCharging}
            >
              {chargingStatus ? '⏹️ Stoppa Laddning' : '▶️ Starta Laddning'}
            </button>
          </div>
        )}
        
        {objectType === 'car' && chargingStatus && (
          <div className="charging-progress">
            <div className="progress-bar">
              <div className="progress-fill" style={{width: '45%'}}></div>
            </div>
            <p>Laddar... 45% → 100%</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default InfoPanel; 