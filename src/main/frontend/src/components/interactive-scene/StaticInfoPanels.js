import React, { useState, useEffect } from 'react';
import { timeService, priceService, chargingOptimizationService } from '../../services/api';
import './InfoPanel.css';

const StaticInfoPanels = () => {
  const [currentTime, setCurrentTime] = useState('--:--');
  const [priceData, setPriceData] = useState({
    currentPrice: 2.50,
    priceStatus: 'Normalpris'
  });
  const [optimalChargingData, setOptimalChargingData] = useState({
    timeRange: '22:00 - 06:00',
    strategy: 'Låg förbrukning'
  });

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

  // Hämta prisdata från backend
  const fetchPriceData = async () => {
    try {
      const priceInfo = await priceService.getCurrentPrice();
      const priceStatus = priceService.getPriceStatus(priceInfo.currentPrice, priceInfo.hourlyPrices);
      
      setPriceData({
        currentPrice: priceInfo.currentPrice,
        priceStatus: priceStatus
      });
    } catch (error) {
      console.error('Failed to fetch price data:', error);
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
    fetchPriceData();
    fetchOptimalChargingData();
    
    const interval = setInterval(() => {
      fetchTime();
      fetchPriceData();
      fetchOptimalChargingData();
    }, 1000); // Uppdatera varje sekund
    
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="top-info-panels">
      <div className="info-panel horizontal-panel price-panel">
        <div className="info-panel-header">
          <h3>💰 Pris per kWh</h3>
        </div>
        <div className="info-panel-content">
          <div className="price-info">
            <span className="current-price">{priceService.formatPrice(priceData.currentPrice)}</span>
            <span className="price-status">{priceData.priceStatus}</span>
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
  );
};

export default StaticInfoPanels; 