package com.makeienko.laddstation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InfoResponse {
    @JsonProperty("sim_time_hour")
    private double simTimeHour; // sim_time_hour

    @JsonProperty("sim_time_min")
    private double simTimeMin;  // sim_time_min

    @JsonProperty("household_load_kwh")
    private double householdLoadKwh; // base_current_load

    @JsonProperty("battery_energy_kwh")
    private double evBatteryEnergyKwh;  // battery_capacity_kWh

    @JsonProperty("ev_battery_charge_start_stopp")
    private boolean evBatteryChargeStartStopp; // ev_battery_charge_start_stopp

    @JsonProperty("ev_batt_max_capacity_kwh")
    private double evBattMaxCapacityKwh;

    @JsonProperty("home_batt_capacity_kwh")
    private double homeBattCapacityKwh;

    @JsonProperty("home_batt_max_capacity_kwh")
    private double homeBattMaxCapacityKwh;

    @JsonProperty("home_batt_min_capacity_kwh")
    private double homeBattMinCapacityKwh;

    @JsonProperty("home_batt_capacity_percent")
    private double homeBattCapacityPercent;

    @JsonProperty("home_battery_mode")
    private String homeBatteryMode;

    @JsonProperty("net_household_load_kwh")
    private double netHouseholdLoadKwh;

    @JsonProperty("solar_production_kwh")
    private double solarProductionKwh;

    @JsonProperty("solar_production_percent")
    private double solarProductionPercent;

    @JsonProperty("solar_production_mode")
    private String solarProductionMode;

    @JsonProperty("solar_max_capacity_kwh")
    private double solarMaxCapacityKwh;

    @JsonProperty("solar_min_capacity_kwh")
    private double solarMinCapacityKwh;

    @JsonProperty("solar_capacity_percent")
    private double solarCapacityPercent;
}
