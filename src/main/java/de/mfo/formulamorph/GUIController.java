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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.EmptyBorder;

import java.util.EnumMap;
import java.util.Hashtable;
import java.util.Formatter;

public class GUIController extends JPanel implements Parameter.ValueChangeListener, Parameter.ActivationStateListener {
	
	final static int maxSliderValue = 10000;
	//final static int sliderMajorTicks = 5;
	private EnumMap< Parameter, JSlider > p2s = new EnumMap< Parameter, JSlider >( Parameter.class );
		
	public GUIController()
	{
		this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
		this.setBorder( new EmptyBorder( 10, 0, 10, 0 ) );
		
		JPanel panelForAllSliders = new JPanel();
		Parameter last_param = null;
		for( Parameter param : Parameter.values() )
		{
			if( last_param != null && last_param.getSurface() != param.getSurface() )
			{
				JSeparator s = new JSeparator( JSeparator.VERTICAL );
				s.setPreferredSize(new Dimension(5,200));
				panelForAllSliders.add( s );
			}
			last_param = param;

			final Parameter p = param;
			JPanel slider_panel = new JPanel();
			slider_panel.setLayout( new BoxLayout( slider_panel, BoxLayout.Y_AXIS ) );
			final JSlider s = new JSlider( JSlider.VERTICAL, 0, maxSliderValue, maxSliderValue / 2 );
			s.addChangeListener( new ChangeListener() { public void stateChanged( ChangeEvent e ) { Main.robot().holdBack(); p.setInterpolatedValue( s.getValue() / (double) maxSliderValue ); } } );
			slider_panel.add( s );
			slider_panel.add( new JLabel( p.name() ) );
			s.setMajorTickSpacing( maxSliderValue / 5 );
			s.setMinorTickSpacing( maxSliderValue / 50 );
			s.setPaintTicks(true);
			s.setPaintLabels( true );
			p.addActivationStateListener( this );
			p.addValueChangeListener( this );
			p2s.put( p, s );
			panelForAllSliders.add( slider_panel );
		}

		this.add( panelForAllSliders, 0 );		
		
		JPanel lrButtonPanel = new JPanel();
		lrButtonPanel.setLayout( new BoxLayout( lrButtonPanel, BoxLayout.X_AXIS ) );
		
		String[] levels = { Gallery.Level.BASIC.name(), Gallery.Level.INTERMEDIATE.name(), Gallery.Level.ADVANCED.name() };
		JComboBox levelsLeft = new JComboBox( levels );
		levelsLeft.setMaximumSize( levelsLeft.getPreferredSize() );
		levelsLeft.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent e) { Main.robot().holdBack(); Main.gui().setLevel( Surface.F, Gallery.Level.valueOf( ( String ) ( ( JComboBox ) e.getSource() ).getSelectedItem() ) ); } } );
		lrButtonPanel.add( levelsLeft );
		
		JButton screenshotLeft = new JButton( "Screenshot Left" );
		screenshotLeft.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.robot().holdBack(); Main.gui().saveScreenShot( Surface.F ); } } );
		lrButtonPanel.add( screenshotLeft );
		
		JButton reloadLeft = new JButton( "Reload Left" );
		reloadLeft.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.robot().holdBack(); try { Main.gui().reload( Surface.F ); } catch ( Exception e ) { System.err.println( "Unable to reload left surface." ); e.printStackTrace( System.err ); } } } );
		lrButtonPanel.add( reloadLeft );

		JButton prevLeft = new JButton( "Previous Left" );
		prevLeft.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.robot().holdBack(); Main.gui().previousSurface( Surface.F ); } } );
		lrButtonPanel.add( prevLeft );
		
		JButton nextLeft = new JButton( "Next Left" );
		nextLeft.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.robot().holdBack(); Main.gui().nextSurface( Surface.F ); } } );
		lrButtonPanel.add( nextLeft );		
		
		lrButtonPanel.add( Box.createHorizontalGlue() );
		
		JButton prevRight = new JButton( "Previous Right" );
		prevRight.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.robot().holdBack(); Main.gui().previousSurface( Surface.G ); } } );
		lrButtonPanel.add( prevRight );

		JButton nextRight = new JButton( "Next Right" );
		nextRight.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.robot().holdBack(); Main.gui().nextSurface( Surface.G ); } } );
		lrButtonPanel.add( nextRight );
		
		JButton reloadRight = new JButton( "Reload Right" );
		reloadRight.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.robot().holdBack(); try { Main.gui().reload( Surface.G ); } catch ( Exception e ) { System.err.println( "Unable to reload right surface." ); e.printStackTrace( System.err ); } } } );
		lrButtonPanel.add( reloadRight );

		JButton screenshotRight = new JButton( "Screenshot Right" );
		screenshotRight.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.robot().holdBack(); Main.gui().saveScreenShot( Surface.G ); } } );
		lrButtonPanel.add( screenshotRight );

		JComboBox levelsRight = new JComboBox( levels );
		levelsRight.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent e) { Main.robot().holdBack(); Main.gui().setLevel( Surface.G, Gallery.Level.valueOf( ( String ) ( ( JComboBox ) e.getSource() ).getSelectedItem() ) ); } } );
		lrButtonPanel.add( levelsRight );				
		
		this.add( lrButtonPanel );
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.X_AXIS ) );
		
		JButton pauseAnim = new JButton( "Pause" );
		pauseAnim.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.robot().holdBack(); Main.gui().pauseAnimation(); } } );
		buttonPanel.add( pauseAnim );
		JButton resumeAnim = new JButton( "Resume" );
		resumeAnim.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.robot().holdBack(); Main.gui().resumeAnimation(); } } );
		buttonPanel.add( resumeAnim );
		
		JButton fullscreenOn = new JButton( "Fullscreen ON" ); 
		fullscreenOn.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.robot().holdBack(); Main.gui().tryFullScreen(); } } );
		buttonPanel.add( fullscreenOn );
		JButton fullscreenOff = new JButton( "Fullscreen OFF" );
		fullscreenOff.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.robot().holdBack(); Main.gui().tryWindowed(); } } );
		buttonPanel.add( fullscreenOff );

		
		this.add( buttonPanel );
	}
	
	public void valueChanged( final Parameter p )
	{
		SwingUtilities.invokeLater( new Runnable() 
		{
			public void run()
			{
				Hashtable< Integer, JLabel > labelTable = new Hashtable< Integer, JLabel >();
				labelTable.put( new Integer( 0 ), new JLabel( String.format( "%.2f", Double.valueOf( p.getMin()))) );
				labelTable.put( new Integer( maxSliderValue / 2 ), new JLabel( String.format( "%.2f", Double.valueOf((p.getMin()+p.getMax())/2))) );
				labelTable.put( new Integer( maxSliderValue ), new JLabel( String.format( "%.2f", Double.valueOf( p.getMax() ) ) ) );
				JSlider s = p2s.get( p );
				s.setLabelTable( labelTable );
				ChangeListener[] cls = s.getChangeListeners();
				for( ChangeListener cl : cls )
					s.removeChangeListener( cl );
				s.setValue( (int) ( maxSliderValue * ( p.getValue() - p.getMin() ) / ( p.getMax() - p.getMin() ) ) );
				for( ChangeListener cl : cls )
					s.addChangeListener( cl );				
			}
		});
	}

	public void stateChanged( final Parameter p )	
	{
		SwingUtilities.invokeLater( new Runnable() { public void run() { p2s.get( p ).setEnabled( p.isActive() ); } } );
	}
}
