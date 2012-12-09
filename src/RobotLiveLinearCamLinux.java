import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.TimerTask;

/**
 * This programs reads lines of 102 integer values separated by spaces matching 
 * the grayscale levels output from the robot.
 */
public class RobotLiveLinearCamLinux extends JPanel {
    private String[] pixels = new String[102];

    private Runnable readInputStreamRunnable = new Runnable() {

        @Override
        public void run() {
            // Read standard input and update image on screen
            // Every line of input contains 102 values separated by spaces
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    RobotLiveLinearCamLinux.this.pixels = line.split(" ");
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                // Redraw the pixels every time a line is read
                                RobotLiveLinearCamLinux.this.repaint();
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    public static void main(String[] args) throws IOException {

        JFrame frame = new JFrame("Schrödi's Live Linear Camera 0.0.1");
        frame.setSize(15+102*15+20, 260);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        RobotLiveLinearCamLinux liveCamPanel = new RobotLiveLinearCamLinux();
        frame.getContentPane().add(liveCamPanel);

        // May not be the best solution, but does the trick: wait 1 second
        // before showing the first picture. It should be enough time for the
        // UI to load.
        liveCamPanel.delayStart(1);
        frame.setVisible(true);
    }

    // Wait some time before starting the thread reading standard input
    private void delayStart(int seconds) {
        new java.util.Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                new Thread(readInputStreamRunnable).start();
            }
        }, seconds * 1000);
    }

    public void paint(Graphics g) {
        int offsetX = 15, offsetY = 20, height = 200, width = 15;
        g.setColor(Color.white);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        for (String strPixel : pixels) {
            int intPixel = Integer.parseInt(strPixel);
            g.setColor(new Color(intPixel, intPixel, intPixel));
            g.fillRect(offsetX, offsetY, width, height);
            offsetX += width;
        }

        g.setColor(Color.BLACK);
    }

}
