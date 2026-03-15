FILE DESCRIPTIONS

DroneSubsystemGUI.java: Swing-based GUI used to configure and start the drone subsystem. Allows the user to specify drone parameters such as speed, acceleration, and agent capacity, and displays subsystem logs.

FireIncidentGUI.java: GUI interface used to start the fire incident subsystem. Allows the user to load zone and event CSV files and start the simulation that generates fire incidents.

SchedulerGUI.java: GUI interface used to configure and run the scheduler subsystem. Allows the user to specify the simulation time scale and displays scheduler messages.

GUISubsystem.java: Networking subsystem responsible for communicating between the GUI and other subsystems (Scheduler and FireIncident) using UDP messages. Handles initialization messages and parses zone data received from FireIncident.

Scheduler.java: Core scheduling subsystem that coordinates drone assignments to fire incidents and manages simulation time.

FireIncident.java: Reads event data from input files and generates fire incidents during the simulation timeline.

drone/LiveDroneTracker.java: Tracks all drones in the system, including available and assigned drones, and provides lookup and assignment functionality.

utils/DroneInfo.java: Represents the state of an individual drone including its location, assignment status, and operational timing.

utils/EventInfo.java: Model representing a fire event including its location, type, intensity, and timestamp.

utils/EventType.java: Enumeration representing different types of fire-related events that can occur in the simulation.

utils/Intensity.java: Enumeration defining fire intensity levels such as LOW, MODERATE, and HIGH.

utils/LiveFireTracker.java: Maintains queues of active fires, fires currently being fought, and extinguished fires during the simulation.

utils/StandardizedTime.java: Provides a standardized simulation time system. Converts real-world time into simulation time using a configurable time scale.

zones/Zone.java: Represents a geographic monitoring zone defined by rectangular coordinate boundaries.

zones/ZoneReader.java: Reads and parses zone definitions from a CSV file and builds a map of zone objects used in the simulation.

udp/Message.java: Encapsulates UDP communication messages exchanged between subsystems. Handles serialization and deserialization of message data.

udp/MessageType.java: Enumeration defining all supported message types used in subsystem communication (INIT, NEW_INCIDENT, FIRE_EXTINGUISHED, ASSIGNMENT, AGENT_DEPLOYED).

SETUP & RUN INSTRUCTIONS

To run the system, follow these steps in IntelliJ:

RUN THE SUBSYSTEM GUIs:
Each subsystem can be started through its GUI class.

Run SchedulerGUI.java

Run FireIncidentGUI.java

Run DroneSubsystemGUI.java

Each subsystem will open its own window and wait for initialization messages from the other subsystems.

LOAD ZONE DATA:
In the FireIncident GUI, click "Browse" next to the Zone File field and select the zone CSV file.

LOAD EVENT DATA:
Click "Browse" next to the Event File field and select the event CSV file containing fire incidents.

CONFIGURE PARAMETERS:
In the DroneSubsystem GUI, configure drone parameters such as:

Total number of drones

Agent capacity

Speed

Acceleration

Deploy rate

Nozzle open time

START THE SIMULATION:
Press the "Start" button in each subsystem GUI. The subsystems will connect via UDP and begin exchanging messages to run the simulation.

TEST DATA REQUIREMENTS

The simulation requires the following input files:

Zone File:
A CSV file containing zone IDs and coordinate boundaries used to define monitoring regions.

Event File:
A CSV file containing timestamps, zone IDs, event types, and intensity levels used to generate fire incidents during the simulation.

SYSTEM COMMUNICATION

The subsystems communicate using UDP sockets and the Message / MessageType classes.
Initialization messages are exchanged when the subsystems start to ensure that all components are connected before the simulation begins.

WORK BREAKDOWN:
Simon D'Amato: I3 Coding
Nicolaus Derikx: I3 Coding 
Anitsan Robert: UML Class Diagram and Updated README.txt
Jeronimo Cumming: Unit Testing
