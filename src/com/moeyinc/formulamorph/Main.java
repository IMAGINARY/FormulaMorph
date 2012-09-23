package com.moeyinc.formulamorph;

import java.util.Locale;
import java.awt.Dimension;
import javax.swing.JFrame;

public class Main {

	private static GUI gui;
	public static GUI gui() { return gui; }
	
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
    	boolean fullscreen = false;
//    	boolean fullscreen = true;
    	
		gui = new GUI();
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
    }
    
    public static void main(String[] args) {
    	Locale.setDefault( Locale.US );

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
