package com.moeyinc.formulamorph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Constants {

	public static final int phidget_port;
	public static final String phidget_host;
	
	public static final boolean enable_fullscreen;
	public static final boolean enable_easter_egg;
	public static final float gallery_item_saturation;
	public static final int screensaver_after_seconds;
	public static final int steps_on_rotation_path;
	
	static
	{
		// init the constants
		Properties props = new Properties();
		try { props.load( new FileInputStream( new File( "settings.properties" ) ) ); }
		catch( FileNotFoundException fnfe ) {}
		catch( IOException fnfe ) {}
		
		phidget_port = toInt( props.getProperty( "phidget_port" ), 4767 );
		phidget_host = props.getProperty( "phidget_host", "localhost" );
		enable_fullscreen = toBoolean( props.getProperty( "enable_fullscreen" ), true );
		enable_easter_egg = toBoolean( props.getProperty( "enable_easter_egg" ), true );
		gallery_item_saturation = toFloat( props.getProperty( "gallery_item_saturation" ), 0.0f );
		screensaver_after_seconds = toInt( props.getProperty( "screensaver_after_seconds" ), 10 );
		steps_on_rotation_path = toInt( props.getProperty( "steps_on_rotation_path" ), 1000 );
	}
	
	private static int toInt( String value, int default_value )
	{
		try { default_value = Integer.parseInt( value ); }
		catch( NullPointerException npe ) {}
		catch( NumberFormatException nfe ) { System.err.println( "not an integer: " + value ); }
		return default_value;
	}
	
	private static float toFloat( String value, float default_value )
	{
		try { default_value = Float.parseFloat( value ); }
		catch( NullPointerException npe ) {}
		catch( NumberFormatException nfe ) { System.err.println( "not a double: " + value ); }
		return default_value;
	}
	
	private static boolean toBoolean( String value, boolean default_value )
	{
		try { default_value = Boolean.parseBoolean( value ); }
		catch( NullPointerException npe ) {}
		catch( NumberFormatException nfe ) { System.err.println( "not a boolean: " + value ); }
		return default_value;
	}
}
