import javax.swing.*;
import java.awt.*;

import static java.lang.System.exit;

/**
 * An EditorPanel is a JPanel with a built-in processing loop.
 * The panel is responsible for driving updates, rendering, and painting.
 * @author Logan Karstetter
 */
public class EditorPanel extends JPanel implements Runnable
{
    /** Default panel width in pixels */
    private static final short DEFAULT_WIDTH = 500;
    /** Default panel height in pixels */
    private static final short DEFAULT_HEIGHT = 500;

    /** The number of nanoseconds in a millisecond */
    private static final int NS_PER_MS = 1000000;
    /** The number of nanoseconds in a second */
    private static final int NS_PER_SEC = 1000000000;

    /** The number of times the thread can skip sleeping before it is forced to sleep */
    private static final byte LOOPS_WITHOUT_SLEEP_BEFORE_YIELD = 16;
    /** The number of times the thread can skip rendering before it is forced to */
    private static final byte FRAMES_SKIPPED_BEFORE_FORCED_RENDER = 5;

    /** The thread that runs the processing loop */
    private Thread processor;
    /** Time allocated for a single loop cycle in nanoseconds */
    private long loopPeriodNs;
    /** Flag for whether the processor thread is running */
    private volatile boolean isRunning;

    /** The Graphics object used to double buffer/render */
    private Graphics dbGraphics;
    /** The image object that is built and rendered each loop */
    private Image dbImage;

    /**
     * Constructor to create an EditorPanel.
     * @param fps Frames per second (0 to 127)
     */
    public EditorPanel(byte fps)
    {
        //Verify fps value before use
        if(fps < 0)
        {
            System.err.println("Error: FPS outside range (0 to 127): " + fps);
            exit(-1);
        }

        //Calculate the allotted loop period
        loopPeriodNs = NS_PER_SEC/fps;

        //Disable double buffering
        setDoubleBuffered(false);

        //Set other panel attributes
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        setFocusable(true);
        requestFocus();
    }

    /**
     * Notifies the EditorPanel that it has been added to a parent
     * component such as a JFrame. This method includes additional
     * logic to start the processor thread. This is important because
     * it prevents the editors processing and rendering from starting
     * before the user can actually see it.
     */
    public void addNotify()
    {
        super.addNotify();
        if(processor == null || !isRunning)
        {
            processor = new Thread(this);
            processor.start();
        }
    }

    /**
     * Stops the editor panel's processing.
     */
    public void stop()
    {
        isRunning = false;
    }

    /**
     * Repeatably update, render, paint and sleep.
     */
    public void run()
    {
        long timeBeforeLoopNs = 0L; //The time measured before the update and render methods are called
        long timeAfterLoopNs  = 0L; //The time measured after the update and render methods are called
        long timeDifferenceNs = 0L; //Amount of time taken to complete methods (timeAfterLoopNs - timeBeforeLoopNs)

        long overtimeNs    = 0L; //The excess time taken to complete the loop (actual - loopPeriod)
        long timeToSleepNs = 0L; //The amount of time remaining in the loop to sleep (loopPeriod - timeDifference) - timeOverslept
        long timeOverslept = 0L; //The amount of time the thread overslept

        byte numSleepSkips    = 0; //The number of times the thread skipped sleeping
        byte numFramesSkipped = 0; //The number of times the thread skipped rendering

        //Capture the time before the first loop starts, start the loop
        timeBeforeLoopNs = System.nanoTime();
        isRunning = true;
        while(isRunning)
        {
            //Update, render, and paint
            //update();
            render();
            paintScreen();

            //Measure time taken to update/render, determine time difference
            timeAfterLoopNs  = System.nanoTime();
            timeDifferenceNs = (timeAfterLoopNs - timeBeforeLoopNs);

            //Calculate the time left to sleep, factor in previous sleep debt, if any
            timeToSleepNs = ((loopPeriodNs - timeDifferenceNs) - timeOverslept);

            //Sleep if time allows
            if(timeToSleepNs > 0L)
            {
                //Convert time to sleep to milliseconds for Thread.sleep()
                try { Thread.sleep(timeToSleepNs/NS_PER_MS); }
                catch (InterruptedException exception) { /*Do nothing*/ }
            }
            else
            {
                //Determine the overtime, timeToSleepNs must be zero or negative, clear timeOverslept
                overtimeNs -= timeToSleepNs;
                timeOverslept = 0L;

                //If we've skipped sleeping too long, force the thread to yield
                if(++numSleepSkips >= LOOPS_WITHOUT_SLEEP_BEFORE_YIELD)
                {
                    Thread.yield();
                    numSleepSkips = 0;
                }
            }

            //Capture the time before the next loop begins
            timeBeforeLoopNs = System.nanoTime();

            //Force updates if rendering is consuming too much time to have UPS ~= FPS
            numFramesSkipped = 0;
            while((overtimeNs > loopPeriodNs) && (numFramesSkipped < FRAMES_SKIPPED_BEFORE_FORCED_RENDER))
            {
                update();
                overtimeNs -= loopPeriodNs;
                numFramesSkipped++;
            }
        }
        //Exit the program
        System.exit(0);
    }

    /**
     * Update
     */
    private void update()
    {

    }

    /**
     * Render the editor using double buffering. If it does not already exist, this
     * method creates an Image the size of the EditorPanel and draws it offscreen.
     * This prevents flickering as the image is constructed and then allows the
     * paintScreen() method to draw the entire screen at once rather than in layers.
     */
    private void render()
    {
        //If the double buffered image is null, create it
        if(dbImage == null)
        {

            //Create the image using the panel's dimensions
            dbImage = createImage(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            if(dbImage == null)
            {
                return;
            }
            else
            {
                //Fetch the image graphics context to enable drawing
                dbGraphics = dbImage.getGraphics();
            }
        }

        //Draw the background and editor
        dbGraphics.setColor(Color.BLACK);
        dbGraphics.fillRect(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);

        //probably call the some other thing here
    }

    /**
     * Actively draw the double buffered image onto the screen.
     */
    private void paintScreen()
    {
        try
        {
            //Retrieve the graphics context
            Graphics graphics = this.getGraphics();

            //Draw the double buffered image
            if(graphics != null && dbImage != null)
            {
                graphics.drawImage(dbImage, 0, 0, null);
            }

            //Sync the toolkit and dispose of the graphics context
            Toolkit.getDefaultToolkit().sync();
            graphics.dispose();
        }
        catch (NullPointerException exception)
        {
            System.out.println("Graphics context error");
            exception.printStackTrace();
        }
    }
}
