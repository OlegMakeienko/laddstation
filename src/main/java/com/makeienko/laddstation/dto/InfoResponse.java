package com.makeienko.laddstation.dto;

import lombok.Data;

@Data
public class InfoResponse {
    private double simTimeHour;               // sim_time_hour
    private double simTimeMin;                // sim_time_min
    private double baseCurrentLoad;           // base_current_load
    private double batteryCapacityKWh;       // battery_capacity_kWh
    private boolean evBatteryChargeStartStopp; // ev_battery_charge_start_stopp
}
