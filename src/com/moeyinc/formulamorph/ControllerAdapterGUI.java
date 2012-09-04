package com.moeyinc.formulamorph;

import java.awt.*;
import javax.swing.*;

public class ControllerAdapterGUI extends JFrame implements Controller, Parameters {

	private Controller c;
	private Parameters p;
	
	public ControllerAdapterGUI( Controller c, Parameters p )
	{
		super( "Controller GUI" );
		if( c == null )
			this.c = new Controller() { // Dummy adapter to ensure that c is not null
				};
		else
			this.c = c;	
		
		if( p == null )
			this.p = new Parameters() { // Dummy adapter to ensure that p is not null 
				@Override public void setParameterValue(Name name, double value) {}
				@Override public void addActiveParameterListener(ActiveParameterListener apl) {}
				@Override public void removeActiveParameterListener(ActiveParameterListener apl) {}
				@Override public void setSurface(Surface surface, int index_in_gallery) {}
				};
		else
			this.p = p;
		
		Container content = getContentPane();
		content.setLayout( new FlowLayout() );
		for( Name n : Parameters.Name.values() )
		{
			JPanel slider_panel = new JPanel();
			slider_panel.setLayout( new BoxLayout( slider_panel, BoxLayout.Y_AXIS ) );
			slider_panel.add( new JSlider( JSlider.VERTICAL, -1, 1, 0 ) );
			slider_panel.add( new JLabel( n.name() ) );
			content.add( slider_panel );
		}
		pack();
		if( isAlwaysOnTopSupported() )
			setAlwaysOnTop( true );
		setVisible( true );
	}
	
	@Override
	public void setParameterValue(Name name, double value) {
		p.setParameterValue(name, value);		
	}

	@Override
	public void addActiveParameterListener(ActiveParameterListener apl) {
		p.addActiveParameterListener(apl);	
	}

	@Override
	public void removeActiveParameterListener(ActiveParameterListener apl) {
		p.removeActiveParameterListener(apl);
	}

	@Override
	public void setSurface(Surface surface, int index_in_gallery) {
		p.setSurface(surface, index_in_gallery);
	}
}
