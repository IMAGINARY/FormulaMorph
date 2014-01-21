/*
 *    Copyright 2012 Christian Stussak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.mfo.formulamorph;

import java.util.Locale;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

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
        
        //Size and display the window. (size has no effect in fullscree mode)
		gui.getContentPane().setPreferredSize(new Dimension(16*75,9*75));
		gui.pack();
		gui.setVisible( true );
        
       	pi = new PhidgetInterface( Constants.phidget_host, Constants.phidget_port );
       	robot = new Robot();
		new Thread( robot ).start();

        if( Constants.enable_fullscreen )
        	SwingUtilities.invokeLater( new Runnable() { public void run() { gui.tryFullScreen(); } } );
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
