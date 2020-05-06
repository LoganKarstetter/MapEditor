import javax.swing.*;

/**
 * The main file responsible for launching the MapEditor.
 * This is the entry point for the program.
 * @author Logan Karstetter
 **/
public class Main
{
    /** The desired frames per second to run at */
    private static final byte FPS = 60;

    /**
     * Main function.
     * @param args Not supported.
     */
    public static void main(String[] args)
    {
        //Create a JFrame and EditorPanel
        JFrame frame = new JFrame("Map Editor");
        EditorPanel editorPanel = new EditorPanel(FPS);

        //Add the panel to the frame
        frame.getContentPane().add(editorPanel);

        //Set frame attributes
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(true);
        frame.setVisible(true);
        frame.pack();

        //Center the frame on the screen
        frame.setLocationRelativeTo(null);
    }
}
