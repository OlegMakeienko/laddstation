# Electric Vehicle (EV) Charging Station Optimization

This project is a Java-based system for optimizing the charging of electric vehicles (EVs). The system ensures that the battery is charged during optimal hours based on electricity prices and household power consumption while adhering to the constraints of total power load capacity.

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

