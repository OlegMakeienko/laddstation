#Written by Rikard Ed 2024-01-30

#charging_simulation.py
#Använd simuleringen av en laddstation för en EV som är kopplat till ett hushåll. Den simulerade laddstationen finns som ett skript i Python och startar en webserver som svarar på anrop via JSON-protokollet. Skriv en applikation (i valfritt programmeringsspråk) som hämtar och sänder följande data.
#1.	Hämta information om vilken effekt laddstationen klarar av
#2.	Hämta information om hushållets förbrukning
#3.	Skicka kommando för att starta och stoppa laddningen av EVs batteri. Laddningen skall starta när elpriset är som lägst och hushållets förbrukning inte överstiger 11 kW (trefas-16A) 3.6 kW (enfas 16A) 7.3 kW (enfas 32A) 6.9 kW (trefas 10 A)
#4.	Avläs batteriets kapacitet och ladda batteriet från 20% till 80%
#5. Skapa ett GUI eller använd ett terminalfönster(kommandoprompt) för att kommunicera med den simulerade 

#1 Charging profile without energy price
#2 charging profile with energy price
import json,time
import threading
from flask import Flask, request, jsonify
from flask_cors import CORS

#Energy_price in Öre per kWh incl VAT
energy_price=[85.28,70.86,68.01,67.95,68.01,85.04,87.86,100.26,118.45,116.61,105.93,91.95,90.51,90.34,90.80,88.85,90.39,99.03,87.11,82.9,80.45,76.48,32.00,34.29]

#Residential building
max_power_residential_building=11  # (11 kW = 16A 3 phase)
base_load_residential_percent=[0.08,0.07,0.20,0.18,0.25,0.35,0.41,0.34,0.35,0.40,0.43,0.56,0.42,0.34,0.32,0.33,0.53,1.00,0.81,0.55,0.39,0.24,0.17,0.09]
base_load_residential_kwh=[value * max_power_residential_building for value in base_load_residential_percent]
base_load_residential_kwh = [round(x, 2) for x in base_load_residential_kwh]
#base_load_residential_kWh=[1.6,1.494,1.332,1.275,1.372,1.408,1.588,2.18,2.142,2.73,1.439,1.416,1.14,1.18,1.651,1.968,2.08,1.87,2.77,3.157,2.365,2.854,2.911,1.942]
current_household_load_kwh = base_load_residential_kwh[0] # Byt namn för tydlighet

#Battery (Citroen e_Berlingo M)
ev_batt_nominal_capacity=50 # kWh
ev_batt_max_capacity=46.3   # kWh
ev_batt_capacity_percent=20 #
ev_batt_capacity_kWh=ev_batt_capacity_percent/100*ev_batt_max_capacity
ev_batt_energy_consumption=226 #kWh per km = 2260 per swedish mil
#ev_battery_charge_start_stopp=False
ev_battery_charge_start_stopp=False

#Home Battery (Tesla Powerwall-like system)
home_batt_max_capacity=13.5 # kWh (typical home battery size)
home_batt_min_capacity_percent=10 # % (minimum to prevent damage)
home_batt_capacity_percent=85 # % (start with good charge)
home_batt_capacity_kWh=home_batt_capacity_percent/100*home_batt_max_capacity
home_batt_min_capacity_kWh=home_batt_min_capacity_percent/100*home_batt_max_capacity
home_battery_charge_discharge_mode="idle" # "charging", "discharging", "idle"

#Solar Panel System (Kraftig villa-anläggning)
solar_panel_max_capacity=10.0 # kW (kraftig anläggning för att täcka hela huset)
# Solproduktion per timme (0-23h) - realistisk kurva för svensk sommardag
solar_production_profile_percent=[0.0,0.0,0.0,0.0,0.0,0.05,0.15,0.35,0.55,0.75,0.90,0.95,1.0,0.95,0.90,0.75,0.55,0.35,0.15,0.05,0.0,0.0,0.0,0.0]
solar_production_kwh=[value * solar_panel_max_capacity for value in solar_production_profile_percent]
solar_production_kwh = [round(x, 2) for x in solar_production_kwh]
current_solar_production_kwh = solar_production_kwh[0] # Aktuell solproduktion

#Charging station
charging_station_info= {"Power":"7.4"} #EV version 2 charger
charging_power=7.4 # kW pchmax from car manufacturer

#time
sim_hour=0
sim_min=0
seconds_per_hour=4

# define a lock to synchronize access to the global variable
global_lock = threading.Lock()

app = Flask(__name__)
CORS(app)

def main_prg():
    global sim_hour
    global sim_min
    global ev_battery_charge_start_stopp
    global ev_batt_capacity_percent
    global ev_batt_capacity_kWh
    global ev_batt_max_capacity
    global current_household_load_kwh # Använd det nya namnet
    global seconds_per_hour
    global home_batt_capacity_percent
    global home_batt_capacity_kWh
    global home_batt_max_capacity
    global home_batt_min_capacity_kWh
    global home_battery_charge_discharge_mode
    global current_solar_production_kwh
    global solar_production_kwh
    
    while True:
        current_household_load_kwh = base_load_residential_kwh[sim_hour] # Uppdatera bara hushållets last
        current_solar_production_kwh = solar_production_kwh[sim_hour] # Uppdatera solproduktion
        
        for i in range(0,seconds_per_hour):
            # EV Battery charging logic
            if ev_battery_charge_start_stopp:
                if ev_batt_capacity_percent <110.0:
                    ev_batt_capacity_kWh=ev_batt_capacity_kWh+charging_power/seconds_per_hour
                    ev_batt_capacity_kWh=round(ev_batt_capacity_kWh,2)
                    # Ta bort: base_current_load uppdateras inte med charging_power här
                    ev_batt_capacity_percent=round(ev_batt_capacity_kWh/ev_batt_max_capacity*100,2)
            
            # Home Battery simulation (simplified - can be expanded later)
            # For now, just ensure it stays within safe limits
            if home_batt_capacity_kWh < home_batt_min_capacity_kWh:
                home_battery_charge_discharge_mode = "idle"  # Stop discharging if too low
            
            if home_batt_capacity_kWh > home_batt_max_capacity:
                home_batt_capacity_kWh = home_batt_max_capacity  # Cap at max
            
            home_batt_capacity_percent = round(home_batt_capacity_kWh/home_batt_max_capacity*100,2)
            
            sim_min=int(round((60/seconds_per_hour*i)%60,0))
            time.sleep(1)
        sim_hour=(sim_hour+1)%24
        sim_min=0

#left as a default route    
@app.route('/')
def home():
    global ev_batt_capacity_kWh
    #time.sleep(1)  # wait for 1 second before responding
    #ev_battery_charge_per_cent=ev_battery_charge_per_cent+1
    #if request.method == 'GET':
    return (json.dumps(ev_batt_capacity_kWh))
    #else:
    #    return jsonify({'error': 'Unsupported HTTP method'})

@app.route('/info', methods=['GET'])
def station_info():
    global current_household_load_kwh # Använd det nya namnet
    # Beräkna total last dynamiskt om det behövs för /info, eller returnera separat
    # total_current_load_if_charging = current_household_load_kwh # Removed calculation
    # if ev_battery_charge_start_stopp:
    #     total_current_load_if_charging += charging_power
    #     total_current_load_if_charging = round(total_current_load_if_charging, 2)

    if request.method == 'GET':
        # Byt namn i output för tydlighet, eller behåll "base_current_load" om Java-koden förväntar sig det
        # och se till att Java-koden förstår att det är hushållets last.
        # För nu, returnerar jag hushållets last som "household_load_kwh"
        # och bilens batterinivå som "battery_energy_kwh" för att undvika missförstånd.
        # Java-koden behöver uppdateras för att använda dessa nya nycklar.
        response_data={
            "sim_time_hour": sim_hour,
            "sim_time_min": sim_min,
            "household_load_kwh": current_household_load_kwh, # Detta är bara hushållet
            "battery_energy_kwh": ev_batt_capacity_kWh,    # Detta är bilens batteri kWh
            "ev_battery_charge_start_stopp": ev_battery_charge_start_stopp,
            "ev_batt_max_capacity_kwh": ev_batt_max_capacity,
            "home_batt_capacity_kwh": home_batt_capacity_kWh,
            "home_batt_max_capacity_kwh": home_batt_max_capacity,
            "home_batt_min_capacity_kwh": home_batt_min_capacity_kWh,
            "home_batt_capacity_percent": home_batt_capacity_percent,
            "home_battery_mode": home_battery_charge_discharge_mode,
            "solar_production_kwh": current_solar_production_kwh,
            "solar_max_capacity_kwh": solar_panel_max_capacity,
            "net_household_load_kwh": round(current_household_load_kwh - current_solar_production_kwh, 2)
        }
        return (json.dumps(response_data),{"Access-Control-Allow-Origin":"*"})
    else:
        return jsonify({'error': 'Unsupported HTTP method'})

#deliver base load, starting at 00, 01, 02 a´clock
@app.route('/baseload', methods=['GET'])
def base_load_info():
    if request.method == 'GET':
        return (json.dumps(base_load_residential_kwh))
    else:
        return jsonify({'error': 'Unsupported HTTP method'})

#deliver price per hour, starting at 00-01 a´clock, 01-02 a´clock, 02-03 ...
@app.route('/priceperhour', methods=['GET'])
def price_per_hour_info():
    if request.method == 'GET':
        return (json.dumps(energy_price))
    else:
        return jsonify({'error': 'Unsupported HTTP method'})

#deliver solar production per hour, starting at 00-01 a´clock, 01-02 a´clock, 02-03 ...
@app.route('/solarproduction', methods=['GET'])
def solar_production_info():
    if request.method == 'GET':
        return (json.dumps(solar_production_kwh))
    else:
        return jsonify({'error': 'Unsupported HTTP method'})

@app.route('/charge', methods=['POST', 'GET'])
def charge_battery():
    global ev_battery_charge_start_stopp
    if request.method == 'POST':
        try:
            json_input = request.json
            # result = simulate_charging(json_input)
            # return jsonify(result)
            try:
                start_charg = json_input.get('charging', 0)
                #start_charg = json_input["charging"]
            except json.JSONDecodeError:
                return json.dumps({'error': 'Invalid JSON input'})
            with global_lock:
                if start_charg=="on":
                    ev_battery_charge_start_stopp=True
                    output_data = {'charging': 'on'}
                    return json.dumps(output_data)
                if start_charg=="off":
                    ev_battery_charge_start_stopp=False
                    output_data = {'charging': 'off'}
                    return json.dumps(output_data)

        except Exception as e:
            return jsonify({'error': str(e)})
    elif request.method == 'GET':
        return jsonify(ev_batt_capacity_percent)
        #return jsonify({'message': 'This is a GET request. Use POST to charge the battery.'})
    else:
        return jsonify({'error': 'Unsupported HTTP method'})

@app.route('/discharge', methods=['POST', 'GET'])
def discharge_battery():
    global ev_battery_charge_start_stopp
    global current_household_load_kwh # Använd det nya namnet
    global base_load_residential_kwh
    global ev_batt_nominal_capacity
    global ev_batt_max_capacity
    global ev_batt_capacity_percent
    global ev_batt_capacity_kWh
    global sim_hour
    global sim_min
    global home_batt_capacity_percent
    global home_batt_capacity_kWh
    global home_batt_max_capacity
    global home_battery_charge_discharge_mode

    if request.method == 'POST':
        try:
            json_input = request.json
            # result = simulate_charging(json_input)
            # return jsonify(result)
            try:
                discharg = json_input.get('discharging', 0)
                #start_charg = json_input["discharging"]
            except json.JSONDecodeError:
                return json.dumps({'error': 'Invalid JSON input'})
            with global_lock:
                if discharg=="on":
                    ev_battery_charge_start_stopp=False
                    current_household_load_kwh = base_load_residential_kwh[0] # Återställ hushållets last
                    #Battery (Citroen e_Berlingo M)
                    ev_batt_nominal_capacity=50 # kWh
                    ev_batt_max_capacity=46.3   # kWh
                    ev_batt_capacity_percent=20 #
                    ev_batt_capacity_kWh=ev_batt_capacity_percent/100*ev_batt_max_capacity
                    #Home Battery reset
                    home_batt_capacity_percent=85 # % (reset to good charge)
                    home_batt_capacity_kWh=home_batt_capacity_percent/100*home_batt_max_capacity
                    home_battery_charge_discharge_mode="idle"
                    sim_hour=0
                    sim_min=0
                    output_data = {'discharging': 'on' }
                    return json.dumps(output_data)
                
        except Exception as e:
            return jsonify({'error': str(e)})
    elif request.method == 'GET':
        #return jsonify(ev_batt_capacity_percent)
        return jsonify({'message': 'This is a GET request. Use POST to reset the battery.'})
    else:
        return jsonify({'error': 'Unsupported HTTP method'})
    
# start the increment_sum thread
increment_sum_thread = threading.Thread(target=main_prg)
increment_sum_thread.start()


if __name__ == '__main__':
    app.run(debug=True, port=5001)
