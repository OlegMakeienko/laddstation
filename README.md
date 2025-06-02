# Electric Vehicle (V2H) Charging Optimization

Systemarkitektur: Off-Grid Stuga
Energikällor:
☀️ Solpaneler (10 kW)
🔋 Husbatteri (13.5 kWh)
🚗 Elbilens batteri (46.3 kWh) - som backup
Energiflöden att simulera:
Sol → Hus (direkt förbrukning)
Sol → Husbatteri (lagring)
Sol → Elbil (laddning när överskott)
Husbatteri → Hus (när sol inte räcker)
Elbil → Hus (V2H när nödsituation)
Hus ← Elbil (normal laddning från husbatteri/sol)
🤔 Logik att Fundera På
Prioriteringsordning:
Först: Solel direkt till huset
Sen: Överskott till husbatteri (om < 90%)
Sen: Överskott till elbil (om husbatteri fullt)
Nöd: Elbil → Hus (om husbatteri < 20% OCH elbil > 40%)
Säkerhetsgränser:
🏠 Husbatteri: Min 10%, Max 100%
🚗 Elbil: Min 40% (för körning), Max 100%
⚠️ Kritisk situation: Husbatteri < 15% → Använd elbil
Optimeringsstrategier:
Sommar: Sol → Hus → Husbatteri → Elbil
Vinter: Spara elbilens energi, ladda bara vid solöverskott
Molnigt: Prioritera husbatteri, undvik elbilladdning

---

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [System Requirements](#system-requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Server Integration](#server-integration)
- [UI Mockup](#ui-mockup)

---

## Overview
The project includes:
- **Optimal Hour Identification:** Finds hours when charging is cost-effective and does not exceed a total load of 11 kW.
- **Battery Management:** Discharges the battery to 20% and charges it to 80% during optimal hours.
- **Real-Time Updates:** Fetches hourly data for prices and base load from a server.
- **Simulation-Driven:** Integrates with a Python-based simulation server.

---

## Features
- Automatically identifies and waits for the best hours to charge the EV.
- Integrates with a server to retrieve hourly power consumption and electricity price data.
- Logs detailed information about the charging process in the console.
- Adheres to the power load limit of 11 kW.

---

## System Requirements
- **Java:** Version 11 or higher
- **Python:** Version 3.8 or higher
- **Dependencies:**
    - Java libraries: `RestTemplate`, `ObjectMapper`
    - Python libraries: `Flask` (for server)

---

## Installation
1. Clone the repository:
    ```bash
    git clone https://github.com/your-repo/ev-charging-optimizer.git
    cd ev-charging-optimizer
    ```
2. Compile the Java project:
    ```bash
    javac -d bin src/*.java
    ```
3. Start the Python server:
    ```bash
    python3 ChargingWebserver_v0.8.py
    ```

---

## Usage
1. Run the Java application:
    ```bash
    java -cp bin Main
    ```
2. Monitor the console for updates on:
    - Current battery status
    - Optimal charging hours
    - Charging/discharging progress

---

## Server Integration
The project integrates with a Python-based server, `ChargingWebserver_v0.8.py`, located in the root directory. This server provides the necessary data for the system:
- **Endpoint 1:** `/priceperhour` — Returns hourly electricity prices.
- **Endpoint 2:** `/baseload` — Returns the household's hourly base load.
- **Endpoint 3:** `/info` — Returns current simulation time and battery status.

Make sure the Python server is running before starting the Java application.

---

## UI Mockup
The project's user interface and design considerations are outlined in the following Figma board:
- **Figma Mockup:** [EV Charging Station Design](https://www.figma.com/board/y69YSBfZzxf308kUPjqUwj/Laddstation?node-id=0-1&node-type=canvas&t=4jHEFDFmGLF5fFTU-0)

This provides a visual representation of the expected functionality and flow.

---

## Contributing
Feel free to open issues or submit pull requests to improve the project.

---

## License
This project is licensed under the MIT License. See the `LICENSE` file for details.

