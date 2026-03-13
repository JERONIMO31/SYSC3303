import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class GUI extends JFrame {


    private GUISubsystem subsystem;

    public GUI(int width, int height){
        subsystem = new GUISubsystem(this);

        Grid grid = new Grid(10, 10, 10, 10);

        add(grid);

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

    public static void main(String[] args){
        new GUI(600, 400);
        //subsystem.mainLoop();
    }
}
