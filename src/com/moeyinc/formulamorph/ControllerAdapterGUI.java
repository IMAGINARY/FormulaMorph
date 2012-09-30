package com.moeyinc.formulamorph;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.EmptyBorder;

import com.moeyinc.formulamorph.Parameters.*;

import java.util.EnumMap;
import java.util.Hashtable;

public class ControllerAdapterGUI extends JPanel implements Controller, Parameters.ValueChangeListener, Parameters.ActivationStateListener {

	private Controller c;
	
	final static int maxSliderValue = 10000;
	//final static int sliderMajorTicks = 5;
	private EnumMap< Parameters.Parameter, JSlider > p2s = new EnumMap< Parameters.Parameter, JSlider >( Parameters.Parameter.class );
		
	public ControllerAdapterGUI( Controller c )
	{
		if( c == null )
			this.c = new Controller() { // Dummy adapter to ensure that c is not null
				};
		else
			this.c = c;	
		
		this.setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
		this.setBorder( new EmptyBorder( 10, 0, 10, 0 ) );
		Parameter last_param = null;
		for( Parameters.Parameter param : Parameters.Parameter.values() )
		{
			if( last_param == null || last_param.getSurface() != param.getSurface() )
			{
				JSeparator s = new JSeparator( JSeparator.VERTICAL );
				s.setPreferredSize(new Dimension(5,200));
				this.add( s );
			}
			last_param = param;

			final Parameter p = param;
			JPanel slider_panel = new JPanel();
			slider_panel.setLayout( new BoxLayout( slider_panel, BoxLayout.Y_AXIS ) );
			final JSlider s = new JSlider( JSlider.VERTICAL, 0, maxSliderValue, maxSliderValue / 2 );
			s.addChangeListener( new ChangeListener() { public void stateChanged( ChangeEvent e ) { p.setInterpolatedValue( s.getValue() / (double) maxSliderValue ); } } );
			slider_panel.add( s );
			slider_panel.add( new JLabel( p.name() ) );
			s.setMajorTickSpacing( maxSliderValue / 5 );
			s.setMinorTickSpacing( maxSliderValue / 50 );
			s.setPaintTicks(true);
			s.setPaintLabels( true );
			p.addActivationStateListener( new ActivationStateListener() { public void stateChanged( Parameter p ) { s.setEnabled( p.isActive() ); } });
			p.addValueChangeListener( new ValueChangeListener() { public void valueChanged( Parameter p ) { ControllerAdapterGUI.this.valueChanged( p ); } });
			p2s.put( p, s );
			this.add( slider_panel );
		}
		JSeparator s = new JSeparator( JSeparator.VERTICAL );
		s.setPreferredSize(new Dimension(5,200));
		this.add( s );

		
		JPanel buttonPanelLeft = new JPanel();
		buttonPanelLeft.setLayout( new BoxLayout( buttonPanelLeft, BoxLayout.Y_AXIS ) );
		
		JButton screenshotLeft = new JButton( "Screenshot Left" );
		screenshotLeft.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.gui().saveScreenShotLeft(); } } );
		buttonPanelLeft.add( screenshotLeft );
		
		JButton reloadLeft = new JButton( "Reload Left" );
		reloadLeft.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { try { Main.gui().reload( Surface.F ); } catch ( Exception e ) { System.err.println( "Unable to reload left surface." ); e.printStackTrace( System.err ); } } } );
		buttonPanelLeft.add( reloadLeft );

		JButton prevLeft = new JButton( "Previous Left" );
		prevLeft.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Surface.F.setId( Surface.F.getId() - 1 ); } } );
		buttonPanelLeft.add( prevLeft );
		
		JButton nextLeft = new JButton( "Next Left" );
		nextLeft.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Surface.F.setId( Surface.F.getId() + 1 ); } } );
		buttonPanelLeft.add( nextLeft );
		
		this.add( buttonPanelLeft, 0 );

		JPanel buttonPanelRight = new JPanel();
		buttonPanelRight.setLayout( new BoxLayout( buttonPanelRight, BoxLayout.Y_AXIS ) );
		
		JButton screenshotRight = new JButton( "Screenshot Right" );
		screenshotRight.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.gui().saveScreenShotRight(); } } );
		buttonPanelRight.add( screenshotRight );
				
		JButton reloadRight = new JButton( "Reload Right" );
		reloadRight.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { try { Main.gui().reload( Surface.G ); } catch ( Exception e ) { System.err.println( "Unable to reload right surface." ); e.printStackTrace( System.err ); } } } );
		buttonPanelRight.add( reloadRight );

		JButton prevRight = new JButton( "Previous Right" );
		prevRight.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Surface.G.setId( Surface.G.getId() - 1 ); } } );
		buttonPanelRight.add( prevRight );
		
		JButton nextRight = new JButton( "Next Right" );
		nextRight.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Surface.G.setId( Surface.G.getId() + 1 ); } } );
		buttonPanelRight.add( nextRight );

		this.add( buttonPanelRight );

		JSeparator s2 = new JSeparator( JSeparator.VERTICAL );
		s2.setPreferredSize(new Dimension(5,200));
		this.add( s2 );
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.Y_AXIS ) );
		
		JButton pauseAnim = new JButton( "Pause" );
		pauseAnim.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.gui().pauseAnimation(); } } );
		buttonPanel.add( pauseAnim );
		JButton resumeAnim = new JButton( "Resume" );
		resumeAnim.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.gui().resumeAnimation(); } } );
		buttonPanel.add( resumeAnim );
		
		JButton fullscreenOn = new JButton( "Fullscreen ON" ); 
		fullscreenOn.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.gui().tryFullScreen(); } } );
		buttonPanel.add( fullscreenOn );
		JButton fullscreenOff = new JButton( "Fullscreen OFF" );
		fullscreenOff.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.gui().tryWindowed(); } } );
		buttonPanel.add( fullscreenOff );

		
		this.add( buttonPanel );
	}
	
	public void valueChanged( Parameter p )
	{
		Hashtable< Integer, JLabel > labelTable = new Hashtable< Integer, JLabel >();
		labelTable.put( new Integer( 0 ), new JLabel( Double.toString(p.getMin())) );
		labelTable.put( new Integer( maxSliderValue / 2 ), new JLabel( Double.toString((p.getMin()+p.getMax())/2)) );
		labelTable.put( new Integer( maxSliderValue ), new JLabel( Double.toString( p.getMax() ) ) );
		JSlider s = p2s.get( p );
		s.setLabelTable( labelTable );
		s.setValue( (int) ( maxSliderValue * ( p.getValue() - p.getMin() ) / ( p.getMax() - p.getMin() ) ) );
	}

	public void stateChanged( Parameter p )	
	{
		p2s.get( p ).setEnabled( p.isActive() );
	}
	
	public void setVisible( boolean visible )
	{
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		if( device.getFullScreenWindow() == Main.gui() )
		{
			
		}
	}
}
