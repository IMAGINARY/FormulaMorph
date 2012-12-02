package com.moeyinc.formulamorph;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.*;
import java.net.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;

import java.util.*;
import java.util.regex.*;

public class UserVerification extends JPanel
{		
	public enum LocationID
	{
		LEFT( Constants.momath_api_location_id_left ), RIGHT( Constants.momath_api_location_id_right );
		
		public final String name;
		LocationID( String name ) { this.name = name; }
		public String toString() { return name; }
	}
	
	public interface ActionListener
	{
		public void visitorSelected( Visitor v );
		public void canceled();
	}
		
	public static class TagData
	{
		public String type;
		public String symbols;
		public TagData( String type, String symbols ) { this.type = type; this.symbols = symbols; }
	}
	
	public class Tag
	{
		private TagData td;
		private final ImageScaler image;
		
		public Tag( Visitor v )
		{
			this.td = new TagData( null, null );
			this.image = new ImageScaler( loadingTagImage );
			this.loadTagInfoAndImageInBackground( v );
		}
		
		public Tag( String type, String symbols )
		{
			this.td = new TagData( type, symbols );
			this.image = new ImageScaler( loadingTagImage );
			this.loadTagImageInBackground();
		}
		
		private void loadTagInfoAndImageInBackground( final Visitor v )
		{
			new Thread( new Runnable() {
				public void run() {
					td = UserVerification.getTagData( v.getID() );
					loadTagImageInBackground();
				}
			}).start();
		}
		
		private void loadTagImageInBackground()
		{
			if( td.symbols != null )
			{		
				try
				{
					URL tagURL = new URL( "http", host, "/api/v1/content/tag-symbol/" + td.symbols + ( td.type != null ? "/" + td.type : "" ) + "?tok=" + token );
					new BackgroundImageLoader( this.image, tagURL, errorTagImage, null );
				}
				catch( MalformedURLException murle )
				{
					this.image.setImage( errorTagImage );
				}
			}
			else
			{
				this.image.setImage( errorTagImage );
			}
		}
		
		public String getType() { return td.type; }
		public String getSymbols() { return td.symbols; }
		public ImageScaler getImage() { return image; }
	}
	
	public class Visitor
	{
		private final String id;
		private final LocationID locationID;
		private final Tag tag;
		private final ImageScaler avatar;
		
		public Visitor( String id, LocationID locationID )
		{
			this.id = id;
			this.locationID = locationID;
			this.tag = new Tag( this );
			
			this.avatar = new ImageScaler( loadingAvatarImage );
			try
			{
				URL avatarURL = new URL( "http", host, "/api/v1/content.svc/avatar/" + id + "?tok=" + token );
				new BackgroundImageLoader( this.avatar, avatarURL, errorAvatarImage, null );
			}
			catch( MalformedURLException murle )
			{
				avatar.setImage( errorAvatarImage );
			}
		}
		
		public String getID() { return id; }
		public LocationID getLocationID() { return locationID; }
		public Tag getTag() { return tag; }
		public ImageScaler getAvatar() { return avatar; }
		
	}
	
	private static Object lockForRWResourceImage = new Object();
	private  static BufferedImage loadResourceImage( URL resource )
	{
		synchronized( lockForRWResourceImage )
		{
			try { return ImageIO.read( resource ); } catch( IOException ioe ) { ioe.printStackTrace(); return null; }
		}
	}

	private class BackgroundImageLoader implements Runnable
	{
		private ImageScaler is;
		private URL urlToLoad;
		private Image errorImage;
		private Runnable callWhenFinished;
		
		public BackgroundImageLoader( ImageScaler is, URL urlToLoad, Image errorImage, Runnable callWhenFinished )
		{
			this.is = is;
			this.urlToLoad = urlToLoad;
			this.errorImage = errorImage;
			this.callWhenFinished = callWhenFinished;
			
			new Thread( this ).start();
		}

		@Override
		public void run()
		{
			final Image i = loadResourceImage( urlToLoad );
			SwingUtilities.invokeLater( new Runnable()
			{
				public void run() {
					is.setImage( i == null ? errorImage : i ); if( callWhenFinished != null ) callWhenFinished.run();
				}
			} );
		}
	}
	
	static final String host = Constants.momath_api_host;
	static final String exhibitID = Constants.momath_api_exhibit_id;
	static final String token = Constants.momath_api_token;
	
	private static final BufferedImage headerImage = loadResourceImage( UserVerification.class.getResource( "UserVerificationHEADER.png" ) );
	private static final BufferedImage loadingTagImage = loadResourceImage( UserVerification.class.getResource( "UserVerificationTAGLOADING.png" ) );
	private static final BufferedImage errorTagImage = loadResourceImage( UserVerification.class.getResource( "UserVerificationMISSINGTAG.png" ) );
	private static final BufferedImage loadingAvatarImage = loadResourceImage( UserVerification.class.getResource( "blank.png" ) );
	private static final BufferedImage errorAvatarImage = loadResourceImage( UserVerification.class.getResource( "blank.png" ) );
	private static final BufferedImage cancelVerificationImage = loadResourceImage( UserVerification.class.getResource( "UserVerificationCANCEL.png" ) );

	
	private JPanel fixedSizeContentPanel = new JPanel();
	
	private Visitor[] visitors;
	private Button[] buttons;
	private int selected = 0;
	
	private abstract class Button extends JPanel
	{
		public Runnable action;
		
		public Button( Runnable action )
		{
			this.action = action;
			this.setBackground( Color.WHITE );
			this.setSelected( false );
			this.setLayout( null );
			Insets i = getInsets();
			Dimension d = new Dimension( i.left + i.right + 60 + 1 + 180 + 2, i.top + i.bottom + 60 );
			this.setMinimumSize( d );
			this.setPreferredSize( d );
			this.setMaximumSize( d );
		}
		
		public void setSelected( boolean selected )
		{
			Color color = selected ? Color.BLACK : Color.LIGHT_GRAY;
			this.setBorder( BorderFactory.createMatteBorder( 0, 6, 0, 6, color ) );
			this.repaint();
		}
	}
	
	private class VisitorButton extends Button
	{
		public Visitor visitor;
		public VisitorButton( Visitor v, Runnable r )
		{
			super( r );
			visitor = v;
			Insets i = getInsets();
			
			this.add( visitor.avatar );
			visitor.avatar.setBounds( i.left + 1, i.top, 60, 60 );
			
			this.add( visitor.getTag().image );
			visitor.getTag().image.setBounds( i.left + 1 + 60 + 1, i.top, 180, 60 );
		}
	}
	
	private class CancelButton extends Button
	{
		public CancelButton( Runnable r )
		{
			super( r );
			
			this.setLayout( new BorderLayout() );
			this.add( new JLabel( new ImageIcon( cancelVerificationImage ) ), BorderLayout.CENTER );
		}
	}
	
	private final ActionListener actionListener;
	private LocationID locationID;
	
	public UserVerification( LocationID locationID, ActionListener actionListener )
	{	
		this.locationID = locationID;
		this.actionListener = actionListener;
		this.addComponentListener( new ComponentAdapter() { public void componentResized( ComponentEvent e ) { repaint(); } } );
		this.addKeyListener( new KeyAdapter() {
			public void keyPressed(KeyEvent e)
			{
				if( e.getKeyCode() == KeyEvent.VK_DOWN )
					selectNext();
				else if( e.getKeyCode() == KeyEvent.VK_UP )
					selectPrevious();
				else if( e.getKeyCode() == KeyEvent.VK_ENTER )
					confirm();
			}
		} );
		
		visitors = getCurrentVisitors( LocationID.LEFT );					
		setContent();
		this.setFocusable( true );
		requestFocus();
	}
	
	private void setContent()
	{	
		this.setLayout( null );
		JPanel fscp = fixedSizeContentPanel;
		fscp.setBackground( Color.WHITE );
		this.add( fscp );
		
		fscp.removeAll();
		fscp.setLayout( null );
		ImageScaler header = new ImageScaler( headerImage );
		fscp.add( header );
		header.setBounds( 0, 0, headerImage.getWidth(), headerImage.getHeight() );
		
		int vgap = 22;
		int i;
		buttons = new Button[ visitors.length + 1 ];
		for( i = 0; i < visitors.length; ++i )
		{
			
			final Visitor v = visitors[ i ];
			final JPanel p = buttons[ i ];
			
			buttons[ i ] = new VisitorButton( v, new Runnable(){ public void run() { actionListener.visitorSelected( v ); } } );
			
			fscp.add( buttons[ i ] );
			Dimension d = buttons[ i ].getPreferredSize();
			buttons[ i ].setBounds( ( headerImage.getWidth() - d.width ) / 2, headerImage.getHeight() + vgap + i * ( d.height + 1 ), d.width, d.height );
		}
		
		buttons[ i ] = new CancelButton( new Runnable(){ public void run() { actionListener.canceled(); } } );
		fscp.add( buttons[ i ] );
		Dimension d = buttons[ i ].getPreferredSize();
		buttons[ i ].setBounds( ( headerImage.getWidth() - d.width ) / 2, headerImage.getHeight() + vgap + i * ( d.height + 1 ), d.width, d.height );
		i++;
		
		buttons[ selected ].setSelected( true );
		fscp.setBounds( 0, 0, headerImage.getWidth(), headerImage.getHeight() + 2 * vgap + ( visitors.length + 1 ) * ( d.height + 1 ) );
		this.setPreferredSize( fscp.getSize() );
	}
	
	private void select( int n )
	{
		this.selected = Math.min( Math.max( 0, n ), this.buttons.length - 1 );
		for( int i = 0; i < this.buttons.length; ++i )
			this.buttons[ i ].setSelected( i == this.selected );
	}
	
	public void selectNext()
	{
		select( this.selected + 1 );
	}
	
	public void selectPrevious()
	{
		select( this.selected - 1 );
	}
	
	public void selectOffset( int offset )
	{
		select( this.selected + offset );
	}
	
	public void confirm()
	{
		SwingUtilities.invokeLater( this.buttons[ this.selected ].action );
	}
	
	public void paintComponent(Graphics g)
	{
		if( this.getWidth() == fixedSizeContentPanel.getWidth() && this.getHeight() == fixedSizeContentPanel.getHeight() )
		{
			fixedSizeContentPanel.setVisible( true );
			super.paintComponent( g );
		}
		else
		{
			fixedSizeContentPanel.setVisible( false );
			BufferedImage image = new BufferedImage( fixedSizeContentPanel.getWidth(), fixedSizeContentPanel.getHeight(), BufferedImage.TYPE_INT_RGB );
	        Graphics2D graphics2D = image.createGraphics();
	        fixedSizeContentPanel.paint( graphics2D );
	        Image scaled = image.getScaledInstance( this.getWidth(), this.getHeight(), Image.SCALE_SMOOTH );
	        g.drawImage( scaled, 0, 0, scaled.getWidth( null ), scaled.getHeight( null ), null );
	        repaint( 250 );
		}
	}
	
    private static String HTTP_GET( URL url )
    	throws MalformedURLException, ProtocolException, IOException
    {
	    HttpURLConnection connection = null;
	    StringBuilder sb = new StringBuilder(); 
	  
	    try {
	        //Set up the initial connection
	        connection = ( HttpURLConnection ) url.openConnection();
	        connection.setRequestMethod("GET");
	        connection.setReadTimeout(10000);                  
	        connection.connect();
	      
	        //read the result from the server
	        BufferedReader rd  = new BufferedReader(new InputStreamReader(connection.getInputStream()));  
	        String line;
	        while ((line = rd.readLine()) != null)
	            sb.append(line + '\n');        
	    }
	    finally
	    {
	    	if( connection != null )
	    		connection.disconnect();
	    }
	    return sb.toString();
    }
    
    private static String HTTP_POST( URL url, InputStream is )
        	throws MalformedURLException, ProtocolException, IOException
        {
    	    HttpURLConnection connection = null;
    	    StringBuilder sb = new StringBuilder(); 
    	  
    	    try {
    	        //Set up the initial connection
    	        connection = ( HttpURLConnection ) url.openConnection();
    	        connection.setRequestMethod("POST");
    	        connection.setDoOutput( true );
    	        connection.setReadTimeout(10000);                  
    	        connection.connect();
    	      
    	        // write the data to the server
    	        OutputStream os = connection.getOutputStream();
    	        byte[] buf = new byte[ 1024 * 10 ];
    	        int len;
    	        while( ( len = is.read( buf ) ) != -1 )
    	        	os.write( buf, 0, len );
    	        os.close();

    	        //read the result from the server
    	        BufferedReader rd  = new BufferedReader(new InputStreamReader(connection.getInputStream()));  
    	        String line;
    	        while ((line = rd.readLine()) != null)
    	            sb.append(line + '\n');        
    	    }
    	    finally
    	    {
    	    	if( connection != null )
    	    		connection.disconnect();
    	    }
    	    return sb.toString();
        }
    
    private static String[] getCurrentVisitorIDs( LocationID locID )
    {
    	String xml = "";
    	try
    	{
    		xml = HTTP_GET( new URL( "http", host, "/api/v1/status/current-status/" + locID + "?tok=" + token ) );
    	}
    	catch( Exception e )
    	{
    		e.printStackTrace();
    	}
    	xml = xml.replaceAll( "\n" , "" ).replaceAll( "\r", "" );
    	
    	String patternString = "<\\s*Visit[^>]+ID=\"([^\"]*)\"";
    	Pattern pattern = Pattern.compile(patternString);
    	Matcher matcher = pattern.matcher( xml );

    	LinkedList< String > idList = new LinkedList< String >();
    	idList.add( "05704f6f-03d0-4107-a04d-dbb7fe1ad333" );
    	while( matcher.find() && idList.size() < 10 )
    		idList.add( matcher.group( 1 ) );
    	String[] ids = new String[ idList.size() ];
    	ids = idList.toArray( ids );
    	return ids;
    }
    
    private static TagData getTagData( String visitorID )
    {
    	String xml = "";
    	try
    	{
    		xml = HTTP_GET( new URL( "http", host, "/api/v1/content/visit/" + visitorID + "?tok=" + token ) );
    	}
    	catch( Exception e )
    	{
    		e.printStackTrace();
    	}
    	xml = xml.replaceAll( "\n" , "" ).replaceAll( "\r", "" );
    	
    	String patternString = "<\\s*Tag[^>]+Type=\"([^\"]*)\"";
    	Pattern pattern = Pattern.compile(patternString);
    	Matcher matcherType = pattern.matcher( xml );

    	patternString = "<\\s*Tag[^>]+Symbols=\"([^\"]*)\"";
    	pattern = Pattern.compile(patternString);
    	Matcher matcherSymbols = pattern.matcher( xml );

    	return new TagData( matcherType.find() ? matcherType.group( 1 ) : null, matcherSymbols.find() ? matcherSymbols.group( 1 ) : null );
    }
    
    private Visitor[] getCurrentVisitors( LocationID locID )
    {
    	String[] ids = getCurrentVisitorIDs( locID );
    	Visitor[] v = new Visitor[ ids.length ];
    	for( int i = 0; i < ids.length; ++i )
    		v[ i ] = new Visitor( ids[ i ], locID );
    	return v;
    }
    
    public static boolean postPNGImageForVisitor( Visitor v, BufferedImage bi, String name )
    {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try { ImageIO.write( bi, "png", baos ); } catch( IOException ioe ) { ioe.printStackTrace(); return false; }
		ByteArrayInputStream bais = new ByteArrayInputStream( baos.toByteArray() );
		String xml = null;
		try
    	{			
			URL url = new URL( "http", host, "/api/v1/content.svc/exhibit-blob/" + exhibitID + "/" + v.getID() + "/" + name + "?tok=" + token );
			System.out.println( url );
    		xml = HTTP_POST( url, bais );
    	}
    	catch( Exception e )
    	{
    		e.printStackTrace();
    	}
		return xml != null && xml.toLowerCase().matches( "success" );
    }
    

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
	}
	
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("UserVerificationDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         
        frame.getContentPane().setLayout( new BorderLayout() );
        frame.getContentPane().add( new UserVerification(
        	LocationID.LEFT,
        	new UserVerification.ActionListener() {
				
				@Override
				public void visitorSelected(UserVerification.Visitor v) {
					// post user content
					System.out.println( "visitor " + v.toString() + " selected" );
					postPNGImageForVisitor( v, UserVerification.errorTagImage, "test0815.png" );
				}
				
				@Override
				public void canceled() {
					System.out.println( "visitor verification canceled");
					System.exit( 0 );
				}
			}
        ) );
         
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}
