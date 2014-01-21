package de.mfo.formulamorph;

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
