import javax.swing.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.TimerTask;

public class RobotLiveLinearCamWin32 extends JPanel {
    int[] pixels = new int[102]; // pixels representing a frame
    String filepath;
    
    private Runnable readInputStreamRunnable = new Runnable() {
        
        @Override
        public void run() {
            
            BufferedReader bf = null;
            
            try{
                bf = new BufferedReader(
                        new FileReader(
                                filepath ) );
            }catch( FileNotFoundException e1 ){
                // TODO Auto-generated catch block
                e1.printStackTrace();
                System.exit( 1 );
            }
            
            int pixelsIndex = 0; // index for managing the pixels array
            int currentToken; // the token currently read (int, one character)
            String digitBuffer = ""; // used in order to get ints from 1 or more
                                     // chars
            
            while( true ){
                try{
                    currentToken = bf.read(); // gets the next char from the
                                              // file
                    if( currentToken == -1 ){ // if the end of the file is
                                              // reached, loops again until new
                                              // input
                    }else if( currentToken == '\r' ){
                    }else if( currentToken == '\n' ){ // if the end of a line is
                                                      // reached
                        // displays the new frame on the screen
                        SwingUtilities.invokeAndWait( new Runnable() {
                            @Override
                            public void run() {
                                RobotLiveLinearCamWin32.this.repaint();
                            }
                        } );
                        pixelsIndex = 0; // begins a new line of pixels
                    }else if( currentToken == ' ' ){
                        // try converting the tokens appearing before the space
                        // in integer
                        pixels[ pixelsIndex ] = Integer.parseInt( digitBuffer );
                        pixelsIndex++; // if no exception occurred, prepares for
                                       // the next pixel
                        digitBuffer = "";
                        
                    }else{
                        digitBuffer += (char) currentToken; // adds the char to
                                                            // the buffer string
                        // this string will be converted into integer when the
                        // next space is met
                    }
                }catch( IOException | NumberFormatException | InvocationTargetException
                        | InterruptedException e ){
                    // if an exception occurs, loops again
                    e.printStackTrace();
                    continue;
                    
                }
            }
        }
    };
    
    
    public static void main( String[] args ) throws IOException {
        
        JFrame frame = new JFrame( "Schrödi's Live Linear Camera 0.0.1" );
        frame.setSize( 15 + 102 * 15 + 20, 260 );
        frame.setResizable( false );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        
        RobotLiveLinearCamWin32 liveCamPanel = new RobotLiveLinearCamWin32(args[0]);

        
        frame.getContentPane().add( liveCamPanel );        
        liveCamPanel.delayStart( 1 ); // starts the thread 1 second after swing
                                      // init
        frame.setVisible( true );
    }
    
    public RobotLiveLinearCamWin32(String filepath){
        this.filepath = filepath;
    }
    
    /**
     * adds a delay of [seconds] seconds before starting the thread responsible
     * for reading the file. It is important in order to be sure the swing
     * components are loaded.
     * 
     * @param seconds
     */
    private void delayStart( int seconds ) {
        new java.util.Timer().schedule( new TimerTask() {
            @Override
            public void run() {
                new Thread( readInputStreamRunnable ).start();
            }
        }, seconds * 1000 );
    }
    
    
    /**
     * paint method for the JPanel. Updates the screen with the last available
     * frame, i.e. the last 102 pixels fully read by the running thread
     */
    @Override
    public void paint( Graphics g ) {
        int offsetX = 15, offsetY = 20, height = 200, width = 15;
        g.setColor( Color.white );
        g.fillRect( 0, 0, this.getWidth(), this.getHeight() );
        
        for( int intPixel : pixels ){
            g.setColor( new Color( intPixel, intPixel, intPixel ) );
            g.fillRect( offsetX, offsetY, width, height );
            offsetX += width;
        }
        
        g.setColor( Color.BLACK );
    }
    
}