package com.moeyinc.formulamorph;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.vecmath.*;

import java.io.*;
import java.net.*;

import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Properties;
import java.util.Set;
import java.util.EnumSet;
import java.text.DecimalFormat;

//import com.moeyinc.formulamorph.Parameters.ActiveParameterListener;
import com.moeyinc.formulamorph.Parameters.*;

import de.mfo.jsurfer.algebra.*;
import de.mfo.jsurfer.rendering.*;
import de.mfo.jsurfer.util.BasicIO;

public class GUI extends JFrame implements ValueChangeListener, SurfaceIdListener
{
	class SurfaceGUIElements
	{
		final public JSurferRenderPanel panel;
		final public LaTeXLabel title;
		final public LaTeXLabel equation;
		
		public SurfaceGUIElements( Surface s )
		{
			LaTeXCommands.getDynamicLaTeXMap().put( "FMImage" + s.name(), "\\includejavaimage[width=5ex,interpolation=bicubic]{FMImage" + s.name() + "}" );
			
			this.panel = new JSurferRenderPanel();
			//this.panel.setBorder( BorderFactory.createLineBorder( Color.LIGHT_GRAY ) );
			
			this.title = new LaTeXLabel( "\\sf\\bf\\Huge\\text{\\jlmDynamic{FMTitle" + s.name() + "}}" );
			this.title.setHAlignment( LaTeXLabel.HAlignment.CENTER );
			this.title.setVAlignment( LaTeXLabel.VAlignment.CENTER_BASELINE );
			
			this.equation = new LaTeXLabel( staticLaTeXSrc( s ) );
		}		
	}
	
	final ControllerAdapterGUI caGUI = new ControllerAdapterGUI( null );
	JPanel content; // fixed 16:9 top container
	static final double aspectRatio = 16.0 / 9.0;
	static final DecimalFormat decimalFormatter = new DecimalFormat("#0.00");

	EnumMap< Parameters.Surface, SurfaceGUIElements > surface2guielems = new EnumMap< Parameters.Surface, SurfaceGUIElements >( Parameters.Surface.class );
		
	JPanel galF;
	JPanel galG;
	JPanel blackStrip;

	public GUI()
	{
		super( "FormulaMorph Main Window" );
	
		caGUI.setDefaultCloseOperation( HIDE_ON_CLOSE );
		this.addMouseListener( new MouseAdapter() { public void mouseClicked( MouseEvent e ) { caGUI.setVisible( true ); } } );
		
    	Parameter.M_t.setMin( 0.0 );
    	Parameter.M_t.setMax( 1.0 );
		Parameter.M_t.setValue( 0.5 );
		for( Parameter p : Parameter.values() )
			p.addValueChangeListener( this );

		// setup the container which has fixed 16:9 aspect ratio
		content = new JPanel();
		content.setBackground(Color.white);
		content.setLayout( null );

		// init components
		surface2guielems.put( Surface.F, new SurfaceGUIElements( Surface.F ) );
		surface2guielems.put( Surface.M, new SurfaceGUIElements( Surface.M ) );
		surface2guielems.put( Surface.G, new SurfaceGUIElements( Surface.G ) );
		
		blackStrip = new JPanel(); blackStrip.setBackground( Color.black );
		galF = new JPanel(); galF.setBackground( Color.lightGray );
		galG = new JPanel(); galG.setBackground( Color.lightGray );

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

		content.add( galF );
		content.add( galG );
		content.add( blackStrip );

		// layout components
		refreshLayout();

		getContentPane().addComponentListener( new ComponentListener() {
		    public void componentResized(ComponentEvent e) {
				// keep aspect ratio
				Rectangle b = e.getComponent().getBounds();
				if( b.width * 9 < b.height * 16 )
					content.setBounds( b.x, b.y, b.width, ( 9 * b.width ) / 16 );
				else
					content.setBounds( b.x, b.y, ( 16 * b.height ) / 9, b.height );

				// setup the layout again
				refreshLayout();
			}
			public void componentHidden(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
		} );
		getContentPane().setLayout( null );
		getContentPane().add( content );
		getContentPane().setBackground( Color.black );		

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
		
		idChanged( Surface.F );
		idChanged( Surface.G );
	}
	
	SurfaceGUIElements s2g( Surface s ) { return this.surface2guielems.get( s ); }

	public void refreshLayout()
	{
		blackStrip.setBounds( computeBoundsFullHD( content, 0, 84, 1920, 624 ) );
		
		s2g( Surface.F ).panel.setBounds( computeBoundsFullHD( content, 373 - 550 / 2, 84 + 624 / 2 - 550 / 2, 550, 550 ) );		
		s2g( Surface.M ).panel.setBounds( computeBoundsFullHD( content, 1920 / 2 - 624 / 2, 84, 624, 624  ) );
		s2g( Surface.G ).panel.setBounds( computeBoundsFullHD( content, ( 1920 - 373 ) - 550 / 2, 84 + 624 / 2 - 550 / 2, 550, 550 ) );

		int titlePrefWidth = 550;
		int titlePrefHeight = 84;
		s2g( Surface.F ).title.setPreferredSize( new Dimension( titlePrefWidth, titlePrefHeight ) );
		s2g( Surface.F ).title.setBounds( computeBoundsFullHD( content, 373 - 550 / 2, 0, titlePrefWidth, titlePrefHeight ) );
		s2g( Surface.G ).title.setPreferredSize( new Dimension( titlePrefWidth, titlePrefHeight ) );
		s2g( Surface.G ).title.setBounds( computeBoundsFullHD( content, ( 1920 - 373 ) - 550 / 2, 0, titlePrefWidth, titlePrefHeight ) );

		int elPrefWidth = 550;
		int elPrefHeight = 1080 - 708;
		s2g( Surface.F ).equation.setPreferredSize( new Dimension( elPrefWidth, elPrefHeight ) );
		s2g( Surface.F ).equation.setBounds( computeBoundsFullHD( content, 373 - 550 / 2, 708, elPrefWidth, elPrefHeight ) );
		s2g( Surface.M ).equation.setPreferredSize( new Dimension( elPrefWidth, elPrefHeight ) );
		s2g( Surface.M ).equation.setBounds( computeBoundsFullHD( content, 1920 / 2 - elPrefWidth / 2, 708, elPrefWidth, elPrefHeight ) );
		s2g( Surface.G ).equation.setPreferredSize( new Dimension( elPrefWidth, elPrefHeight ) );
		s2g( Surface.G ).equation.setBounds( computeBoundsFullHD( content, ( 1920 - 373 ) - 550 / 2, 708, elPrefWidth, elPrefHeight ) );

		galF.setBounds( computeBoundsFullHD( content, 38, 151, 60, 492 ) );
		galG.setBounds( computeBoundsFullHD( content, 1920 - 38 - 60, 151, 60, 492 ) );

		repaint();
	}

	private static Rectangle computeBoundsFullHD( Component p, double x, double y, double w, double h )
	{
		x = x / 19.2; y = y / 10.8; w = w / 19.2; h = h / 10.8;
		return computeBounds( p, x, y, w, h );
	}

	private static Rectangle computeBounds( Component p, double x, double y, double w, double h )
	{
		x = x / 100; y = y / 100; w = w / 100; h = h / 100;
		return new Rectangle( (int) ( p.getWidth() * x ), (int) ( p.getHeight() * y ), (int) ( p.getWidth() * w ), (int) ( p.getHeight() * h ) );
	}

	protected void hideCursor()
	{
		// Transparent 16 x 16 pixel boolean fullscreen cursor image.
		BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

		// Create a new blank cursor.
		Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
			cursorImg, new Point(0, 0), "blank cursor");

		// Set the blank cursor to the JFrame.
		getContentPane().setCursor(blankCursor);
	}

    public void tryFullScreen() {
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        boolean isFullScreen = device.isFullScreenSupported();
        if (isFullScreen)
			hideCursor();
		else
			System.err.println( "Fullscreen mode not supported on this plattform! We try it anyway ..." );

        setUndecorated(true);
        setResizable(false);
        validate();
        device.setFullScreenWindow( this );
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
    	
    	// surface name
    	sb.append( "\\newcommand{\\title" + s.name() + "}{\\FMDynamic[i]{FMTitle" + s.name() + "}}\n" );

    	// parameters
    	for( Parameter p : s.getParameters() )
    	{
			sb.append( p.getLaTeXColorDefinition() );
			sb.append( "\\newcommand{\\FMP" );
			sb.append( p.getSurface().name() );
			sb.append( p.getName() );
			sb.append( "}{\\fgcolor{" );
			sb.append( p.getLaTeXColorName() );
			sb.append( "}\\ovalbox{\\fgcolor{black}{\\jlmDynamic{FMParam" );
			sb.append( p.getSurface().name() );
			sb.append( p.getName() );
			sb.append( "}}}}\n" );
    	}
    	
    	String rem;
    	switch( s )
    	{
    	case M:
        	rem = "" +
    			"\\newcommand{\\nl}{\\\\}\n" +
    			"\\sf\\begin{array}{c}\n" +
    				"\\bf\\Large\\fgcolor{Gray}{\\text{Formula Morph}}\\\\\\\\\n" +
					"\\sf\\small\\raisebox{3ex}{\\scalebox{1}[-1]{\\jlmDynamic{FMImageF}}}\\cdot\\:(1-\\FMPMt)+\\FMPMt\\:\\cdot\\raisebox{3ex}{\\scalebox{1}[-1]{\\jlmDynamic{FMImageG}}}\n" +
    			"\\end{array}";
        	break;
    	case F:
        case G:
        	rem = "" +
    			"\\newcommand{\\nl}{\\\\}\n" +
    			"\\sf\\begin{array}{c}\n" +
    				"\\raisebox{-2em}{\\bf\\Large\\fgcolor{Gray}\\text{Formula for \\title" + s.name() +  "}}\\\\\n" +
    				"\\fgcolor{Gray}{\n" +
    					"\\left[\n" +
    					"\\fgcolor{black}{\n" + 
    						"\\begin{array}{c}\n" +
    							"\\ \\vspace{1em} \\\\\n" +
    							"{\\Large\\text{\\title" + s.name() + "}=}{\\small\\raisebox{3ex}{\\scalebox{1}[-1]{\\jlmDynamic{FMImage" + s.name() + "}}}}\\\\\\\\\n" +
    							"\\FMDynamic[i]{FMEquation" + s.name() + "}\\\\\n" +
    							"\\hphantom{MMMMMMMMMMMMMMMM}\n" +
    						"\\end{array}\n" +
    					"}\n" +
    					"\\right]\n" +
    				"}\n" +
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
        setOptimalCameraDistance( asr.getCamera() );

        for( int i = 0; i < asr.MAX_LIGHTS; i++ )
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
	
    public void idChanged( Surface s )
    {
    	try
    	{
	    	switch( s )
	    	{
	    	case F:
	    		loadFromFile( Surface.F, new File( "gallery/zitrus.jsurf" ).toURI().toURL() );
	    		break;
	    	case G:
	    		loadFromFile( Surface.G, new File( "gallery/heart.jsurf" ).toURI().toURL() );
	    		break;
	    	case M:
	    		// should not occur - do nothing
	    	}
    		s2g( s ).panel.scheduleSurfaceRepaint();
    	}
    	catch( Exception e )
    	{
    		e.printStackTrace();
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
		s2g( Surface.M ).panel.scheduleSurfaceRepaint();
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
		repaint();
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
        	double value, min, max;
        	try { value = Double.parseDouble( props.getProperty( "surface_parameter_" + name ) ); } catch( NumberFormatException nfe ) { value = Double.NaN; }
        	try { min = Double.parseDouble( props.getProperty( "surface_parametermin_" + name ) ); } catch( NumberFormatException nfe ) { min = Double.NaN; }
        	try { max = Double.parseDouble( props.getProperty( "surface_parametermax_" + name ) ); } catch( NumberFormatException nfe ) { max = Double.NaN; }

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
    		else
    		{
	        	// disable FormulaMorphParameters that are not set
	        	for( Parameter p : unsetFormulaMorphParams )
	        		p.setActive( false );
        	}
        }

        asr.getFrontMaterial().loadProperties(props, "front_material_", "");
        asr.getBackMaterial().loadProperties(props, "back_material_", "");
        
        LaTeXCommands.getDynamicLaTeXMap().put( "FMTitle" + surf.name(), props.getProperty( "surface_title_latex" ) );
        LaTeXCommands.getDynamicLaTeXMap().put( "FMEquation" + surf.name(), "\\begin{array}{c}\n" + props.getProperty( "surface_equation_latex" ).replaceAll( "\\\\FMP", "\\\\FMP" + surf.name() ).replaceAll( "\\\\\\\\", "\\\\nl" ) + "\n\\end{array}\n" );
    }

    protected static void setOptimalCameraDistance( Camera c )
    {
        float cameraDistance;
        switch( c.getCameraType() )
        {
            case ORTHOGRAPHIC_CAMERA:
                cameraDistance = 1.0f;
                break;
            case PERSPECTIVE_CAMERA:
                cameraDistance = ( float ) ( 1.0 / Math.sin( ( Math.PI / 180.0 ) * ( c.getFoVY() / 2.0 ) ) );
                break;
            default:
                throw new RuntimeException();
        }
        c.lookAt( new Point3d( 0, 0, cameraDistance ), new Point3d( 0, 0, -1 ), new Vector3d( 0, 1, 0 ) );
    }
}
