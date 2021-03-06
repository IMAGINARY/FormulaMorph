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

import java.util.*;
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
	{
		this.host = host;
		this.port = port;
		this.socket = new Socket();
		try { reconnect(); } catch( IOException ioe ) { /* do nothing since PhidgetWriter will try to reconnect broken connections anyway */ }
		phidgetReaderClient = new PhidgetReaderClient();
		phidgetWriterClient = new PhidgetWriterClient();		
		for( Parameter p : Parameter.values() )
			p.addActivationStateListener( this );
		new Thread( new HeartBeat(), "PhidgetHeartBeat" ).start();
		new Thread( phidgetReaderClient, "PhidgetReaderClient" ).start();
		new Thread( phidgetWriterClient, "PhidgetWriterClient" ).start();
	}
	
	private synchronized void reconnect()
		throws IOException, UnknownHostException
	{
		if( ! shutdown )
		{	
			try
			{
				socket.close();
				socket = new Socket( host, port );
				socket.setKeepAlive( true );
				socket.setSoTimeout( heartbeat_ms * 5 );
				if( !socket.isConnected() || socket.isClosed() )
					throw new IOException( "PhidgetInterface: no connection to " + host + ":" + port );
			}
			catch( IOException e )
			{
				// block for 1s to avoid calling this method 
				try { Thread.sleep( 1000 ); } catch( InterruptedException ie ) {}
				throw e;
			}
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
			Integer[] easterEggSequence_tmp = { 1, -1, 1, 1, -1, -1, -1, 1, 1, 1, 1, 1 }; // alternating Fibonacci sequence code (1, -1, 2, -3, 5)
			List< Integer > easterEggSequencePos = new ArrayList< Integer >( Arrays.asList( easterEggSequence_tmp ) );

			// second sequence is the negative version of the first one
			for( int i = 0; i < easterEggSequence_tmp.length; ++i )
				easterEggSequence_tmp[ i ] = -easterEggSequence_tmp[ i ];
			List< Integer > easterEggSequenceNeg = new ArrayList< Integer >( Arrays.asList( easterEggSequence_tmp ) );
			final EnumMap< Surface, LinkedList< Integer > > currentEasterEggSequences = new EnumMap< Surface, LinkedList< Integer > >( Surface.class );
			currentEasterEggSequences.put( Surface.F, new LinkedList< Integer >( Arrays.asList( easterEggSequence_tmp ) ) );
			currentEasterEggSequences.put( Surface.G, new LinkedList< Integer >( Arrays.asList( easterEggSequence_tmp ) ) );
			
			// receive input
			while( !PhidgetInterface.this.shutdown )
			{
				try
				{
					BufferedReader in = new BufferedReader( new InputStreamReader( PhidgetInterface.this.socket.getInputStream() ) );
					String cmd;
					while( ( cmd = in.readLine() ) != null )
					{
						cmd = cmd.replaceAll( "#.*$", "" ); // strip comments (everything from # to the end of the command)
						cmd = cmd.replaceAll( "\\s", "" ); // strip whitespace
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
									
									// insert offset into easter egg sequence
									for( int i = 0; i < Math.abs( offset ); ++i )
									{
										currentEasterEggSequences.get( surface ).poll();
										currentEasterEggSequences.get( surface ).offer( offset > 0 ? +1 : -1 );
                                        //System.out.println(currentEasterEggSequences.toString());
									}
									
									// test if easter egg sequences is complete							
									if( currentEasterEggSequences.get( surface ).equals( easterEggSequencePos ) || currentEasterEggSequences.get( surface ).equals( easterEggSequenceNeg ) )
									{
										// launch easter egg
										SwingUtilities.invokeLater( new Runnable()
										{
											public void run()
											{
												System.out.println( "Easter egg found on " + new Date() );
												Main.gui().selectEasterEggSurface( surface );
											}
										} );
									}
									else
									{
										SwingUtilities.invokeLater( new Runnable()
										{
											public void run()
											{
												Main.gui().nextSurface( surface, offset );
											}
										} );
									}
								}
								else
								{
									unknown_command = true;
								}
							}
							else if( dev.equals( "RE" ) )
							{ // rotary encoder
								final int relative_angle = Integer.parseInt( values[ 0 ] );
								
								if( id > 0 && id <= 12 )
								{
									final Parameter[] params = {
										Parameter.F_a, Parameter.F_b, Parameter.F_c, Parameter.F_d, Parameter.F_e, Parameter.F_f,
										Parameter.G_a, Parameter.G_b, Parameter.G_c, Parameter.G_d, Parameter.G_e, Parameter.G_f };
									final Parameter param = params[ id - 1 ];
									if( param.isActive() )
									{
										SwingUtilities.invokeLater( new Runnable()
										{
											public void run()
											{
												param.setValue( param.getValue() + ( relative_angle / param.getSpeed() ) * param.getRange() );
											}
										} );
									}
								}
								else
								{
									unknown_command = true;
								}
							}
							else if( dev.equals( "JW" ) )
							{ // jog wheel
								final int relative_angle = Integer.parseInt( values[ 0 ] );
								
								if( id > 0 && id <= 1 )
								{
									SwingUtilities.invokeLater( new Runnable()
									{
										public void run()
										{
											Main.gui().stepPath( relative_angle );
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
								if( id == 1 || id == 2 )
								{
									boolean on = Integer.parseInt( values[ 0 ] ) == 1;		
									if( on )
									{
										System.out.println( "SW in" );
										SwingUtilities.invokeLater( new Runnable()
										{
											public void run()
											{
												if( id == 1 )
													Main.gui().saveScreenShot( Surface.F );
												else if( id == 2 )
													Main.gui().saveScreenShot( Surface.G );
											}
										} );
										System.out.println( "SW out" );
									}
								}
								else
								{
									unknown_command = true;
								}
                            }
                            else
                            {
                                unknown_command = true;
                            }
						}
						catch( ArrayIndexOutOfBoundsException aioobe ) { unknown_command = true; }
						catch( NullPointerException npe ) { unknown_command = true; }
						catch( NumberFormatException nfe ) { unknown_command = true; }
						
						if( unknown_command )
							;//System.err.println( "PhidgetReader: Unknown command \"" + cmd + "\"" );
                        else
                            Main.robot().holdBack();
					}
				}
				catch( IOException ioe )
				{
					System.err.println( "PhidgetReader: no I/O connection to " + PhidgetInterface.this.host + ":" + PhidgetInterface.this.port );
					try { Thread.sleep( 1000 ); } catch( InterruptedException ie ) {}
				}
			}	
		}
	}
	
	class PhidgetWriterClient implements Runnable
	{
		public void run()
		{
			boolean reconnect = false;
			while( !PhidgetInterface.this.shutdown )
			{
				try
				{
					if( !PhidgetInterface.this.socket.isConnected() || PhidgetInterface.this.socket.isClosed() || reconnect )
					{
						PhidgetInterface.this.reconnect();
						reconnect = false;
					}
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
								out.write( "#FM\n" );
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
				catch( IOException ioe )
				{
					System.err.println( "PhidgetWriter: no I/O connection to " + PhidgetInterface.this.host + ":" + PhidgetInterface.this.port );
					reconnect = true;
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

