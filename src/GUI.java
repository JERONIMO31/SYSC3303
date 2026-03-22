import drone.DroneInfo;
import event.EventInfo;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class GUI extends JFrame {


    private static GUISubsystem subsystem;

    private InfoPanel infoPanel;

    private Grid gridPanel;

    public GUI(int width, int height, int cellSize){
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Fire Fighting Drone Simulator GUI");
        subsystem = new GUISubsystem(this);
        //Change to a GridBag Later
        setLayout(new GridLayout(1, 2, 10, 0));
        int[] maxDimensions = subsystem.getMaxDimensions();
        gridPanel = new Grid(maxDimensions[0]/cellSize, maxDimensions[1]/cellSize, cellSize, cellSize);
        infoPanel = new InfoPanel(subsystem.getDroneMap(), subsystem.getEventList());

        add(gridPanel);
        add(infoPanel);

        setMinimumSize(new Dimension(width, height));

        pack();
        setLocationRelativeTo(null);

        new Timer(100,e->{
            updateDronePositions(subsystem.getDroneMap());
        }).start();
    }

    public class Grid extends JPanel {
        /**
         * A JPanel containing a grid of cells, the reference to which are stored in
         * a two-dimensional ArrayList.
         *
         @param gridWidth The number of cells in the width of the grid
         @param gridHeight The number of cells in the height of the grid
         @param cellWidth The pixel width of the cell
         @param cellHeight The pixel height of the cell
         @param cellList A reference to the list of cells you would want to populate
         */

        public int cellWidth;
        public int cellHeight;


        public ArrayList<ArrayList<JPanel>> cellList;

        public Grid(int gridWidth, int gridHeight, int cellWidth, int cellHeight){
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
            setLayout(new GridLayout(gridHeight, gridWidth, 0, 0));
            cellList = new ArrayList<>();
            for (int i = 0; i < gridWidth; i++) {
                ArrayList<JPanel> tempList = new ArrayList<>();
                for (int j = 0; j < gridHeight; j++) {
                    JPanel cell = new JPanel() {
                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(cellWidth, cellHeight);
                        }
                    };
                    cell.setBorder(BorderFactory.createLineBorder(Color.black));
                    tempList.add(cell);
                    add(cell);
                }
                cellList.add(tempList);
                //cellList.add(i, new ArrayList<>());
            }
        }
    }

    public class InfoPanel extends JPanel {
        // Okay what do I want to do
        // I want a list of all the drones for their statuses
        // I guess a list of events (or a list of zones? discuss)
        // Aaaaaaand
        // I think really that's about it
        //I guess maybe I could show the time too?
        private JPanel DronePanel;
        private JPanel EventPanel;
        private JLabel dptitle;
        private JLabel eptitle;
        public InfoPanel(HashMap<String, String> drones, ArrayList<EventInfo> events){
            setLayout(new GridLayout(2, 1));
            DronePanel = new JPanel();
            EventPanel = new JPanel();
            DronePanel.setLayout(new BoxLayout(DronePanel, BoxLayout.Y_AXIS));
            EventPanel.setLayout(new BoxLayout(EventPanel, BoxLayout.Y_AXIS));
            dptitle = new JLabel("Events");
            eptitle = new JLabel("Drones");
            DronePanel.add(dptitle);
            EventPanel.add(eptitle);
            add(new JScrollPane(DronePanel));
            add(new JScrollPane(EventPanel));
        }

        public void updateDronePanel(HashMap<String, String> droneMap){
            DronePanel.removeAll();
            DronePanel.add(dptitle);
            for (String droneId : droneMap.keySet()){
                JPanel droneContainer = new JPanel();
                droneContainer.setLayout(new BoxLayout(droneContainer, BoxLayout.Y_AXIS));
                JLabel droneName = new JLabel("Drone "+droneId);
                JLabel droneLocation = new JLabel("("+droneMap.get(droneId)+")");
                droneContainer.add(droneName);
                droneContainer.add(droneLocation);
                droneContainer.setBorder(BorderFactory.createLineBorder(Color.black));
                DronePanel.add(droneContainer);
            }
            //pack();
        }

        public void updateEventPanel(ArrayList<String[]> events){
            EventPanel.removeAll();
            EventPanel.add(eptitle);
            for (String[] event : events){
                JPanel eventContainer = new JPanel();
                eventContainer.setLayout(new BoxLayout(eventContainer, BoxLayout.Y_AXIS));
                for (String line : event) {
                    if (line != null) {
                        eventContainer.add(new JLabel(line));
                    }
                }
                eventContainer.setBorder(BorderFactory.createLineBorder(Color.black));
                EventPanel.add(eventContainer);
            }
            //pack();
        }
    }

    public void clearGrid(){
        for (ArrayList<JPanel> row : gridPanel.cellList){
            for (JPanel cell : row){
                cell.setBackground(Color.WHITE);
            }
        }
    }

    public void colorCell(int x, int y, Color color){
        if (x< 0 || y < 0 || x >= gridPanel.cellList.size() || y >= gridPanel.cellList.get(0).size()){
            return;
        }
        gridPanel.cellList.get(x).get(y).setBackground(color);
    }

    public void updateDronePositions(HashMap<String, String> droneMap){
        clearGrid();

        for(String droneId : droneMap.keySet()){
            String location = droneMap.get(droneId);

            String[] parts = location.split(",");

            int x = Integer.parseInt((parts[0]));
            int y = Integer.parseInt((parts[1]));


            x = x/gridPanel.cellWidth;
            y = y/gridPanel.cellHeight;

            colorCell(x,y,Color.BLUE);
        }

        repaint();
    }

    public void updateGrid(){

    }

    public void updateDrones(HashMap<String, String> droneMap){
        infoPanel.updateDronePanel(droneMap);
        revalidate();
        repaint();
        //pack();
    }

    public void updateEvent(ArrayList<String[]> events){
        infoPanel.updateEventPanel(events);
        revalidate();
        repaint();
        //pack();
    }

    public static void main(String[] args){
        GUI gui = new GUI(800, 500, 10);
        gui.setVisible(true);
        subsystem.mainLoop();
    }
}
