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
	
	public static final boolean enable_momath_api;
	public static final String momath_api_host;
	public static final String momath_api_token;
	public static final String momath_api_exhibit_id;
	public static final String momath_api_location_id_left;
	public static final String momath_api_location_id_right;
	
	public static final boolean enable_user_verification;
	public static final int user_verification_timeout;
	public static final int verification_confirmation_timeout;

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
		
		enable_momath_api = toBoolean( props.getProperty( "enable_momath_api" ), true );
		momath_api_host = props.getProperty( "momath_api_host", "api.momath.org" );
		momath_api_token = props.getProperty( "momath_api_token", "" );
		momath_api_exhibit_id = props.getProperty( "momath_api_exhibit_id", "FOMO.OD" );
		momath_api_location_id_left = props.getProperty( "momath_api_location_id_left", "OD.15" );
		momath_api_location_id_right = props.getProperty( "momath_api_location_id_right", "OD.15" );
		
		enable_user_verification = toBoolean( props.getProperty( "enable_user_verification" ), true );
		user_verification_timeout = toInt( props.getProperty( "user_verification_timeout" ), 8 );
		verification_confirmation_timeout = toInt( props.getProperty( "verification_confirmation_timeout" ), 3 );
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
