package com.moeyinc.formulamorph;

import javax.vecmath.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class RotationPath {

	private static final Quat4d[] q;
	private static final String filename = "quaternions.txt";
	
	static
	{
		try
		{
			URL resURL = RotationPath.class.getResource( filename );
			BufferedReader br = new BufferedReader( new InputStreamReader( resURL.openStream() ) );
			
			String line;
			ArrayList< Quat4d > ql = new ArrayList< Quat4d >( 10000 ); 
			while( ( line = br.readLine() ) != null )
			{
			    String[] c = line.split( " " );
			    double x = Double.parseDouble( c[ 0 ] );
			    double y = Double.parseDouble( c[ 1 ] );
			    double z = Double.parseDouble( c[ 2 ] );
			    double w = Double.parseDouble( c[ 3 ] );
			    ql.add( new Quat4d( x, y, z, w ) );
			}
			q = ql.toArray( new Quat4d[ ql.size() ] );
		}
		catch( Exception e )
		{
			throw new RuntimeException( "Error reading " + filename, e );
		}
	}

	public static Quat4d at( double t )
	{
		double t_floor = Math.floor( t );
		t = t - t_floor;
		while( t < 0 )
			t = t + 1.0;
		// now t\in[0,1]
		
		Quat4d q1 = q[ ( ( int ) ( t * q.length ) ) % q.length ];
		Quat4d q2 = q[ ( ( int ) ( t * q.length + 1 ) ) % q.length ];

		Quat4d q = new Quat4d();
		q.interpolate( q1, q2, t );
		return q;
	}
}
