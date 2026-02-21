1. FILE DESCRIPTIONS

- FireFightingDroneSimulation.java: Main entry point that creates and 
  displays the simulation GUI window.
- GUI.java: Builds the Swing interface, handles file browsing/start-stop 
  controls, and coordinates simulation threads.
- Drone.java: Drone worker thread that travels to assigned fires, deploys 
  suppression agent, returns home, and refills.
- FireIncident.java: Reads event CSV data, schedules fire reports by 
  simulation time, and publishes fires to the live tracker.
- Scheduler.java: Continuously matches available drones to active fires and 
  updates fire assignment status.
- GridPanel.java: Custom map grid renderer that draws cells, zones, and 
  markers for events/drone positions.
- GridWithLegend.java: Container panel combining the grid and legend into 
  the visualization shown in the GUI.
- LegendPanel.java: Side legend panel listing color-coded map symbols used 
  by the simulation display.
- utils/DroneInfo.java: Thread-safe drone state/model (assignment, location, 
  travel timing, and available agent tracking).
- utils/EndCondition.java: Shared synchronized stop flag used by all threads 
  to end the simulation cleanly.
- utils/EventInfo.java: Fire event model containing location, intensity, 
  event type, assigned drone, and remaining agent required.
- utils/EventType.java: Enum of supported event categories used when parsing 
  event input data.
- utils/Intensity.java: Enum of fire intensity levels (LOW, MODERATE, HIGH).
- utils/LiveDroneTracker.java: Shared tracker managing ready/busy drone sets 
  and lookup of active drone objects.
- utils/LiveFireTracker.java: Shared tracker for queued fires, fires being 
  fought, and extinguished fire lifecycle updates.
- utils/standardizedTime.java: Provides relative simulation time based on 
  real start time for consistent event scheduling/logging.
- utils/Zone.java: Zone model storing boundary coordinates, computed center 
  point, and zone identity metadata.
- utils/ZoneReader.java: CSV parser/loader that reads zone definitions and 
  builds the zone map used by the simulation.


2. SETUP & RUN INSTRUCTIONS

To run the simulation, follow these steps in IntelliJ:

RUN THE APPLICATION: 
   Open 'FireFightingDroneSimulation.java' and click the green Play 
   icon (Run). A window titled "Fire Fighting Drone 
   Simulation" will appear.

LOAD ZONE DATA: 
   In the GUI, click "Browse" next to the "Zone File" bar. 
   Navigate to the 'SYSC3303' folder and select:
   -> sample_zone_file.csv

LOAD EVENT DATA: 
   Click "Browse" next to the "Event File" bar. 
   Navigate to the 'SYSC3303' folder and select:
   -> sample_event_file.csv

START: 
   Click the "Start" button to begin the simulation.

3. TEST DATA REQUIREMENTS

The simulation requires both files to work correctly:
- Zone File: Must contain the coordinates and IDs of the areas 
  the drones monitor.
- Event File: Must contain timestamps, Zone IDs, fire types, and 
  intensity levels.


4. WORK BREAKDOWN (Iteration 2)

Simon D'Amato: Updated README and UML class and sequence diagrams.
Anitsan Robert: State Machine Diagrams and required Unit Testing for Iteration 1
