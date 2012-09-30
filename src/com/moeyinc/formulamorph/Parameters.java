package com.moeyinc.formulamorph;

import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;

import java.util.Map;
import java.util.HashMap;

import javax.vecmath.Color3f;

public interface Parameters {

	public interface ValueChangeListener
	{
		public void valueChanged( Parameter p );
	}
	
	public interface ActivationStateListener
	{
		public void stateChanged( Parameter p );
	}
	
	public interface SurfaceIdListener
	{
		public void idChanged( Surface s );
	}
	
	public enum Surface
	{
		F( 0 ), M(), G( 0 );
		
		private int id;
		private EnumSet< Parameter > parameters;
		private Set< SurfaceIdListener > idListeners = new HashSet< SurfaceIdListener >();
		private Map< Character, Parameter > name2param = new HashMap< Character, Parameter >();
		
		Surface() {}
		Surface( int id ) { this.id = id; }
		
		public int getId() { return id; }
		public void setId( int id ) { this.id = id; this.notifyIdListener(); }
		
		public void addIdListener( SurfaceIdListener sil ) { idListeners.add( sil ); }
		public void removeIdListener( SurfaceIdListener sil ) { idListeners.remove( sil ); }	
		public void notifyIdListener() { for( SurfaceIdListener sil : idListeners ) sil.idChanged( this ); }
		
		public void addParameter( Parameter p )
		{
			if( parameters == null )
				 parameters = EnumSet.noneOf( Parameter.class );
			parameters.add( p );
			name2param.put( p.getName(), p );
		}
		public Parameter getParameter( char name ) { return name2param.get( name ); }
		public EnumSet< Parameter > getParameters()
		{
			if( parameters == null )
				 parameters = EnumSet.noneOf( Parameter.class );
			return parameters.clone();
		}
	};

	public enum Parameter
	{	
		F_a( Surface.F, 'a', new Color3f( 0.0f / 255.0f, 183.0f / 255.0f, 235.0f / 255.0f ) ),
		F_b( Surface.F, 'b', new Color3f( 249.0f / 255.0f, 214.0f / 255.0f, 22.0f / 255.0f ) ),
		F_c( Surface.F, 'c', new Color3f( 219.0f / 255.0f, 40.0f / 255.0f, 165.0f / 255.0f ) ),
		F_d( Surface.F, 'd', new Color3f( 226.0f / 255.0f, 61.0f / 255.0f, 40.0f / 255.0f ) ), 
		F_e( Surface.F, 'e', new Color3f( 247.0f / 255.0f, 127.0f / 255.0f, 0.0f / 255.0f ) ),
		F_f( Surface.F, 'f', new Color3f( 28.0f / 255.0f, 63.0f / 255.0f, 148.0f / 255.0f ) ),
		
		M_t( Surface.M, 't', new Color3f( 101.0f / 255.0f, 105.0f / 255.0f, 109.0f / 255.0f ) ),
		
		G_a( Surface.G, 'a', new Color3f( 0.0f / 255.0f, 183.0f / 255.0f, 235.0f / 255.0f ) ),
		G_b( Surface.G, 'b', new Color3f( 249.0f / 255.0f, 214.0f / 255.0f, 22.0f / 255.0f ) ),
		G_c( Surface.G, 'c', new Color3f( 219.0f / 255.0f, 40.0f / 255.0f, 165.0f / 255.0f ) ),
		G_d( Surface.G, 'd', new Color3f( 226.0f / 255.0f, 61.0f / 255.0f, 40.0f / 255.0f ) ), 
		G_e( Surface.G, 'e', new Color3f( 247.0f / 255.0f, 127.0f / 255.0f, 0.0f / 255.0f ) ),
		G_f( Surface.G, 'f', new Color3f( 28.0f / 255.0f, 63.0f / 255.0f, 148.0f / 255.0f ) );
		
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
		private Set<ValueChangeListener> valueListeners = new HashSet<ValueChangeListener>();
		private Set<ActivationStateListener> stateListeners = new HashSet<ActivationStateListener>();
		
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
		public void setActive( boolean active ) { if( this.active != active ) { this.active = active; for( ActivationStateListener asl : stateListeners ) asl.stateChanged( this ); } }
		
		public double getValue() { return value; }
		public double getMin() { return min; }
		public double getMax() { return max; }
		
		public void setValue( double value ) { if( this.value != value ) { this.value = value; notifyValueChangeListeners(); } }
		public void setMin( double min ) { if( this.min != min ) { this.min = min; notifyValueChangeListeners(); } }
		public void setMax( double max ) { if( this.max != max ) { this.max = max; notifyValueChangeListeners(); } }
		
		public double setInterpolatedValue( double t ) { setValue( min * ( 1.0 - t ) + max * t ); return this.value; }
		
		public void addValueChangeListener( ValueChangeListener vcl ) { valueListeners.add(vcl); }
		public void removeValueChangeListener( ValueChangeListener vcl ) { valueListeners.remove( vcl ); }
		public void notifyValueChangeListeners() { for( ValueChangeListener vcl : valueListeners ) vcl.valueChanged( this ); }

		public void addActivationStateListener( ActivationStateListener asl ) { stateListeners.add(asl); }
		public void removeActivationStateListener( ActivationStateListener asl ) { stateListeners.remove( asl ); }
		public void notifyActivationStateListeners() { for( ActivationStateListener asl : stateListeners ) asl.stateChanged( this ); }
	}
}
