import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class enables us to have a graphic representation of what the KJunior
 * robot sees with its camera. The camera captures a line 102 pixels wide and
 * returns three arrays of 34 values each : LEFT, MIDDLE, RIGHT pixels values.
 * <p/>
 * This application reads these values from a file and displays the pixels on
 * screen. When it reaches the end of the file, it loops back and restarts
 * endlessly.
 * <p/>
 * <p/>
 * File format :
 * <ul>
 * <li>each group of three lines represents one frame.</li>
 * <li>in each group, the first line holds the 34 values of the LEFT array, the
 * second line the values from the MIDDLE array, the third line the values from
 * the RIGHT array</li>
 * <li>each value is separated by a space</li>
 * <li>the groups are not separated by a blank line, but only appended one after
 * another</li>
 * </ul>
 * <p/>
 * <p>Original author: Lucy Linder<br/>
 * Context: KJunior (TIC) Project, first year of Bachelor of Computer Science, EIA-FR</p>
 *
 * @version 2
 * @since 06.12.2012
 */
public class RobotLinearCamAnalyser extends JPanel {

    private int[] pixels; // the pixels read by the robot and stored in a file
    private String filepath; // the file storing the pixel values
    private int nextLine = 0; // the next line to be read from the file
    private int nbrOfFrames; // the number of frames (images) contained in
    // the file
    private BufferedReader bf;
    private Timer timer; // the timer to update the frames
    private int frequency; // frequency of the frames
    private int pixelHeight = 200, pixelWidth = 12; // dimension of a "pixel"

    private boolean showDelimiters = false;


    /**
     * main : creates a frame, initializes the variables and starts the timer.
     *
     * @param args program arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        /********** checks the arguments *****/

        if (args.length < 1 || args.length > 2) { // not enough arguments ?
            System.err
                    .println("You must specify the path to a file and optionally the frame rate in ms.");
            System.exit(1);
        }

        // Main panel
        RobotLinearCamAnalyser camAnalyserPanel;

        int frequency = 0;
        // The user may optionally specify a frame rate.
        // If this is the case, there is be more than one argument,
        if (args.length > 1) {
            try {
                frequency = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                // second argument not an integer ?
                System.err
                        .println("The second argument must be an integer representing the frame rate in ms.");
                System.exit(1);
            }
        } else {
            // Launch analyzer with default frequency
            try {
                camAnalyserPanel = new RobotLinearCamAnalyser(args[0]);
                frequency = camAnalyserPanel.frequency; // copy frequency set by constructor
            } catch (FileNotFoundException e) {
                System.err.println("the file : " + args[0] + " does not exist...");
                System.exit(1);
            }
        }

        // second argument less or equal to 0 ?
        if (frequency < 1) {
            System.out.println("the frame rate cannot be negative...");
            System.exit(1);
        }

        // Launch analyzer with the frequency given by the user
        try {
            camAnalyserPanel = new RobotLinearCamAnalyser(args[0], frequency);
        } catch (FileNotFoundException e) {
            System.err.println("the file : " + args[0] + " does not exist...");
            System.exit(1);
            return;
        }

        /********* arguments valid, creates the swing frame *****/

        // Use native look (optionally)
        /*
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Is not a big issue, only show a small message in the console.
            System.err.println("Could not load native look and feel. The application will use the (ugly) Metal L&F.");
        }
        */

        JFrame frame = new JFrame("KJunior Robot Linear Camera Analyzer");
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(camAnalyserPanel, BorderLayout.CENTER);
        mainPanel.add(camAnalyserPanel.getButtonPanel(), BorderLayout.SOUTH);

        frame.getContentPane().add(mainPanel);

        frame.setSize(15 + 102 * camAnalyserPanel.pixelWidth + 20, 430);
        frame.setResizable(false);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }// end main


    /**
     * *****************************************************************************
     * ******************************************************************************
     */

    public RobotLinearCamAnalyser(String filepath) throws IOException {
        this(filepath, 40);
    }


    /**
     * @param filepath  the file holding the captures
     * @param frequency the frequency, in milliseconds, at which to show the captures
     *                  (default is 40)
     * @throws IOException
     */
    public RobotLinearCamAnalyser(String filepath, int frequency)
            throws IOException {
        this.setPreferredSize(new Dimension(15 + 102 * this.pixelWidth + 20,
                this.pixelHeight + 120));
        this.pixels = new int[102];
        this.frequency = frequency;
        this.filepath = filepath;

        this.nbrOfFrames = this.getNbrOfLines();
        this.bf = new BufferedReader(new FileReader(filepath));
        this.parseFromFile();

        // timer : reads the 1 line in the file and updates the screen
        this.timer = new Timer(this.frequency, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                try {
                    parseFromFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        this.timer.start();

    }// end constructor


    /**
     * gets the total number of lines in the specified file
     *
     * @return
     * @throws IOException
     */
    public int getNbrOfLines() throws IOException {
        BufferedReader bf = new BufferedReader(new FileReader(filepath));
        int lines = 0;

        while (bf.readLine() != null)
            lines++;

        bf.close();
        return lines;
    }


    /**
     * paints the JPanel : creates a representation of the pixels seen by the
     * robot's camera and writes informations under it. The pixels are stored in
     * an array of ints that is updated by calling the parseFromFile method. The
     * text to draw under the rectangle is stored in the "infos" String.
     */
    @Override
    public void paint(Graphics g) {

        int offsetX = 15, offsetY = 20; // offset for the rectangles/pixels

        // fills the jpanel background
        g.setColor(this.getBackground());
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        StringBuilder builder = new StringBuilder();
        String left = "", middle = "";

        // draws the "pixels"
        for (int i = 0; i < this.pixels.length; i++) {
            g.setColor(new Color(pixels[i], pixels[i], pixels[i]));
            g.fillRect(offsetX, offsetY, pixelWidth, pixelHeight);
            offsetX += pixelWidth;
            // Fill with zeroes so that it looks nice
            builder.append(String.format("%03d  ", pixels[i]));
            // records the pixel values as string
            if (i == 33) {
                left = builder.toString();
                builder = new StringBuilder();
            } else if (i == 67) {
                middle = builder.toString();
                builder = new StringBuilder();
            }

        }

        // draws the pixel values
        g.setColor(Color.black);
        g.drawString("LEFT:      " + left, 20, offsetY + pixelHeight + 55);
        g.drawString("MIDDLE:  " + middle, 20, offsetY + pixelHeight + 75);
        g.drawString("RIGHT:    " + builder.toString(), 20, offsetY
                + pixelHeight + 95);

        // draws the number of frames info
        g.setColor(Color.BLACK);
        g.setFont(getFont().deriveFont(15f).deriveFont(Font.BOLD));
        g.drawString("frame number : " + this.nextLine + " / "
                + (this.nbrOfFrames - 1), 20, offsetY + pixelHeight + 30);

        // draws the delimiters of the left, middle and right pixel zones
        if (this.showDelimiters) {
            g.setColor(Color.RED);
            g.drawLine(15 + 34 * this.pixelWidth, offsetY,
                    15 + (34 * this.pixelWidth), offsetY + this.pixelHeight);
            g.drawLine(15 + 68 * this.pixelWidth, offsetY,
                    15 + (68 * this.pixelWidth), offsetY + this.pixelHeight);
        }
    }// end paint


    /**
     * reads a file to get the pixel values. The pixels must be stored in the
     * following format : the first line is the LEFT pixel values separated by a
     * space (34) the second line is the MIDDLE pixel values the third line is
     * the RIGHT pixel values. It is possible to have any number of "captures",
     * they will be appended one after another
     *
     * @throws IOException
     */
    public void parseFromFile() throws IOException {

        String line;
        int i = 0; // i j for the indexes of the pixels array

        // reads 1 line and stores the pixel values in the pixels array
        line = bf.readLine();

        // if the end of file is reached, starts again
        if (line == null) {
            this.reset();
            line = bf.readLine();
        }

        for (String pixel : line.split(" ")) {
            try {
                pixels[i++] = Integer.parseInt(pixel);
            } catch (NumberFormatException e) {
                System.out.println("parse error: " + pixel);
            }
        }

        // updates the infos String and updates/repaints the jpanel
        nextLine++;
        this.repaint();

    }// end parseFromFile


    /**
     * begins to read the file from the beginning.
     *
     * @throws FileNotFoundException
     */
    public void reset() throws FileNotFoundException {
        this.nextLine = 0;
        try {
            this.bf.close();
        } catch (IOException e) {
            System.out.println("while closing the bufferedreader");
            e.printStackTrace();
        }
        this.bf = new BufferedReader(new FileReader(filepath));
    }


    /**
     * goes to the specified frame. Throws a NumberFormatException if the frame
     * number provided is out of range
     *
     * @param frameNbr
     * @throws IOException
     */
    public void goToFrame(int frameNbr) throws IOException {

        if (frameNbr < 0 || frameNbr > nbrOfFrames) {
            throw new NumberFormatException("frame number out of range");
        }

        reset();

        this.nextLine = frameNbr - 1; // updates lineCount to display

        // reads/skips all the frames before frameNbr
        for (int i = 0; i < frameNbr; i++)
            bf.readLine();
        parseFromFile(); // reads the next unread frame, i.e. frameNbr

    }


    /**
     * gets the panel containing the buttons reset, next, play/pause and the
     * textfield "go to frame"
     *
     * @return
     */
    public JPanel getButtonPanel() {
        final JButton pauseButton, resetButton, previousFrameButton, nextFrameButton, showDelimitersButton;
        JLabel goToLabel;
        final JTextField goToFrame;
        final JSlider frameSlider = new JSlider(JSlider.HORIZONTAL, 1,
                nbrOfFrames - 1, 1);

        final ImageIcon pauseIcon = new ImageIcon("resources/pause.png"), playIcon = new ImageIcon(
                "resources/play.png");

        // implements the play/pause button
        pauseButton = new JButton(pauseIcon);
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JButton but = (JButton) e.getSource();

                if (timer.isRunning()) {
                    timer.stop();
                    but.setIcon(playIcon);
                    frameSlider.setValue(nextLine);
                } else {
                    timer.start();
                    but.setIcon(pauseIcon);
                }
            }
        });

        // reset button : start again from the beginning
        resetButton = new JButton(new ImageIcon("resources/stop.png"));
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                if (timer.isRunning()) { // stops the timer and updates the
                    // play/pause button
                    pauseButton.doClick();
                }

                // goes to the first frame
                try {
                    reset();
                    parseFromFile();
                    frameSlider.setValue(1);
                } catch (IOException e1) {
                    System.out.println("file not found");
                    e1.printStackTrace();
                }

            }
        });

        // previous button
        previousFrameButton = new JButton(new ImageIcon("resources/previous.png"));
        previousFrameButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                // stops the timer
                if (timer.isRunning()) {
                    pauseButton.doClick();
                }

                try { // displays the previous frame
                    if (nextLine == 1) {
                        nextLine = nbrOfFrames - 1;
                        goToFrame(nextLine);
                    } else {
                        goToFrame(--nextLine);
                    }
                    frameSlider.setValue(nextLine); // updates the sliders
                    // value
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    System.out.println("catched :" + e1.getMessage());
                }
            }
        });

        // next button
        nextFrameButton = new JButton(new ImageIcon("resources/next.png"));
        nextFrameButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                // stops the timer
                if (timer.isRunning()) {
                    pauseButton.doClick();
                }

                try { // displays the next frame
                    parseFromFile();
                    frameSlider.setValue(nextLine); // updates the sliders
                    // value
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    System.out.println("catched :" + e1.getMessage());
                }
            }
        });

        // show delimiters button : shows where the left, middle, and right
        // arrays start
        showDelimitersButton = new JButton("show delimiters");
        showDelimitersButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JButton but = (JButton) e.getSource();

                if (showDelimiters) {
                    showDelimiters = false;
                    but.setText("show delimiters");
                } else {
                    showDelimiters = true;
                    but.setText("hide delimiters");
                }

                if (!timer.isRunning())
                    repaint();
            }

        });

        // creates the go to frame textfield
        goToFrame = new JTextField(10);
        goToFrame.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    JTextField source = (JTextField) e.getSource();
                    try {
                        goToFrame(Integer.parseInt(source.getText()));
                        if (timer.isRunning()) // stops the timer
                            pauseButton.doClick();
                        frameSlider.setValue(nextLine);// updates the sliders
                        // value
                    } catch (Exception e2) {
                        source.setText("");
                        System.out.println("catched :" + e2.getMessage());
                    }
                }
            }


            public void keyReleased(KeyEvent e) {
            }


            public void keyTyped(KeyEvent e) {
            }

        });
        goToLabel = new JLabel("go to frame : "); // sets the label
        goToLabel.setLabelFor(goToFrame);

        // configures the jslider created at the beginning of the method
        frameSlider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                // if we don't want the frames to adjust during the slider
                // changes
                // if( source.getValueIsAdjusting() )
                // return;
                if (timer.isRunning()) {
                    pauseButton.doClick();
                }
                try {
                    goToFrame(source.getValue());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        frameSlider.setPreferredSize(new Dimension(getWidth() - 25, 5));

        // adds all the buttons to the jpanel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.add(pauseButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(previousFrameButton);
        buttonPanel.add(nextFrameButton);

        JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 1));
        optionPanel.add(showDelimitersButton);
        optionPanel.add(goToLabel);
        optionPanel.add(goToFrame);

        // creates a container, required in order to have two lines of control :
        // sliders + buttons
        JPanel container = new JPanel(new GridLayout(3, 1));
        container.add(frameSlider); // adds the slider
        container.add(buttonPanel);// adds the button panel
        container.add(optionPanel);
        return container;

    }// end getButtonPanel
    /**
     * reads a file to get the pixel values. The pixels must be stored in the
     * following format : the first line is the LEFT pixel values separated by a
     * space (34) the second line is the MIDDLE pixel values the third line is
     * the RIGHT pixel values. It is possible to have any number of "captures",
     * they will be appended one after another
     *
     * @throws IOException
     *
     *             public void parseFromFile() throws IOException {
     *
     *             BufferedReader bf = new BufferedReader( new FileReader(
     *             filepath ) ); String line; int j = 0;
     *
     *             for( int i = 0; i < lineCount; i++ ) bf.readLine();
     *
     *             System.out.println( "\n line count " + lineCount ); for( int
     *             i = 0; i < 3; i++ ){ line = bf.readLine(); if( line == null
     *             ){ lineCount = 0; return; } for( String pixel : line.split(
     *             " " ) ){ try{ pixels[ i ][ j++ ] = Integer.parseInt( pixel );
     *             }catch( NumberFormatException e ){ System.out.println(
     *             "parse error" ); } } lineCount++; j = 0;
     *
     *             }
     *
     *             infos = "frame number : " + ( lineCount / 3 );
     *             this.repaint(); }// end parseFromFile
     */

}// end class
