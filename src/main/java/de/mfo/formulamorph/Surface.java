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
