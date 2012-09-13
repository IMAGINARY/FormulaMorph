package com.moeyinc.formulamorph;

import java.awt.Dimension;

import javax.swing.JFrame;

public class Main {

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
    	boolean fullscreen = false;
//    	boolean fullscreen = true;
    	
		GUI f = new GUI();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if( fullscreen )
        {
        	f.tryFullScreen();
        }
        else
        {
	        //Size and display the window. (size has no effect in fullscree mode)
			f.getContentPane().setPreferredSize(new Dimension(16*75,9*75));
			f.pack();
			f.setVisible( true );
        }
        
    }
    
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
