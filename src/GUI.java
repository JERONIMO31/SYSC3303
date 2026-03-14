import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class GUI extends JFrame {


    private static GUISubsystem subsystem;

    public GUI(int width, int height){
        subsystem = new GUISubsystem(this);
        setLayout( new GridLayout(2, 1, 10, 0));
        Grid grid = new Grid(10, 10, 10, 10);
        InfoPanel info = new InfoPanel();

        add(grid);
        add(info);

        pack();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public class Grid extends JPanel {
        /**
         * A JPanel containing a grid of cells, the reference to which are stored in
         * a two-dimensional ArrayList
         *
         @param gridWidth The number of cells in the width of the grid
         @param gridHeight The number of cells in the height of the grid
         @param cellWidth The pixel width of the cell
         @param cellHeight The pixel height of the cell
         @param cellList A reference to the list of cells you would want to populate
         */
        public ArrayList<ArrayList<JPanel>> cellList;
        public Grid(int gridWidth, int gridHeight, int cellWidth, int cellHeight){
            setLayout(new GridLayout(gridWidth, gridHeight, 0, 0));
            cellList = new ArrayList<ArrayList<JPanel>>();
            for (int i = 0; i < gridWidth; i++) {
                ArrayList<JPanel> tempList = new ArrayList<>();
                for (int j = 0; j < gridHeight; j++) {
                    JPanel cell = new JPanel() {
                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(cellWidth, cellHeight);
                        }
                    };
                    tempList.add(cell);
                }
                cellList.add(i, new ArrayList<>());
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
        public InfoPanel(){
            setLayout(new GridLayout(1, 3));

        }
    }

    public void updateGrid(){

    }

    public static void main(String[] args){
        GUI gui = new GUI(600, 400);
        gui.setVisible(true);
        subsystem.mainLoop();
    }
}
