package com.moeyinc.formulamorph;

import java.util.Locale;
import java.awt.Dimension;
import javax.swing.JFrame;

public class Main {

	private static GUI gui;
	private static PhidgetInterface pi;
	private static Robot robot = null;
	public static GUI gui() { return gui; }
	public static PhidgetInterface phidgetInterface() { return pi; }
	public static Robot robot() { return robot; }
	
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
    	boolean fullscreen = false;
//    	boolean fullscreen = true;
    	
    	try
    	{
    		gui = new GUI();
    	}
    	catch( Exception e )
    	{
    		System.err.println( "Could not start GUI. Cause:" );
    		e.printStackTrace( System.err );
    		System.exit( -1 );
    	}
        gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if( fullscreen )
        {
        	gui.tryFullScreen();
        }
        else
        {
	        //Size and display the window. (size has no effect in fullscree mode)
			gui.getContentPane().setPreferredSize(new Dimension(16*75,9*75));
			gui.pack();
			gui.setVisible( true );
        }
        String host = System.getProperty( "FMPhidgetHost" , "localhost" );

        int port = 4767;
        String port_string = System.getProperty( "FMPhidgetPort" , "" + port );
        try
        {
        	port = Integer.parseInt( port_string );
        }
        catch( NumberFormatException nfe )
        {
        	System.err.println( "unable to convert FMPhidgetPort=\"" + port_string + "\" to integer port number");
        	System.exit(-1);
        }
       	pi = new PhidgetInterface( host, port );
       	robot = new Robot();
		new Thread( robot ).start();
    }
    
    public static void main(String[] args) {
    	Locale.setDefault( Locale.US );
    	try {
            // Set System L&F
    		javax.swing.UIManager.setLookAndFeel( javax.swing.UIManager.getSystemLookAndFeelClassName());
    	} 
	    catch ( Exception e ) {
	       e.printStackTrace();
	    }

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
