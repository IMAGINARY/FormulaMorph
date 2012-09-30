package com.moeyinc.formulamorph;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum Surface
{
	F, M, G;
	
	private EnumSet< Parameter > parameters;
	private Map< Character, Parameter > name2param = new HashMap< Character, Parameter >();
	
/*
	Surface() {}
	Surface( int id ) { this.id = id; }

	public static interface IdListener
	{
		public void idChanged( Surface s );
	}
	
	private int id;
	private Set< Surface.IdListener > idListeners = new HashSet< Surface.IdListener >();
	
	public int getId() { return id; }
	public void setId( int id ) { this.id = id; this.notifyIdListener(); }
	
	public void addIdListener( Surface.IdListener sil ) { idListeners.add( sil ); }
	public void removeIdListener( Surface.IdListener sil ) { idListeners.remove( sil ); }	
	public void notifyIdListener() { for( Surface.IdListener sil : idListeners ) sil.idChanged( this ); }
*/	
	
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
}