import javax.swing.*;
import java.awt.*;
/**
 * LegendPanel is a custom Swing panel that displays a legend for the Fire Simulation Map.
 *
 * Features:
 * - Vertical layout of legend items using BoxLayout.
 * - Each legend item shows a colored box and a descriptive label.
 * - Supports custom text inside the color box for labeling purposes.
 * - Fixed preferred width to fit alongside the main grid panel.
 *
 * Usage:
 * LegendPanel legend = new LegendPanel();
 * legend.addLegendItem(Color.RED, "", "Active Fire");
 * legend.addLegendItem(Color.GREEN, "", "Extinguished Fire");
 * 
 * See also:
 * GridPanel
 * GridWithLegend
 */
class LegendPanel extends JPanel {
    /**
        * Constructs a LegendPanel with vertical layout and titled border.
        * Sets the preferred width to 200 pixels.
     */
    public LegendPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Legend"));
        setPreferredSize(new Dimension(200, 0)); 
    }

    /**
     * Adds a legend item to the panel.
     *
     * Responsibilities:
     * - Create a colored box with optional label.
     * - Display a descriptive text next to the color box.
     * - Add the legend item to the vertical layout.
     *
     * @param color background color of the box
     * @param l text to display inside the color box (can be empty)
     * @param text descriptive label for the legend item
    */
    public void addLegendItem(Color color, String l, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JPanel colorBox = new JPanel(new GridBagLayout());
        JLabel boxLabel = new JLabel(l);
        colorBox.add(boxLabel);
        colorBox.setBackground(color);
        colorBox.setPreferredSize(new Dimension(30, 30));
        colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JLabel label = new JLabel(text);

        item.add(colorBox);
        item.add(label);
        add(item);
    }
}