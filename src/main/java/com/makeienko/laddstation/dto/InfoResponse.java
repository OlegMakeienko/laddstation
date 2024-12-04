package com.makeienko.laddstation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InfoResponse {
    @JsonProperty("sim_time_hour")
    private double simTimeHour; // sim_time_hour

    @JsonProperty("sim_time_min")
    private double simTimeMin;  // sim_time_min

    @JsonProperty("base_current_load")
    private double baseCurrentLoad; // base_current_load

    @JsonProperty("battery_capacity_kWh")
    private double batteryCapacityKWh; // battery_capacity_kWh

    @JsonProperty("ev_battery_charge_start_stopp")
    private boolean evBatteryChargeStartStopp; // ev_battery_charge_start_stopp
}
