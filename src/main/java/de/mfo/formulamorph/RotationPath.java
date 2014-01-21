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

import javax.vecmath.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class RotationPath {

	private static final Quat4d[] q;
	private static final String filename = "quaternions.txt";
	
	static
	{
		try
		{
			// read the quaternions
			URL resURL = RotationPath.class.getResource( filename );
			BufferedReader br = new BufferedReader( new InputStreamReader( resURL.openStream() ) );
			
			String line;
			ArrayList< Quat4d > ql = new ArrayList< Quat4d >( 10000 ); 
			while( ( line = br.readLine() ) != null )
			{
				line = line.trim();
				if( line.isEmpty() )
					continue;
				
			    String[] c = line.split( " +" );
			    double x = Double.parseDouble( c[ 0 ] );
			    double y = Double.parseDouble( c[ 1 ] );
			    double z = Double.parseDouble( c[ 2 ] );
			    double w = Double.parseDouble( c[ 3 ] );
			    
			    Quat4d q_tmp = new Quat4d( x, y, z, w );
			    q_tmp.normalize();
			    if( ql.size() > 0 && new Vector4d( ql.get( ql.size() - 1 ) ).dot( new Vector4d( q_tmp ) ) < 0.0 )
			    	q_tmp.negate();			    	
			    ql.add( q_tmp );
			}			
			q = ql.toArray( new Quat4d[ ql.size() ] );
			/*			
			// resample set in order to make sample points equally spaced
			//does not work
			double[] s = new double[ ql.size() ];
			s[ 0 ] = 0.0;
			for( int i = 1; i < ql.size(); ++i )
				s[ i ] = s[ i - 1 ] + Math.acos( new Vector4d( ql.get( i - 1 ) ).dot( new Vector4d( ql.get( i ) ) ) );
			
			q = new Quat4d[ ql.size() ];
			int j = 0;
			for( int i = 0; i < s.length; ++i )
			{
				double t = s[ s.length - 1 ] * ( i / ( s.length - 1.0 ) );
				while( j < s.length && s[ j ] <= t )
					++j;
				
				// now we have to interpolate the quaternions j-1 and j
				q[ i ] = new Quat4d();
				double ti = 0.0;
				if( j < s.length )
				{
					ti = ( t - s[ j - 1 ] ) / ( s[ j ] - s[ j - 1 ] );
					q[ i ].interpolate( ql.get( j - 1 ), ql.get( j ), ti );
				}
				System.out.println( "t=" + t + " ti=" + ti + " j=" + j + " q[" + i + "]=" + q[ i ] );				
			}
			*/
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
		
		//System.out.println( ( ( int ) ( t * q.length ) ) % q.length );
		
		Quat4d q1 = q[ ( ( int ) ( t * q.length ) ) % q.length ];
		Quat4d q2 = q[ ( ( int ) ( t * q.length + 1 ) ) % q.length ];

		Quat4d q = new Quat4d();
		q.interpolate( q1, q2, t );
		return q;
	}
}
