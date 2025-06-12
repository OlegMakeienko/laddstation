const API_BASE_URL = 'http://localhost:5001';
const API_JAVA_BACKEND_URL = 'http://localhost:8080';

export const timeService = {
  // Hämta simulerad tid från Java-backend
  async getCurrentTime() {
    try {
      // Använd Java-servern för tid
      const response = await fetch(`${API_JAVA_BACKEND_URL}/api/time`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      return data; // Java-servern returnerar redan rätt format {hour, minute}
    } catch (error) {
      console.error('Error fetching time from Java backend:', error);
      // Fallback till lokal tid om Java-servern inte är tillgänglig
      const now = new Date();
      return {
        hour: now.getHours(),
        minute: now.getMinutes()
      };
      /* Python-fallback inaktiverad
      try {
        const pythonResponse = await fetch(`${API_BASE_URL}/info`);
        if (!pythonResponse.ok) {
          throw new Error(`HTTP error! status: ${pythonResponse.status}`);
        }
        const pythonData = await pythonResponse.json();
        return {
          hour: pythonData.sim_time_hour,
          minute: pythonData.sim_time_min
        };
      } catch (innerError) {
        console.error('Failed to get time from Python backend too:', innerError);
        // Fallback till lokal tid om inget backend är tillgängligt
        const now = new Date();
        return {
          hour: now.getHours(),
          minute: now.getMinutes()
        };
      }
      */
    }
  },

  // Formatera tid till svensk format
  formatTime(hour, minute) {
    const h = Math.floor(hour);
    const m = Math.floor(minute);
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`;
  }
};

export const householdService = {
  // Hämta hushållsförbrukning per timme från Java-backend
  async getBaseload() {
    try {
      // Använd Java-servern för hushållsförbrukning
      const response = await fetch(`${API_JAVA_BACKEND_URL}/api/baseload`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      return data;
    } catch (error) {
      console.error('Error fetching baseload from Java backend:', error);
      // Fallback data om Java-servern inte är tillgänglig
      return Array(24).fill(0).map(() => Math.random() * 5 + 2); // 2-7 kWh
      /* Python-fallback inaktiverad
      try {
        const pythonResponse = await fetch(`${API_BASE_URL}/baseload`);
        if (!pythonResponse.ok) {
          throw new Error(`HTTP error! status: ${pythonResponse.status}`);
        }
        const data = await pythonResponse.json();
        return data;
      } catch (innerError) {
        console.error('Failed to get baseload from Python backend too:', innerError);
        // Fallback data om inget backend är tillgängligt
        return Array(24).fill(0).map(() => Math.random() * 5 + 2); // 2-7 kWh
      }
      */
    }
  },

  // Beräkna aktuell förbrukning baserat på tid
  getCurrentConsumption(baseloadArray, currentHour) {
    if (!baseloadArray || baseloadArray.length === 0) return 0;
    const hour = Math.floor(currentHour) % 24;
    return baseloadArray[hour] || 0;
  },

  // Beräkna dagens totala förbrukning
  getTotalDailyConsumption(baseloadArray) {
    if (!baseloadArray || baseloadArray.length === 0) return 0;
    return baseloadArray.reduce((sum, hourly) => sum + hourly, 0);
  }
};

export const batteryService = {
  // Hämta batteristatus från Java-backend
  async getBatteryStatus() {
    try {
      // Använd Java-servern för batteristatus
      const response = await fetch(`${API_JAVA_BACKEND_URL}/api/ev-battery`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      return data; // Java-servern returnerar redan rätt format
    } catch (error) {
      console.error('Error fetching battery status from Java backend:', error);
      // Fallback data om Java-servern inte är tillgänglig
      return {
        percentage: 45,
        currentEnergyKwh: 20.8,
        maxCapacityKwh: 46.3,
        isCharging: false
      };
      /* Python-fallback inaktiverad
      try {
        const pythonResponse = await fetch(`${API_BASE_URL}/info`);
        if (!pythonResponse.ok) {
          throw new Error(`HTTP error! status: ${pythonResponse.status}`);
        }
        const data = await pythonResponse.json();
        // Konvertera från Python-serverns format till det format som React-appen förväntar sig
        return {
          percentage: Math.round((data.battery_energy_kwh / data.ev_batt_max_capacity_kwh) * 100),
          currentEnergyKwh: data.battery_energy_kwh,
          maxCapacityKwh: data.ev_batt_max_capacity_kwh,
          isCharging: data.ev_battery_charge_start_stopp
        };
      } catch (innerError) {
        console.error('Failed to get battery status from Python backend too:', innerError);
        // Fallback data om inget backend är tillgängligt
        return {
          percentage: 45,
          currentEnergyKwh: 20.8,
          maxCapacityKwh: 46.3,
          isCharging: false
        };
      }
      */
    }
  },
  
  // Ladda ur batteriet till 20%
  async dischargeBattery() {
    try {
      const response = await fetch(`${API_JAVA_BACKEND_URL}/api/discharge`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Error discharging battery:', error);
      throw error;
    }
  }
};

export const priceService = {
  // Hämta aktuellt timpris från Java-backend
  async getCurrentPrice() {
    try {
      // Använd Java-servern för prisinformation
      const response = await fetch(`${API_JAVA_BACKEND_URL}/api/current-price`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      return data; // Java-servern returnerar redan rätt format
    } catch (error) {
      console.error('Error fetching current price from Java backend:', error);
      // Fallback data om Java-servern inte är tillgänglig
      return {
        currentPrice: 2.50,
        currentHour: new Date().getHours(),
        hourlyPrices: Array(24).fill(2.50)
      };
      /* Python-fallback inaktiverad
      try {
        // Hämta priser per timme
        const priceResponse = await fetch(`${API_BASE_URL}/priceperhour`);
        if (!priceResponse.ok) {
          throw new Error(`HTTP error! status: ${priceResponse.status}`);
        }
        const hourlyPrices = await priceResponse.json();

        // Hämta aktuell timme
        const timeResponse = await fetch(`${API_BASE_URL}/info`);
        if (!timeResponse.ok) {
          throw new Error(`HTTP error! status: ${timeResponse.status}`);
        }
        const timeData = await timeResponse.json();
        const currentHour = timeData.sim_time_hour;
        
        // Returnera data i det format som React-appen förväntar sig
        return {
          currentPrice: hourlyPrices[currentHour] / 100, // Konvertera från öre till kronor
          currentHour: currentHour,
          hourlyPrices: hourlyPrices.map(price => price / 100) // Konvertera från öre till kronor
        };
      } catch (innerError) {
        console.error('Failed to get price from Python backend too:', innerError);
        // Fallback data om inget backend är tillgängligt
        return {
          currentPrice: 2.50,
          currentHour: new Date().getHours(),
          hourlyPrices: Array(24).fill(2.50)
        };
      }
      */
    }
  },

  // Formatera pris till svensk valuta
  formatPrice(price) {
    return `${price.toFixed(2)} kr/kWh`;
  },

  // Bestäm prisstatus baserat på pris
  getPriceStatus(currentPrice, hourlyPrices) {
    if (!hourlyPrices || hourlyPrices.length === 0) return 'Normalpris';
    
    const minPrice = Math.min(...hourlyPrices);
    const maxPrice = Math.max(...hourlyPrices);
    const avgPrice = hourlyPrices.reduce((sum, price) => sum + price, 0) / hourlyPrices.length;
    
    if (currentPrice <= minPrice + (avgPrice - minPrice) * 0.3) {
      return 'Lågt pris';
    } else if (currentPrice >= maxPrice - (maxPrice - avgPrice) * 0.3) {
      return 'Högt pris';
    } else {
      return 'Normalpris';
    }
  }
};

export const chargingOptimizationService = {
  // Hämta optimala laddningstider från Java-backend
  async getOptimalChargingHours() {
    try {
      // Använd Java-servern för optimala laddningstider
      const response = await fetch(`${API_JAVA_BACKEND_URL}/api/optimal-charging-hours`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      return data;
    } catch (error) {
      console.error('Error fetching optimal charging hours from Java backend:', error);
      // Fallback data om Java-servern inte är tillgänglig
      return {
        optimalHours: [22, 23, 0, 1, 2, 3, 4, 5],
        strategy: 'Låg förbrukning',
        timeRange: '22:00 - 06:00'
      };
      /* Python-fallback inaktiverad
      try {
        // Hämta baseload från Python-servern
        const baseloadResponse = await fetch(`${API_BASE_URL}/baseload`);
        if (!baseloadResponse.ok) {
          throw new Error(`HTTP error! status: ${baseloadResponse.status}`);
        }
        const baseloadData = await baseloadResponse.json();
        
        // Beräkna de 8 timmarna med lägst förbrukning
        const loadWithIndex = baseloadData.map((load, index) => ({ load, index }));
        loadWithIndex.sort((a, b) => a.load - b.load);
        const optimalHours = loadWithIndex.slice(0, 8).map(item => item.index).sort((a, b) => a - b);
        
        // Skapa en tidsintervallsträng baserat på timmarna
        let timeRange;
        if (optimalHours.length > 0) {
          const startHour = optimalHours[0];
          const endHour = (optimalHours[optimalHours.length - 1] + 1) % 24;
          timeRange = `${startHour.toString().padStart(2, '0')}:00 - ${endHour.toString().padStart(2, '0')}:00`;
        } else {
          timeRange = "22:00 - 06:00"; // Fallback
        }
        
        return {
          optimalHours: optimalHours,
          strategy: 'Låg förbrukning',
          timeRange: timeRange
        };
      } catch (innerError) {
        console.error('Failed to calculate optimal hours:', innerError);
        // Fallback data om ingen beräkning är möjlig
        return {
          optimalHours: [22, 23, 0, 1, 2, 3, 4, 5],
          strategy: 'Låg förbrukning',
          timeRange: '22:00 - 06:00'
        };
      }
      */
    }
  }
};