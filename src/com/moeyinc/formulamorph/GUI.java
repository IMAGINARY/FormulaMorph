package com.moeyinc.formulamorph;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.vecmath.*;
import javax.imageio.ImageIO;

import java.io.*;
import java.net.*;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.EnumSet;
import java.text.DecimalFormat;

//import com.moeyinc.formulamorph.Parameters.ActiveParameterListener;
import de.mfo.jsurfer.algebra.*;
import de.mfo.jsurfer.rendering.*;
import de.mfo.jsurfer.util.BasicIO;

public class GUI extends JFrame implements Parameter.ValueChangeListener
{		
	class SurfaceGUIElements
	{
		final public JSurferRenderPanel panel;
		final public LaTeXLabel title;
		final public LaTeXLabel equation;
		final public JPanel levelIcon;
		final public LaTeXLabel levelLabel;
		final private Surface surface;
		private List< Gallery.GalleryItem > gallery_items;
		private List< Gallery.GalleryItem > gallery_items_unmodifyable;
		private int id_in_gallery;
		private ArrayList< JPanel > gallery_panels;
		private List< JPanel > gallery_panels_unmodifiable;
		private Map< Gallery.Level, ImageScaler > levelIcons;
		
		public SurfaceGUIElements( Surface s )
		{
			levelIcons = new EnumMap< Gallery.Level, ImageScaler >( Gallery.Level.class );
			levelIcons.put( Gallery.Level.BASIC, new ImageScaler( sierpinskiBASIC ) );
			levelIcons.put( Gallery.Level.INTERMEDIATE, new ImageScaler( sierpinskiINTERMEDIATE ) );
			levelIcons.put( Gallery.Level.ADVANCED, new ImageScaler( sierpinskiADVANCED ) );
			
			surface = s;
			id_in_gallery = 0;

			LaTeXCommands.getDynamicLaTeXMap().put( "FMImage" + s.name(), "\\includejavaimage[interpolation=bicubic]{FMImage" + s.name() + "}" );
			
			this.panel = new JSurferRenderPanel();
			
			this.title = new LaTeXLabel( "\\sf\\bf\\LARGE\\fgcolor{white}{\\jlmDynamic{FMTitle" + s.name() + "}}" );
			this.title.setHAlignment( LaTeXLabel.HAlignment.CENTER );
			this.title.setVAlignment( LaTeXLabel.VAlignment.CENTER_BASELINE );
			//this.title.setBackground( Color.GRAY ); this.title.setOpaque( true ); 
			
			this.equation = new LaTeXLabel( staticLaTeXSrc( s ) );
			//this.equation.setBackground( Color.GRAY ); this.equation.setOpaque( true );
			
			this.levelIcon = new JPanel( new BorderLayout() );
			this.levelLabel = new LaTeXLabel( "\\tiny\\fgcolor{white}{\\jlmDynamic{FMLevel" + s.name() + "}}" );
			
			gallery_panels = new ArrayList< JPanel >();
			for( int i = 0; i < 7; ++i )
			{
				JPanel p = new JPanel( new BorderLayout() );
				p.setBackground( new Color( 0.1f, 0.1f, 0.1f ) );
				p.setOpaque( true );
				gallery_panels.add( i, p );
			}
			gallery_panels.get( gallery_panels.size() / 2 ).setBorder( BorderFactory.createLineBorder( Color.WHITE, 3 ) );		
			gallery_panels_unmodifiable = Collections.unmodifiableList( gallery_panels );
			
			// load galleries
			gallery_items = new ArrayList< Gallery.GalleryItem >();
			gallery_items_unmodifyable = Collections.unmodifiableList( gallery_items );
			try
			{
				switch( surface )
				{
					case F:
					case G:
						this.gallery_items.addAll( new Gallery( Gallery.Level.BASIC, new File( "gallery" + File.separator + "basic" ) ).getItems() );
						this.gallery_items.addAll( new Gallery( Gallery.Level.INTERMEDIATE, new File( "gallery" + File.separator + "intermediate" ) ).getItems() );
						this.gallery_items.addAll( new Gallery( Gallery.Level.ADVANCED, new File( "gallery" + File.separator + "advanced" ) ).getItems() );
						break;
					default:
						break;
				}
			}
			catch( Exception e )
			{
				System.err.println( "Unable to initialize galleries" );
				e.printStackTrace( System.err );
				System.exit( -1 );
			}
		}
		
		public int id() { return id_in_gallery; }
		public void setId( int id ) { id_in_gallery = id; }
		public List< Gallery.GalleryItem > galleryItems() { return gallery_items_unmodifyable; }
		
		public List< JPanel > galleryPanels() { return gallery_panels_unmodifiable; }
		public int highlightedGalleryPanel() { return 3; }
		
		public void setLevelIcon( Gallery.Level l )
		{
		    LaTeXCommands.getDynamicLaTeXMap().put( "FMLevel" + surface.name(), l.name().substring(0,1).toUpperCase() + l.name().substring(1).toLowerCase() );
			this.levelIcon.removeAll();
			this.levelIcon.add( this.levelIcons.get( l ) );
		}
	}
	
	JPanel content; // fixed 16:9 top container
	static final double aspectRatio = 16.0 / 9.0;
	static final DecimalFormat decimalFormatter = new DecimalFormat("#0.00");

	EnumMap< Surface, SurfaceGUIElements > surface2guielems = new EnumMap< Surface, SurfaceGUIElements >( Surface.class );
		
	static BufferedImage triangle;
	static BufferedImage triangleFlipped;
	static {
		triangle = new BufferedImage( 100, 100, BufferedImage.TYPE_BYTE_GRAY );
		Graphics2D g = (Graphics2D) triangle.getGraphics();
		g.setColor( new Color( 0, 0, 0, 0 ) );
		g.fillRect( 0, 0, triangle.getWidth(), triangle.getHeight() );
		g.setColor( Color.LIGHT_GRAY );
		Polygon p = new Polygon();
		p.addPoint( -triangle.getWidth() / 2, 0 );
		p.addPoint( triangle.getWidth() / 2, 0 );
		p.addPoint( 0, (int) ( triangle.getWidth() * Math.sqrt( 3.0 ) / 2.0 ) );
		AffineTransform tx = AffineTransform.getScaleInstance( 0.6, 0.6 );
		tx.preConcatenate( AffineTransform.getTranslateInstance( triangle.getWidth() / 2, 0 ) );
		g.setTransform( tx );
		g.fillPolygon( p );
		
		tx = AffineTransform.getScaleInstance(1, -1);
		tx.translate(0, -triangle.getHeight(null));
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		triangleFlipped = op.filter( triangle, null);
	}
	ImageScaler triangleFTop = new ImageScaler( triangleFlipped );
	ImageScaler triangleFBottom = new ImageScaler( triangle );
	ImageScaler triangleGTop = new ImageScaler( triangleFlipped );
	ImageScaler triangleGBottom = new ImageScaler( triangle );
	
	static BufferedImage sierpinskiBASIC = loadIMGResource( "BASIC.png" );
	static BufferedImage sierpinskiINTERMEDIATE = loadIMGResource( "INTERMEDIATE.png" );
	static BufferedImage sierpinskiADVANCED = loadIMGResource( "ADVANCED.png" );
	
	public static BufferedImage loadIMGResource( String path )
	{
		InputStream stream = GUI.class.getResourceAsStream( path );
		try
		{ 
			return ImageIO.read( stream ); 
		}
		catch (IOException e)
		{
			System.err.println( "unable to load resource \"" + path + "\"" );
			e.printStackTrace();
		}
		return new BufferedImage( 1,1, BufferedImage.TYPE_BYTE_GRAY );
	}
			
	//JPanel blackStrip;

	RotationAnimation rotationAnimation;
	
	final GUIController caGUI = new GUIController();
	JFrame caGUIFrame = new JFrame();
	JInternalFrame caGUIInternalFrame = new JInternalFrame();
		
	public GUI()
		throws Exception, IOException
	{
		super( "FormulaMorph Main Window" );
		
		this.getLayeredPane().add( caGUIInternalFrame );
		this.addMouseListener( new MouseAdapter() { public void mouseClicked( MouseEvent e ) { if( e.getButton() == MouseEvent.BUTTON2 || e.isMetaDown() ) setupControllerGUI( true ); else requestFocus(); } } );
		
    	Parameter.M_t.setMin( 0.0 );
    	Parameter.M_t.setMax( 1.0 );
		Parameter.M_t.setValue( 0.5 );
		LaTeXCommands.getDynamicLaTeXMap().put( "OneMinusT", "0.00" );
		for( Parameter p : Parameter.values() )
			p.addValueChangeListener( this );

		// setup the container which has fixed 16:9 aspect ratio
		content = new JPanel();
		content.setBackground(Color.BLACK);
		content.setLayout( null );

		// init components
		surface2guielems.put( Surface.F, new SurfaceGUIElements( Surface.F ) );
		surface2guielems.put( Surface.M, new SurfaceGUIElements( Surface.M ) );
		surface2guielems.put( Surface.G, new SurfaceGUIElements( Surface.G ) );
		
		//blackStrip = new JPanel(); blackStrip.setBackground( Color.black );
		
		final LaTeXLabel eqF = s2g( Surface.F ).equation;
		final LaTeXLabel eqM = s2g( Surface.M ).equation;
		final LaTeXLabel eqG = s2g( Surface.G ).equation;
		s2g( Surface.F ).panel.addImageUpdateListener( new JSurferRenderPanel.ImageUpdateListener() {
			public void imageUpdated( Image img )
			{
				LaTeXCommands.getImageMap().put( "FMImageF", img );
				eqF.repaint();
				eqM.repaint();
			}
		});		
		s2g( Surface.G ).panel.addImageUpdateListener( new JSurferRenderPanel.ImageUpdateListener() {
			public void imageUpdated( Image img )
			{
				LaTeXCommands.getImageMap().put( "FMImageG", img );
				eqG.repaint();
				eqM.repaint();
			}
		});
		
		// add components
		content.add( s2g( Surface.F ).title );
		content.add( s2g( Surface.F ).panel );		
		content.add( s2g( Surface.F ).equation );

		content.add( s2g( Surface.M ).panel );		
		content.add( s2g( Surface.M ).equation );

		content.add( s2g( Surface.G ).title );
		content.add( s2g( Surface.G ).panel );		
		content.add( s2g( Surface.G ).equation );

		for( JComponent c : s2g( Surface.F ).galleryPanels() )
			content.add( c );
		for( JComponent c : s2g( Surface.G ).galleryPanels() )
			content.add( c );
		content.add( triangleFTop );
		content.add( triangleFBottom );
		content.add( triangleGTop );
		content.add( triangleGBottom );

		content.add( s2g( Surface.F ).levelLabel );
		content.add( s2g( Surface.F ).levelIcon );
		content.add( s2g( Surface.G ).levelLabel );
		content.add( s2g( Surface.G ).levelIcon );
		
		//content.add( blackStrip );

		// layout components
		refreshLayout();

		getContentPane().addComponentListener( new ComponentListener() {
		    public void componentResized(ComponentEvent e) {
				// keep aspect ratio
				Rectangle b = e.getComponent().getBounds();
				Dimension d;
				if( b.width * 9 < b.height * 16 )
					d = new Dimension( b.width, ( 9 * b.width ) / 16 );
				else
					d = new Dimension( ( 16 * b.height ) / 9, b.height );

				content.setBounds( b.x, b.y + ( b.height - d.height ) / 2, d.width, d.height );

				// setup the layout again
				refreshLayout();
			}
			public void componentHidden(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
		} );
		getContentPane().setLayout( null );
		getContentPane().add( content );
		getContentPane().setBackground( Color.DARK_GRAY );		

		try
		{
	        Properties props = new Properties();
	        props.load( new File( "gallery/defaults.jsurf" ).toURI().toURL().openStream() );
	        for( Surface s : Surface.values() )
	        	setDefaults( s, props );
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
				
		rotationAnimation = new RotationAnimation(); 
		rotationAnimation.pause();
		new Thread( rotationAnimation ).start();

        selectRandomSurface( Surface.F );
        selectRandomSurface( Surface.G );
        
        this.addKeyListener( new KeyAdapter() {
            public void keyTyped( KeyEvent ke )
            {
                if( ke.getKeyChar() == 'r' )
                {
                    try { GUI.this.reload( Surface.F ); GUI.this.reload( Surface.G ); } catch( Exception e ) {}
                }
            }
        }
        );
	}
	
	SurfaceGUIElements s2g( Surface s ) { return this.surface2guielems.get( s ); }

	public void refreshLayout()
	{
		//blackStrip.setBounds( computeBoundsFullHD( content, 0, 84, 1920, 624 ) );
		
		int yCenter = 380; 
		
		int surfXStart = 36 + 62 + 1;
		int surfSize = ( 1920 - 2 * surfXStart ) / 3; 
		int surfYStart = yCenter - surfSize / 2;
		s2g( Surface.F ).panel.setBounds( computeBoundsFullHD( content, surfXStart, surfYStart, surfSize, surfSize ) );		
		s2g( Surface.M ).panel.setBounds( computeBoundsFullHD( content, surfXStart + surfSize, surfYStart, surfSize, surfSize ) );		
		s2g( Surface.G ).panel.setBounds( computeBoundsFullHD( content, surfXStart + 2 * surfSize, surfYStart, surfSize, surfSize ) );		

		int titlePrefWidth = 700;
		int titlePrefHeight = surfYStart;
		s2g( Surface.F ).title.setPreferredSize( new Dimension( titlePrefWidth, titlePrefHeight ) );
		s2g( Surface.F ).title.setBounds( computeBoundsFullHD( content, surfXStart + ( surfSize - titlePrefWidth ) / 2.0, 0, titlePrefWidth, titlePrefHeight ) );
		s2g( Surface.G ).title.setPreferredSize( new Dimension( titlePrefWidth, titlePrefHeight ) );
		s2g( Surface.G ).title.setBounds( computeBoundsFullHD( content, surfXStart + 2 * surfSize + ( surfSize - titlePrefWidth ) / 2.0, 0, titlePrefWidth, titlePrefHeight ) );

		int elYStart = surfYStart + surfSize;
		int elPrefHeight = 1080 - elYStart;
		int elPrefWidth = 516;
		double elXStart = surfXStart + ( surfSize - elPrefWidth ) / 2;
		s2g( Surface.F ).equation.setPreferredSize( new Dimension( elPrefWidth, elPrefHeight ) );
		s2g( Surface.F ).equation.setBounds( computeBoundsFullHD( content, elXStart, elYStart, elPrefWidth, elPrefHeight ) );
		s2g( Surface.M ).equation.setPreferredSize( new Dimension( elPrefWidth, elPrefHeight ) );
		s2g( Surface.M ).equation.setBounds( computeBoundsFullHD( content, 1920 / 2 - elPrefWidth / 2.0, elYStart, elPrefWidth, elPrefHeight ) );
		s2g( Surface.G ).equation.setPreferredSize( new Dimension( elPrefWidth, elPrefHeight ) );
		s2g( Surface.G ).equation.setBounds( computeBoundsFullHD( content, 1920 - elXStart - elPrefWidth, elYStart, elPrefWidth, elPrefHeight ) );
		
		int gal_length = s2g( Surface.F ).galleryPanels().size();
		int galSize = 62;
		int spacing = 8;
		int galXStart = 36;
		int galYStart = yCenter - ( gal_length * galSize + ( gal_length - 1 ) * spacing ) / 2;
		for( int i = 0; i < gal_length; ++i )
		{
			s2g( Surface.F ).galleryPanels().get( i ).setBounds( computeBoundsFullHD( content, galXStart, galYStart + i * ( galSize + spacing ), galSize, galSize ) );
			s2g( Surface.F ).galleryPanels().get( i ).revalidate();
			s2g( Surface.G ).galleryPanels().get( i ).setBounds( computeBoundsFullHD( content, 1920 - galXStart - galSize, galYStart + i * ( galSize + spacing ), galSize, galSize ) );
			s2g( Surface.G ).galleryPanels().get( i ).revalidate();
		}
		triangleFTop.setBounds( computeBoundsFullHD( content, galXStart, galYStart + -1 * ( galSize + spacing ), galSize, galSize ) );
		triangleFBottom.setBounds( computeBoundsFullHD( content, galXStart, galYStart + gal_length * ( galSize + spacing ), galSize, galSize ) );
		triangleGTop.setBounds( computeBoundsFullHD( content, 1920 - galXStart - galSize, galYStart + -1 * ( galSize + spacing ), galSize, galSize ) );
		triangleGBottom.setBounds( computeBoundsFullHD( content, 1920 - galXStart - galSize, galYStart + gal_length * ( galSize + spacing ), galSize, galSize ) );
		
		int galYEnd = galYStart + gal_length * ( galSize + spacing ) + galSize;
		s2g( Surface.F ).levelIcon.setBounds( computeBoundsFullHD( content, galXStart, galYEnd + ( 1080 - galYEnd - galSize ) / 2.0, galSize, galSize ) );
		s2g( Surface.G ).levelIcon.setBounds( computeBoundsFullHD( content, 1920 - galXStart - galSize, galYEnd + ( 1080 - galYEnd - galSize ) / 2.0, galSize, galSize ) );

		s2g( Surface.F ).levelLabel.setPreferredSize( new Dimension( 2 * galSize, galSize / 2 ) );
		s2g( Surface.F ).levelLabel.setBounds( computeBoundsFullHD( content, galXStart - galSize / 2.0, galYEnd + ( 1080 - galYEnd ) / 2.0 - galSize, 2 * galSize, galSize / 2 ) );
		s2g( Surface.G ).levelLabel.setPreferredSize( new Dimension( 2 * galSize, galSize / 2 ) );
		s2g( Surface.G ).levelLabel.setBounds( computeBoundsFullHD( content, 1920 - galXStart - 3 * galSize / 2.0, galYEnd + ( 1080 - galYEnd ) / 2.0 - galSize, 2 * galSize, galSize / 2 ) );
		
		repaint();
	}
	
	private static Rectangle computeBoundsFullHD( Component p, double x, double y, double w, double h )
	{
		if( p.getWidth() == 1920 && p.getHeight() == 1080 )
		{
			return new Rectangle( (int) x, (int) y, (int) w, (int) h );
		}
		else
		{
			x = x / 19.2; y = y / 10.8; w = w / 19.2; h = h / 10.8;
			return computeBounds( p, x, y, w, h );
		}
	}

	private static Rectangle computeBounds( Component p, double x, double y, double w, double h )
	{
		x = x / 100; y = y / 100; w = w / 100; h = h / 100;
		return new Rectangle( (int) ( p.getWidth() * x ), (int) ( p.getHeight() * y ), (int) ( p.getWidth() * w ), (int) ( p.getHeight() * h ) );
	}

	protected void hideCursor( boolean hide )
	{
		// Transparent 16 x 16 pixel boolean fullscreen cursor image.
		BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

		// Create a new blank cursor.
		Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
			cursorImg, new Point(0, 0), "blank cursor");

		// Set the blank cursor to the JFrame.
		getContentPane().setCursor( hide ? blankCursor : null );
	}

    public void tryFullScreen() {
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        boolean isFullScreen = device.isFullScreenSupported();
        if( !isFullScreen )
        	System.err.println( "Fullscreen mode not supported on this plattform! We try it anyway ..." );

        boolean visible = isVisible();
        setVisible(false);
        dispose();
        setUndecorated(true);
        setResizable(false);
        validate();
        device.setFullScreenWindow( this );
        setVisible(visible);
        hideCursor( true );
        setupControllerGUI( caGUIFrame.isVisible() || caGUIInternalFrame.isVisible() );
    }
    
    public void tryWindowed()
    {
    	GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    	boolean visible = isVisible();
    	setVisible( false );
    	dispose();
        setUndecorated(false);
        setResizable(true);
        validate();
        if( device.getFullScreenWindow() == this )
        	device.setFullScreenWindow( null );
        setVisible( visible );
        hideCursor( false );
        setupControllerGUI( caGUIFrame.isVisible() || caGUIInternalFrame.isVisible() );
    }

    public void setupControllerGUI( boolean visible )
    {
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		caGUIFrame.setVisible( false );
		caGUIInternalFrame.setVisible( false );
		caGUIFrame.dispose();
		caGUIInternalFrame.dispose();
		caGUIFrame.getContentPane().remove( caGUI );
		caGUIInternalFrame.getContentPane().remove( caGUI );
		
		if( device.getFullScreenWindow() == GUI.this )
		{
			caGUIInternalFrame = new JInternalFrame();
			caGUIInternalFrame.setClosable( true );
			caGUIInternalFrame.setDefaultCloseOperation( JInternalFrame.DISPOSE_ON_CLOSE );
			caGUIInternalFrame.getContentPane().add( caGUI );
			caGUIInternalFrame.pack();
			GUI.this.getLayeredPane().add( caGUIInternalFrame );
			caGUIInternalFrame.setLocation( ( this.getWidth() - caGUIInternalFrame.getWidth() ) / 2, ( this.getHeight() - caGUIInternalFrame.getHeight() ) / 2 );
			caGUIInternalFrame.setVisible( visible );
		}
		else
		{
			caGUIFrame = new JFrame();
			caGUIFrame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
			caGUIFrame.getContentPane().add( caGUI );
			caGUIFrame.pack();
			if( caGUIFrame.isAlwaysOnTopSupported() )
				caGUIFrame.setAlwaysOnTop( true );
			caGUIFrame.setVisible( visible );
		}    	
    }
    
    public void saveScreenShotLeft() { try { saveScreenShotToFile( new File( "left.png" ) ); } catch ( IOException ioe ) { ioe.printStackTrace(); } }
    public void saveScreenShotRight() { try { saveScreenShotToFile( new File( "right.png" ) ); } catch ( IOException ioe ) { ioe.printStackTrace(); } }
    
    public void saveScreenShotToFile( File f )
    		throws IOException
    {
    	FileOutputStream fos = new FileOutputStream( f );
    	saveScreenShot( fos );
    	fos.close();
    }
    
    public void saveScreenShotToURL( URL url )
    		throws IOException
    {
		URLConnection urlCon = url.openConnection();
		urlCon.setDoOutput( true );
		urlCon.connect();
		OutputStream out = urlCon.getOutputStream();
		saveScreenShot( out );    	
		out.close();
    }
    
    public void saveScreenShot( OutputStream out )
    	throws IOException
    {
    	BufferedImage image = new BufferedImage( content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB );
        Graphics2D graphics2D = image.createGraphics();
        content.paint( graphics2D );
        javax.imageio.ImageIO.write( image, "png", out );    	
    }

    private static final String staticLaTeXSrc( Surface s )
    {
		LaTeXCommands.getImageMap().put( "FMPImage" + s.name(), null ); // at least put it in the map although it is still null
    	
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( "\\newcommand{\\fixheight}{\\vphantom{Tpgqy}}" );
    	
    	// parameters
    	for( Parameter p : s.getParameters() )
    	{
			sb.append( p.getLaTeXColorDefinition() );
			sb.append( "\\newcommand{\\FMC");
			sb.append( p.getSurface().name() );
			sb.append( p.getName() );
			sb.append( "}[1]{\\fgcolor{");
			sb.append( p.getLaTeXColorName() );
			sb.append( "}{#1}}\n" );		
			
			sb.append( "\\newcommand{\\FMB");
			sb.append( p.getSurface().name() );
			sb.append( p.getName() );
			sb.append( "}[1]{\\FMC" ); 
			sb.append( p.getSurface().name() );
			sb.append( p.getName() );
			sb.append( "{\\FMOvalbox[0.4em]{\\fgcolor{white}{#1}}}}\n" );	

			sb.append( "\\newcommand{\\FMV" );
			sb.append( p.getSurface().name() );
			sb.append( p.getName() );
			sb.append( "}{\\jlmDynamic{FMParam" );
			sb.append( p.getSurface().name() );
			sb.append( p.getName() );
			sb.append( "}}\n" );		
			
			sb.append( "\\newcommand{\\FMP" );
			sb.append( p.getSurface().name() );
			sb.append( p.getName() );
			sb.append( "}{\\FMB" ); 
			sb.append( p.getSurface().name() );
			sb.append( p.getName() );
			sb.append( "{\\FMV" );
			sb.append( p.getSurface().name() );
			sb.append( p.getName() );
			sb.append( "\\vphantom{-}}}\n" );
    	}
    	
    	String rem;
    	switch( s )
    	{
    	case M:
        	rem = "" +
    			"\\newcommand{\\nl}{\\\\}\n" +
    			"\\sf\\fgcolor{white}{\\begin{array}{c}\n" +
    				"\\bf\\Large\\text{Formula Morph}\\\\\\\\\n" +
					"\\sf\\FMBMt{\\jlmDynamic{OneMinusT}}\\:\\cdot\\raisebox{4.1275ex}{\\scalebox{1}[-1]{\\resizebox{7ex}{!}{\\jlmDynamic{FMImageF}}}}\\qquad{\\bf +}\\qquad\\:\\FMPMt\\:\\cdot\\raisebox{4.1275ex}{\\scalebox{1}[-1]{\\resizebox{7ex}{!}{\\jlmDynamic{FMImageG}}}}\n" +
    			"\\end{array}}";
        	break;
    	case F:
        case G:
        	rem = "" +
    			"\\newcommand{\\nl}{\\\\}\n" +
    			"\\sf\\begin{array}{c}\n" +
    				//"\\raisebox{-2.5em}{\\bf\\Large\\fgcolor{white}\\FMDynamic[i]{FMTitleFormula" + s.name() + "}}\\\\\n" +
    				"{\\bf\\Large\\fgcolor{white}\\FMDynamic[i]{FMTitleFormula" + s.name() + "}}\\\\\n" +
    				"\\fgcolor{white}{\n" +
    					"\\left[\n" +
//    					"\\fgcolor{white}{\n" + 
    						"\\begin{array}{c}\n" +
    							"{\\Large\\FMDynamic[i]{FMTitleWImage" + s.name() + "}=}{\\small\\raisebox{3.4ex}{\\scalebox{1}[-1]{\\resizebox{5ex}{!}{\\jlmDynamic{FMImage" + s.name() + "}}}}}\\\\\\\\\n" +
    							"\\FMDynamic[i]{FMEquation" + s.name() + "}\\\\\n" +
    							"\\hphantom{MMMMMMMMMMMMMMMMMMMaj\\,}\n" +
    						"\\end{array}\n" +
//    					"}\n" +
    					"\\right]\n" +
    				"}\\\\\n" +
        			"\\vspace{0em}\n" +    					
    			"\\end{array}";
        	break;
        default:
        	rem = "unknown surface: " + s;	
    	}
    	
    	sb.append( rem );
    	return sb.toString();
    }
    
    public void setDefaults( Surface surf, Properties props )
    {
    	AlgebraicSurfaceRenderer asr = s2g( surf ).panel.getAlgebraicSurfaceRenderer();

        asr.getCamera().loadProperties( props, "camera_", "" );
        Util.setOptimalCameraDistance( asr.getCamera() );

        for( int i = 0; i < AlgebraicSurfaceRenderer.MAX_LIGHTS; i++ )
        {
            asr.getLightSource( i ).setStatus(LightSource.Status.OFF);
            asr.getLightSource( i ).loadProperties( props, "light_", "_" + i );
        }
        asr.setBackgroundColor( BasicIO.fromColor3fString( props.getProperty( "background_color" ) ) );

        Matrix4d identity = new Matrix4d();
        identity.setIdentity();
        asr.setTransform( BasicIO.fromMatrix4dString( props.getProperty( "rotation_matrix" ) ) );
        Matrix4d scaleMatrix = new Matrix4d();
        scaleMatrix.setIdentity();
        scaleMatrix.setScale( 1.0 / Double.parseDouble( props.getProperty( "scale_factor" ) ) );
        asr.setSurfaceTransform( scaleMatrix );
    }
    
    public void valueChanged( Parameter p )
    {
    	LaTeXCommands.getDynamicLaTeXMap().put( "FMParam" + p.getSurface().name() + p.getName(), decimalFormatter.format( p.getValue()) );
		
    	JSurferRenderPanel jsp = s2g( p.getSurface() ).panel;
    	jsp.getAlgebraicSurfaceRenderer().setParameterValue( Character.toString( p.getName() ), p.getValue() );
    	jsp.scheduleSurfaceRepaint();

    	AlgebraicSurfaceRenderer asr_M =  s2g( Surface.M ).panel.getAlgebraicSurfaceRenderer();
    	if( p.getSurface() == Surface.M )
    	{
    		// interpolate colors of the morph
    		float t = (float) Parameter.M_t.getValue();
    		asr_M.setParameterValue( Character.toString( p.getName() ), p.getValue() );

    		AlgebraicSurfaceRenderer asr_F =  s2g( Surface.F ).panel.getAlgebraicSurfaceRenderer();
    		AlgebraicSurfaceRenderer asr_G =  s2g( Surface.G ).panel.getAlgebraicSurfaceRenderer();
    		
    		asr_M.setFrontMaterial( Material.lerp( asr_F.getFrontMaterial(), asr_G.getFrontMaterial(), t ) );
    		asr_M.setBackMaterial( Material.lerp( asr_F.getBackMaterial(), asr_G.getBackMaterial(), t ) );

    		// update OneMinusT global LaTeX variable
        	LaTeXCommands.getDynamicLaTeXMap().put( "OneMinusT", decimalFormatter.format( Math.max( 0.0, 1.0 - p.getValue() ) ) );
       	}
    	else
    	{
    		// the parameter at morph, too
    		asr_M.setParameterValue( p.name(), p.getValue() );
    	}
    	
		s2g( p.getSurface() ).equation.repaint();
		s2g( Surface.M ).panel.scheduleSurfaceRepaint();
//		System.out.println(LaTeXCommands.getDynamicLaTeXMap().toString() );
    }
	
    public void nextSurface( Surface s )
    {
    	nextSurface( s, 1 );
    }
    
    public void previousSurface( Surface s )
    {
    	previousSurface( s, 1 );
    }
 
    public void nextSurface( Surface s, int offset )
    {
    	s2g( s ).setId( s2g( s ).id() + offset );
    	idChanged( s );
    }
    
    public void previousSurface( Surface s, int offset )
    {
    	nextSurface( s, -offset );   	
    }
    
    public void setLevel( Surface s, Gallery.Level level ) // jump to the middle item in the gallery that has the requested level
    {
    	if( s2g( s ).galleryItems().get( s2g( s ).id() ).level() != level )
    	{
	    	int id;
	    	List< Gallery.GalleryItem > galleryItems = s2g( s ).galleryItems();
	    	for( id = 0; id < galleryItems.size() && galleryItems.get( id ).level() != level; ++id )
	    		; // find first item with requested level
	    	/*
	    	int count:
	    	for( count = 0; count + id < galleryItems.size() && galleryItems.get( id + count ).level() == level; ++count )
	    		; // count items which have that level starting from the first one
	    	s2g( s ).setId( id + count / 2 );
	    	*/
	    	s2g( s ).setId( id );
	    	idChanged( s );
    	}
    }
    
    private Gallery easterEggGallery = new Gallery( Gallery.Level.BASIC, new File( "gallery" + File.separator + "easter" ) );
    private java.util.Random easterEggSelector = new java.util.Random();
    public void selectEasterEggSurface( Surface s )
    {
    	List< Gallery.GalleryItem > items = easterEggGallery.getItems();
    	setSurface(s, items.get( easterEggSelector.nextInt( items.size() ) ) );
    }

    public void selectRandomSurface( Surface s ) { selectRandomSurface( s, new java.util.Random() ); }
    public void selectRandomSurface( Surface s, java.util.Random r )
    {
    	s2g( s ).setId( r.nextInt( s2g( s ).galleryItems().size() ) );
    	idChanged( s );
    }
    
    public void idChanged( Surface s )
    {
    	if( s == Surface.M )
    		return;
    	
    	List< Gallery.GalleryItem > galleryItems = s2g( s ).galleryItems();
    	if( s2g( s ).id() < 0 )
    	{
    		s2g( s ).setId( 0 );
    		return;
    	}
    	if( s2g( s ).id() >= galleryItems.size() )
    	{
    		s2g( s ).setId( galleryItems.size() - 1 );
    		return;
    	}

    	Gallery.GalleryItem galleryItem = galleryItems.get( s2g( s ).id() );
    	s2g( s ).setLevelIcon( galleryItem.level() );
    	    	
		{	// set content of gallery panels 
			List< JPanel > galleryPanels = s2g( s ).galleryPanels();
	    	for( JPanel p : galleryPanels )
	    	{
	    		p.removeAll();
	    		p.revalidate();
	    	}
	    	for( int panel_id = 0; panel_id < galleryPanels.size(); ++panel_id )
	    	{
	    		JPanel p = galleryPanels.get( panel_id ); 
	    		int galItemId = s2g( s ).id() - s2g( s ).highlightedGalleryPanel() + panel_id;
	    		if( galItemId >= 0 && galItemId < galleryItems.size() )
	    		{
	    			Gallery.GalleryItem item = galleryItems.get( galItemId );
	    			item.setGrayScale( panel_id != s2g( s ).highlightedGalleryPanel() );
	    			p.add( item );
	    			p.revalidate();
	    			p.repaint();
	    		}
	    	}
	    }
		
		setSurface( s, galleryItem );
    }
    
    public void setSurface( Surface s, Gallery.GalleryItem galleryItem )
    {
    	s2g( s ).panel.setScheduleSurfaceRepaintEnabled( false );
    	s2g( Surface.M ).panel.setScheduleSurfaceRepaintEnabled( false );

    	try
    	{
    		loadFromProperties( s, galleryItem.jsurfProperties() );
    	}
    	catch( Exception e )
    	{
    		System.err.println( "Could not load item " + s2g( s ).id() + " of " + galleryItem.level().name() + " gallery of " + s.name() );
    		e.printStackTrace();
    		return;
    	}

    	// set the morphed surface
    	AlgebraicSurfaceRenderer asr_F = s2g( Surface.F ).panel.getAlgebraicSurfaceRenderer();
    	AlgebraicSurfaceRenderer asr_M = s2g( Surface.M ).panel.getAlgebraicSurfaceRenderer();
    	AlgebraicSurfaceRenderer asr_G = s2g( Surface.G ).panel.getAlgebraicSurfaceRenderer();
    	
    	CloneVisitor cv = new CloneVisitor();

    	// retrieve syntax tree for F and rename parameters (a->F_a,...)
    	PolynomialOperation po_F = asr_F.getSurfaceFamily().accept( cv, ( Void ) null );
    	Map< String, String > m_F = new HashMap< String, String >();
    	for( Parameter p : Surface.F.getParameters() )
    		m_F.put( Character.toString( p.getName() ), p.name() );
    	po_F = po_F.accept( new DoubleVariableRenameVisitor( m_F ), ( Void ) null );

    	// retrieve syntax tree for G and rename parameters (a->G_a,...)
    	PolynomialOperation po_G = asr_G.getSurfaceFamily().accept( cv, ( Void ) null );
    	Map< String, String > m_G = new HashMap< String, String >();
    	for( Parameter p : Surface.G.getParameters() )
    		m_G.put( Character.toString( p.getName() ), p.name() );
    	po_G = po_G.accept( new DoubleVariableRenameVisitor( m_G ), ( Void ) null );
    	
    	// connect both syntax trees using po_F*(1-t)+t+po_G
    	PolynomialOperation po_M = new PolynomialAddition(
    			new PolynomialMultiplication(
    					new DoubleBinaryOperation(
    							DoubleBinaryOperation.Op.sub,
    							new DoubleValue( 1.0 ),
    							new DoubleVariable( "t" )
    							),
    					po_F
    					),
    			new PolynomialMultiplication(
    					new DoubleVariable( "t" ),
    					po_G
    					)
    			);
    	asr_M.setSurfaceFamily( po_M );
    	Parameter.M_t.setActive( true );
    	Parameter.M_t.setMin( 0.0 );
    	Parameter.M_t.setMax( 1.0 );

		for( Parameter p : Surface.F.getParameters() )
		{
			p.notifyActivationStateListeners();
			p.notifyValueChangeListeners();
		}
		for( Parameter p : Surface.M.getParameters() )
		{
			p.notifyActivationStateListeners();
			p.notifyValueChangeListeners();
		}
		for( Parameter p : Surface.G.getParameters() )
		{
			p.notifyActivationStateListeners();
			p.notifyValueChangeListeners();
		}
		s2g( s ).title.reparseOnRepaint();
		s2g( s ).equation.reparseOnRepaint();
		
    	s2g( s ).panel.setScheduleSurfaceRepaintEnabled( true );
    	s2g( Surface.M ).panel.setScheduleSurfaceRepaintEnabled( true );
    	s2g( s ).panel.scheduleSurfaceRepaint();
    	s2g( Surface.M ).panel.scheduleSurfaceRepaint();
		
		repaint();    	
    }

    public void reload( Surface s )
    	throws IOException, Exception
    {
    	s2g( s ).galleryItems().get( s2g( s ).id() ).reload();
    	idChanged( s );
    }
    
    public void loadFromString( Surface surf, String s )
            throws Exception
    {
        Properties props = new Properties();
        props.load( new ByteArrayInputStream( s.getBytes() ) );
        loadFromProperties( surf, props );
    }

    public void loadFromFile( Surface s, URL url )
            throws IOException, Exception
    {
        Properties props = new Properties();
        props.load( url.openStream() );
        loadFromProperties( s, props );
    }

    public void loadFromProperties( Surface surf, Properties props ) // load only attributes that are not handled in setDefaults(...)
            throws Exception
    {
    	AlgebraicSurfaceRenderer asr = s2g( surf ).panel.getAlgebraicSurfaceRenderer();
        asr.setSurfaceFamily( props.getProperty( "surface_equation" ) );
        Set< String > param_names = asr.getAllParameterNames();
        EnumSet< Parameter > unsetFormulaMorphParams = surf.getParameters();

        for( String name : param_names )
        {
        	double value, min, max, speed;
        	try { value = Double.parseDouble( props.getProperty( "surface_parameter_" + name ) ); } catch( NumberFormatException nfe ) { value = Double.NaN; }
        	try { min = Double.parseDouble( props.getProperty( "surface_parametermin_" + name ) ); } catch( NumberFormatException nfe ) { min = Double.NaN; }
        	try { max = Double.parseDouble( props.getProperty( "surface_parametermax_" + name ) ); } catch( NumberFormatException nfe ) { max = Double.NaN; }
        	try { speed = Double.parseDouble( props.getProperty( "surface_parameterspeed_" + name ) ); }
        		catch( NumberFormatException nfe ) { speed = Double.NaN; }
        		catch( NullPointerException npe ) { speed = 1.0; }

        	min = Double.isNaN( min ) ? value : min;
        	max = Double.isNaN( max ) ? value : max;
        	value = value < min ? min : ( value > max ? max : value );
        	
    		Parameter fmp = ( name.length() == 1 ) ? surf.getParameter( name.charAt(0) ) : null;
    		if( fmp != null )
    		{
    			// set value/min/max of FormulaMorph parameter and activate
    			fmp.setMin( min );
    			fmp.setMax( max );
    			fmp.setValue( value );
    			fmp.setActive( true );
    			fmp.notifyActivationStateListeners();
    			fmp.notifyValueChangeListeners();
    			unsetFormulaMorphParams.remove( fmp );
    		}
        }
        
    	// disable FormulaMorphParameters that are not set
    	for( Parameter p : unsetFormulaMorphParams )
    		p.setActive( false );

        asr.getFrontMaterial().loadProperties(props, "front_material_", "");
        asr.getBackMaterial().loadProperties(props, "back_material_", "");
        
        LaTeXCommands.getDynamicLaTeXMap().put( "FMTitle" + surf.name(), "\\begin{array}{c}\n\\vphantom{T}\\\\\n\\text{\\fixheight{}" + props.getProperty( "surface_title_latex" ).replaceAll( "\\\\\\\\", "\\ " ) + "}\\\\\n\\vphantom{.}\\end{array}" );
        LaTeXCommands.getDynamicLaTeXMap().put( "FMTitleFormula" + surf.name(), "\\begin{array}{c}\n\\text{Formula for " + props.getProperty( "surface_title_latex" ).replaceAll( "\\\\\\\\", "}\\\\\\\\\\\\text{" ) + "\\fixheight}\\end{array}" );
        LaTeXCommands.getDynamicLaTeXMap().put( "FMTitleWImage" + surf.name(), "\\begin{array}{c}\n\\text{" + props.getProperty( "surface_title_latex" ).replaceAll( "\\\\\\\\", "}\\\\\\\\\\\\text{" ) + "\\fixheight}\\end{array}" );
        LaTeXCommands.getDynamicLaTeXMap().put( "FMEquation" + surf.name(), "{" + props.getProperty( "surface_equation_latex_size", "" ) + "\\begin{array}{c}\n" + props.getProperty( "surface_equation_latex" ).replaceAll( "\\\\FMB", "\\\\FMB" + surf.name() ).replaceAll( "\\\\FMC", "\\\\FMC" + surf.name() ).replaceAll( "\\\\FMV", "\\\\FMV" + surf.name() ).replaceAll( "\\\\FMP", "\\\\FMP" + surf.name() ).replaceAll( "\\\\\\\\", "\\\\nl" ) + "\n\\end{array}}\n" );
    }

    public void stepPath( int steps ) { this.rotationAnimation.stepPath( steps ); }
    public void pauseAnimation() { this.rotationAnimation.pause(); }
    public void resumeAnimation() { this.rotationAnimation.resume(); }
    
    class RotationAnimation implements Runnable
    {
    	boolean stop;
    	boolean pause;
    	Object lock;
    	Quat4d current;
    	Quat4d step;
    	
    	public RotationAnimation()
    	{
    		stop = false;
    		pause = true;
    		lock = new Object();
    		
    		current = new Quat4d();
    		current.set( new AxisAngle4d() );
    		
    		step = new Quat4d();
    		step.set( new AxisAngle4d() );
 
    		double angleStep = 6 * Math.PI / 1000;
    		Quat4d rotX = new Quat4d(); rotX.set( new AxisAngle4d( 1, 0, 0, angleStep ) ); step.mul( rotX );
    		Quat4d rotY = new Quat4d(); rotY.set( new AxisAngle4d( 0, 1, 0, angleStep ) ); step.mul( rotY );
    		Quat4d rotZ = new Quat4d(); rotZ.set( new AxisAngle4d( 0, 0, 1, angleStep ) ); step.mul( rotZ );
    	}
    	
    	public void setPathPosition()
    	{
			Matrix4d newRotation = new Matrix4d();
			newRotation.setIdentity();
			newRotation.setRotation( current );
			for( Surface s : Surface.values() )
			{
				GUI.this.s2g(s).panel.getAlgebraicSurfaceRenderer().setTransform( newRotation );
				GUI.this.s2g(s).panel.scheduleSurfaceRepaint();
			}
    	}
    	
    	public void stepPath( int steps )
    	{
    		for( int i = 0; i < steps; ++i )
    			current.mul( step );
    		for( int i = 0; i < -steps; ++i )
    			current.mulInverse( step );
    		setPathPosition();
    	}
    	
    	public void pause() { this.pause = true; }
    	public void resume() { if( pause ) { synchronized( lock ) { lock.notify(); } } this.pause = false; }
    	public void stop() { this.stop = true; }
    	
    	public void run()
    	{
    		synchronized( lock )
    		{
				try{ Thread.sleep( 500 ); } catch( Exception e ) {}	    		
	    		while( !stop )
	    		{
	    			try
	    			{
	    				Thread.sleep( 20 );
	    				if( pause )
	    					lock.wait();
	    			} catch( Exception e ) {}
	    			stepPath( 1 );
	    			setPathPosition();
	    		}
    		}
    	}
    }
}
