import React from 'react';
import './InfoPanel.css';

const InfoPanel = ({ objectType, chargingStatus, onToggleCharging, onClose }) => {
  const getObjectInfo = () => {
    switch (objectType) {
      case 'house':
        return {
          title: '🏠 Smart Hus',
          info: [
            'Energiförbrukning: 15 kWh/dag',
            'Solpaneler: 8 kW installerat',
            'Energiproduktion idag: 32 kWh',
            'Nettoexport: +17 kWh',
            'Status: Producerar överskott'
          ]
        };
      case 'car':
        return {
          title: '🚗 Elbil',
          info: [
            'Batteristatus: 45%',
            'Räckvidd kvar: 180 km',
            'Laddningshastighet: 11 kW',
            'Tid till fullt: 3h 20min',
            `Status: ${chargingStatus ? 'Laddar aktiv' : 'Redo för laddning'}`
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