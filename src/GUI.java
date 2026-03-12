public class GUI {
    private GUISubsystem subsystem;

    public GUI(int width, int height){
        subsystem = new GUISubsystem(this);
    }

    public static void main(String[] args){
        new GUI(600, 400);
        //subsystem.mainLoop();
    }
}
