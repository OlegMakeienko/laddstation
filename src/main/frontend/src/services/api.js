const API_BASE_URL = 'http://localhost:5001';

export const timeService = {
  // Hämta simulerad tid från backend
  async getCurrentTime() {
    try {
      const response = await fetch(`${API_BASE_URL}/info`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      return {
        hour: data.sim_time_hour,
        minute: data.sim_time_min
      };
    } catch (error) {
      console.error('Error fetching time from backend:', error);
      // Fallback till lokal tid om backend inte är tillgängligt
      const now = new Date();
      return {
        hour: now.getHours(),
        minute: now.getMinutes()
      };
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
  // Hämta hushållsförbrukning per timme
  async getBaseload() {
    try {
      const response = await fetch(`${API_BASE_URL}/baseload`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      return data;
    } catch (error) {
      console.error('Error fetching baseload from backend:', error);
      // Fallback data om backend inte är tillgängligt
      return Array(24).fill(0).map(() => Math.random() * 5 + 2); // 2-7 kWh
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
  // Hämta batteristatus från backend
  async getBatteryStatus() {
    try {
      const response = await fetch(`${API_BASE_URL}/info`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      // Konvertera från Python-serverns format till det format som React-appen förväntar sig
      return {
        percentage: Math.round((data.battery_energy_kwh / data.ev_batt_max_capacity_kwh) * 100),
        currentEnergyKwh: data.battery_energy_kwh,
        maxCapacityKwh: data.ev_batt_max_capacity_kwh,
        isCharging: data.ev_battery_charge_start_stopp
      };
    } catch (error) {
      console.error('Error fetching battery status from backend:', error);
      // Fallback data om backend inte är tillgängligt
      return {
        percentage: 45,
        currentEnergyKwh: 20.8,
        maxCapacityKwh: 46.3,
        isCharging: false
      };
    }
  }
};

export const priceService = {
  // Hämta aktuellt timpris från backend
  async getCurrentPrice() {
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
    } catch (error) {
      console.error('Error fetching current price from backend:', error);
      // Fallback data om backend inte är tillgängligt
      return {
        currentPrice: 2.50,
        currentHour: new Date().getHours(),
        hourlyPrices: Array(24).fill(2.50)
      };
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
  // Hämta optimala laddningstider från backend
  async getOptimalChargingHours() {
    try {
      const response = await fetch(`${API_BASE_URL}/optimal-charging-hours`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      return data;
    } catch (error) {
      console.error('Error fetching optimal charging hours from backend:', error);
      // Fallback data om backend inte är tillgängligt
      return {
        optimalHours: [22, 23, 0, 1, 2, 3, 4, 5],
        strategy: 'Låg förbrukning',
        timeRange: '22:00 - 06:00'
      };
    }
  }
};