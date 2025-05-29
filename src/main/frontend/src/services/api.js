const API_BASE_URL = 'http://localhost:8080/api';

export const timeService = {
  // Hämta simulerad tid från backend
  async getCurrentTime() {
    try {
      const response = await fetch(`${API_BASE_URL}/time`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      return data;
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