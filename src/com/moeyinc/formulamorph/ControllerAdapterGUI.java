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
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.Y_AXIS ) );
		
		JButton screenshotleft = new JButton( "Screenshot Left" );
		screenshotleft.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.gui().saveScreenShotLeft(); } } );
		buttonPanel.add( screenshotleft );
		JButton screenshotright = new JButton( "Screenshot Right" );
		screenshotright.addActionListener( new ActionListener() { public void actionPerformed( ActionEvent ae ) { Main.gui().saveScreenShotRight(); } } );
		buttonPanel.add( screenshotright );
		
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
