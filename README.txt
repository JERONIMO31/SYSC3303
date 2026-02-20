1 FILE DESCRIPTIONS
- FireFightingDroneSimulation.java: The main entry point that launches 
  the Graphical User Interface (GUI).
- GUI.java: Handles user inputs, file selection, and displays 
  real-time status messages from the threads.
- Drone.java: Represents the drone thread that travels to fires, 
  deploys fire-suppression agents, and refills.
- FireIncident.java: Processes fire events from the CSV files and 
  reports them to the tracker at scheduled times.
- Scheduler.java: Monitors active fires and assigns ready drones to 
  the locations that need them.
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

3.TEST DATA REQUIREMENTS
The simulation requires both files to work correctly:
- Zone File: Must contain the coordinates and IDs of the areas 
  the drones monitor.
- Event File: Must contain timestamps, Zone IDs, fire types, and 
  intensity levels.
