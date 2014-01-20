package com.moeyinc.formulamorph;

import java.util.HashSet;
import java.util.Set;

import javax.vecmath.Color3f;

public enum Parameter
{	
	F_a( Surface.F, 'a', new Color3f( 0.0f / 255.0f, 173.0f / 255.0f, 239.0f / 255.0f ) ),
	F_b( Surface.F, 'b', new Color3f( 255.0f / 255.0f, 221.0f / 255.0f, 0.0f / 255.0f ) ),
	F_c( Surface.F, 'c', new Color3f( 200.0f / 255.0f, 89.0f / 255.0f, 161.0f / 255.0f ) ),
	F_d( Surface.F, 'd', new Color3f( 241.0f / 255.0f, 92.0f / 255.0f, 34.0f / 255.0f ) ), 
	F_e( Surface.F, 'e', new Color3f( 237.0f / 255.0f, 31.0f / 255.0f, 36.0f / 255.0f ) ),
	F_f( Surface.F, 'f', new Color3f( 104.0f / 255.0f, 8.0f / 255.0f, 220.0f / 255.0f ) ),
	
	M_t( Surface.M, 't', new Color3f( 101.0f / 255.0f, 105.0f / 255.0f, 109.0f / 255.0f ) ),
	
	G_a( Surface.G, 'a', new Color3f( 0.0f / 255.0f, 173.0f / 255.0f, 239.0f / 255.0f ) ),
	G_b( Surface.G, 'b', new Color3f( 255.0f / 255.0f, 221.0f / 255.0f, 0.0f / 255.0f ) ),
	G_c( Surface.G, 'c', new Color3f( 200.0f / 255.0f, 89.0f / 255.0f, 161.0f / 255.0f ) ),
	G_d( Surface.G, 'd', new Color3f( 241.0f / 255.0f, 92.0f / 255.0f, 34.0f / 255.0f ) ), 
	G_e( Surface.G, 'e', new Color3f( 237.0f / 255.0f, 31.0f / 255.0f, 36.0f / 255.0f ) ),
	G_f( Surface.G, 'f', new Color3f( 104.0f / 255.0f, 8.0f / 255.0f, 220.0f / 255.0f ) );
	
	public static interface ValueChangeListener
	{
		public void valueChanged( Parameter p );
	}
	public static interface ActivationStateListener
	{
		public void stateChanged( Parameter p );
	}
	static {
		for( Parameter p : Parameter.values() )
			p.surface.addParameter( p );
	}
	
	private final Surface surface;
	private final char name;
	private final String latexColorDefinition;
	private final String latexColorName;
	private boolean active;
	private double value;
	private double min;
	private double max;
	private double speed = 360.0;
	private Set<Parameter.ValueChangeListener> valueListeners = new HashSet<Parameter.ValueChangeListener>();
	private Set<Parameter.ActivationStateListener> stateListeners = new HashSet<Parameter.ActivationStateListener>();
	
	Parameter( Surface s, char name, Color3f c )
	{
			this.name = name;
			this.active = false;
			this.surface = s;
			this.latexColorName = "FMPC" + this.surface.name() + this.name;
			this.latexColorDefinition = "\\definecolor{" + this.latexColorName + "}{rgb}{" + c.x + "," + c.y + "," + c.z +"}";
	}
	
	public Surface getSurface() { return surface; }
	public char getName() { return name; }
	public String getLaTeXColorDefinition() { return latexColorDefinition; }
	public String getLaTeXColorName() { return latexColorName; }
	
	public boolean isActive() { return active; }
	public void setActive( boolean active ) { if( this.active != active ) { this.active = active; for( Parameter.ActivationStateListener asl : stateListeners ) asl.stateChanged( this ); } }
	
	public double getValue() { return value; }
	public double getRange() { return max - min; }
	public double getMin() { return min; }
	public double getMax() { return max; }
	public double getSpeed() { return speed; }
	
	public void setValue( double value )
	{
		value = value < min ? min : ( value > max ? max : value );
		if( this.value != value )
		{
			this.value = value;
			notifyValueChangeListeners();
		}
	}
	public void setMin( double min ) { if( this.min != min ) { this.min = min; notifyValueChangeListeners(); } }
	public void setMax( double max ) { if( this.max != max ) { this.max = max; notifyValueChangeListeners(); } }
	public void setSpeed( double speed ) { if( this.speed != speed ) { this.speed = speed; notifyValueChangeListeners(); } }
	
	public double setInterpolatedValue( double t ) { setValue( min * ( 1.0 - t ) + max * t ); return this.value; }
	
	public void addValueChangeListener( Parameter.ValueChangeListener vcl ) { valueListeners.add(vcl); }
	public void removeValueChangeListener( Parameter.ValueChangeListener vcl ) { valueListeners.remove( vcl ); }
	public void notifyValueChangeListeners() { for( Parameter.ValueChangeListener vcl : valueListeners ) vcl.valueChanged( this ); }

	public void addActivationStateListener( Parameter.ActivationStateListener asl ) { stateListeners.add(asl); }
	public void removeActivationStateListener( Parameter.ActivationStateListener asl ) { stateListeners.remove( asl ); }
	public void notifyActivationStateListeners() { for( Parameter.ActivationStateListener asl : stateListeners ) asl.stateChanged( this ); }
}
