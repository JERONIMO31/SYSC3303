import drone.LiveDroneTracker;
import utils.StandardizedTime;

import java.net.DatagramSocket;

public class GUISubsystem {

    private DatagramSocket socket;
    //private GUI gui;
    private StandardizedTime standardizedTime;
    private boolean readyToStart = false;
    public static final int GUI_SUBSYSTEM_PORT = 4567;

    public GUISubsystem(int width, int height){
        try {
            this.socket = new DatagramSocket(GUI_SUBSYSTEM_PORT);
            this.socket.setSoTimeout(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
