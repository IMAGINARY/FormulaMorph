package com.moeyinc.formulamorph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingDeque;

import javax.swing.SwingUtilities;

public class PhidgetInterface implements Parameter.ActivationStateListener
{
	String host;
	int port;
	Socket socket;
	LinkedBlockingDeque< String > outDeque = new LinkedBlockingDeque< String >();
	PhidgetReaderClient phidgetReaderClient;
	PhidgetWriterClient phidgetWriterClient;
	private boolean shutdown = false;
	static int heartbeat_ms = 1000;
		
	public PhidgetInterface( String host, int port )
		throws IOException
	{
		this.host = host;
		this.port = port;
		reconnect();
		phidgetReaderClient = new PhidgetReaderClient();
		phidgetWriterClient = new PhidgetWriterClient();		
		new Thread( phidgetReaderClient ).start();
		new Thread( phidgetWriterClient ).start();
		for( Parameter p : Parameter.values() )
			p.addActivationStateListener( this );
		new Thread( new HeartBeat() ).start();
	}
	
	private synchronized void reconnect()
		throws IOException, UnknownHostException
	{
		if( ! shutdown )
		{
			if( socket != null )
				socket.close();
			socket = new Socket( host, port );
			socket.setKeepAlive( true );
			socket.setSoTimeout( heartbeat_ms * 5 );
		}
	}
	
	public void shutdown() throws IOException
	{
		shutdown = true;
		try { socket.close(); } catch( IOException ioe ) {}
	}
	
	public void stateChanged( Parameter p )
	{
		setLEDEnabled( p, p.isActive() );
	}
	
	public void setLEDEnabled( Parameter p, boolean enabled )
	{
		int LED_id = -1;
		switch( p )
		{
			case F_a: LED_id = 1; break;
			case F_b: LED_id = 2; break;
			case F_c: LED_id = 3; break;
			case F_d: LED_id = 4; break;
			case F_e: LED_id = 5; break;
			case F_f: LED_id = 6; break;
			case M_t: return; // do nothing
			case G_a: LED_id = 7; break;
			case G_b: LED_id = 8; break;
			case G_c: LED_id = 9; break;
			case G_d: LED_id = 10; break;
			case G_e: LED_id = 11; break;
			case G_f: LED_id = 12; break;
		}
		String cmd = "LD," + LED_id + "," + ( enabled ? '1' : '0' );
		boolean done = false;
		do
		{
			try
			{
				outDeque.put( cmd );
				done = true;
			}
			catch( Exception e )
			{
				// retry
			}
		}
		while( !done );
	}

	class PhidgetReaderClient implements Runnable
	{		
		public void run()
		{
			while( !PhidgetInterface.this.shutdown )
			{
				try
				{
					if( !PhidgetInterface.this.socket.isConnected() || PhidgetInterface.this.socket.isClosed() )
						PhidgetInterface.this.reconnect();
					BufferedReader in = new BufferedReader( new InputStreamReader( PhidgetInterface.this.socket.getInputStream() ) );
					String cmd;
					boolean[] digital_switch = { false, false };
					while( ( cmd = in.readLine() ) != null )
					{
						cmd = cmd.replaceAll( "\\s", "" );
						if( cmd.isEmpty() )
							continue; // heart beat
						boolean unknown_command = false;
						try
						{
							String[] parts = cmd.split(",");
							String dev = parts[ 0 ];
							final int id = Integer.parseInt( parts[ 1 ] );
							String[] values = Arrays.copyOfRange( parts, 2, parts.length );

							if( dev.equals( "FS" ) )
							{ // formula selector
								if( id == 1 || id == 2 )
								{
									final int offset = Integer.parseInt( values[ 0 ] );
									final Surface surface = id == 1 ? Surface.F : Surface.G;
									SwingUtilities.invokeLater( new Runnable()
									{
										public void run()
										{
											Main.gui().nextSurface( surface, offset );
										}
									} );
								}
								else
								{
									unknown_command = true;
								}
							}
							else if( dev.equals( "RE" ) )
							{ // rotary encoder
								final int angle = Integer.parseInt( values[ 0 ] );
								
								if( id > 0 && id <= 12 )
								{
									final Parameter[] params = {
										Parameter.F_a, Parameter.F_b, Parameter.F_c, Parameter.F_d, Parameter.F_e, Parameter.F_f,
										Parameter.G_a, Parameter.G_b, Parameter.G_c, Parameter.G_d, Parameter.G_e, Parameter.G_f };
									final Parameter param = params[ id - 1 ];
									SwingUtilities.invokeLater( new Runnable()
									{
										public void run()
										{
											param.setValue( param.getMin() + ( angle / param.getSpeed() ) * ( param.getMax() - param.getMin() ) );
										}
									} );							
								}
								else
								{
									unknown_command = true;
								}
							}
							else if( dev.equals( "JS" ) )
							{ // joystick
								if( id == 1 )
								{
									final double js_value = Double.parseDouble( values[ 0 ] );
									SwingUtilities.invokeLater( new Runnable() { public void run() { Parameter.M_t.setValue( js_value ); } } );
								}
								else
								{
									unknown_command = true;
								}
							}
							else if( dev.equals( "SW" ) )
							{ // digital switch
								boolean on = Integer.parseInt( values[ 0 ] ) == 1;		
								if( !digital_switch[ id + 1 ] && on )
								{ // was off, now is on
									SwingUtilities.invokeLater( new Runnable()
									{
										public void run()
										{
											if( id == 1 )
												Main.gui().saveScreenShotLeft();
											else if( id == 2 )
												Main.gui().saveScreenShotLeft();
										}
									} );
								}
								digital_switch[ id + 1 ] = on;
							}
						}
						catch( ArrayIndexOutOfBoundsException aioobe ) { unknown_command = true; }
						catch( NullPointerException npe ) { unknown_command = true; }
						catch( NumberFormatException nfe ) { unknown_command = true; }
						
						if( unknown_command )
							System.err.println( "PhidgetReader: Unknown command \"" + cmd + "\"" );	
					}				
				}
				catch( UnknownHostException uhe )
				{
					System.err.println( "PhidgetReader: unknown host: " + PhidgetInterface.this.host );
				}	
				catch( IOException ioe )
				{
					System.err.println( "PhidgetReader: no I/O connection to " + PhidgetInterface.this.host + ":" + PhidgetInterface.this.port );
				}
			}	
		}
	}
	
	class PhidgetWriterClient implements Runnable
	{
		public void run()
		{
			while( !PhidgetInterface.this.shutdown )
			{
				try
				{
					if( !PhidgetInterface.this.socket.isConnected() )
						PhidgetInterface.this.reconnect();
					OutputStreamWriter out = new OutputStreamWriter( PhidgetInterface.this.socket.getOutputStream() );
					String cmd = null;
					while( true )
					{
						try
						{
							if( cmd == null )
								cmd = outDeque.take();
							try
							{
								out.write( cmd );
								out.write( '\n' );
								out.flush();
								cmd = null;
							}
							catch( IOException ioe )
							{
								// put cmd back into queue and retry
								outDeque.putFirst( cmd );
								throw ioe;
							}
						}
						catch( InterruptedException ie )
						{
							// retry
						}
					}
					
				}
				catch( UnknownHostException uhe )
				{
					System.err.println( "PhidgetReader: unknown host: " + PhidgetInterface.this.host );
				}	
				catch( IOException ioe )
				{
					System.err.println( "PhidgetReader: no I/O connection to " + PhidgetInterface.this.host + ":" + PhidgetInterface.this.port );
				}
			}	
		}		
	}
	
	class HeartBeat implements Runnable
	{
		public void run()
		{
			while( !PhidgetInterface.this.shutdown )
			{
				try
				{
					Thread.sleep( heartbeat_ms );
					if( outDeque.size() < 10 ) // don't send anything if there is still something in the buffer
						outDeque.put( "" ); // use empty command as heart beat
				}
				catch( Exception ie )
				{
					// just repeat
				}
			}
		}	
	}
}

