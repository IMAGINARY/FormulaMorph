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
	final ControllerAdapterGUI caGUI = new ControllerAdapterGUI( null );
	JPanel content; // fixed 16:9 top container
	static final double aspectRatio = 16.0 / 9.0;

	EnumMap< Parameters.Surface, JSurferRenderPanel > surface2panel = new EnumMap< Parameters.Surface, JSurferRenderPanel >( Parameters.Surface.class );
	EnumMap< Parameters.Surface, String > surface2latex = new EnumMap< Parameters.Surface, String>( Parameters.Surface.class );
	
	final LaTeXLabel titleFLaTeX;
	final LaTeXLabel titleGLaTeX;
	final LaTeXLabel equationLaTeX;
	
	JPanel galF;
	JPanel galG;

	public GUI()
	{
		super( "FormulaMorph Main Window" );
	
		caGUI.setDefaultCloseOperation( HIDE_ON_CLOSE );
		this.addMouseListener( new MouseAdapter() { public void mouseClicked( MouseEvent e ) { caGUI.setVisible( true ); } } );
		
		for( Parameter p : Parameter.values() )
			p.addValueChangeListener( this );

		// setup the contained which has fixed 16:9 aspect ratio
		content = new JPanel();
		content.setBackground(Color.white);
		content.setLayout( null );

		// init components
		
		surface2panel.put( Surface.F, new JSurferRenderPanel() ); //surfF.setBackground( Color.gray );
		surface2panel.put( Surface.M, new JSurferRenderPanel() ); //surfM.setBackground( Color.gray );
		surface2panel.put( Surface.G, new JSurferRenderPanel() ); //surfG.setBackground( Color.gray );

		Border border = BorderFactory.createLineBorder( Color.LIGHT_GRAY );
		for( Surface s : Surface.values() )
			surface2panel.get( s ).setBorder(border);
		
		LaTeXCommands.getImageMap().put( "imageF", null );
		LaTeXCommands.getImageMap().put( "imageG", null );
		titleFLaTeX = new LaTeXLabel( "\\sf\\huge\\text{Zitrus}" ); titleFLaTeX.setBackground( Color.gray ); titleFLaTeX.setOpaque( true );
		titleGLaTeX = new LaTeXLabel( "\\sf\\huge\\text{Heart}" ); titleGLaTeX.setBackground( Color.gray ); titleGLaTeX.setOpaque( true );
		equationLaTeX = new LaTeXLabel( setupLaTeXSrc() );

		javax.swing.JFrame f = new JFrame();
		f.setDefaultCloseOperation( HIDE_ON_CLOSE );
		f.getContentPane().add( new LaTeXLabel("123") );
		f.setMinimumSize( new Dimension( 100, 100 ) );
		f.pack();
		f.setVisible( true );
		
 //equationLaTeX.setBackground( Color.gray ); equationLaTeX.setOpaque( true );

		galF = new JPanel(); galF.setBackground( Color.lightGray );
		galG = new JPanel(); galG.setBackground( Color.lightGray );

		surface2panel.get( Surface.F ).addImageUpdateListener( new JSurferRenderPanel.ImageUpdateListener() {
			public void imageUpdated( Image img )
			{
				LaTeXCommands.getImageMap().put( "imageF", img );
				equationLaTeX.setLaTeXSrc( setupLaTeXSrc() );
			}
		});		
		surface2panel.get( Surface.G ).addImageUpdateListener( new JSurferRenderPanel.ImageUpdateListener() {
			public void imageUpdated( Image img )
			{
				LaTeXCommands.getImageMap().put( "imageG", img );
				equationLaTeX.setLaTeXSrc( setupLaTeXSrc() );
			}
		});
		
		// add components
		content.add( surface2panel.get( Surface.F ) );
		content.add( surface2panel.get( Surface.M ) );
		content.add( surface2panel.get( Surface.G ) );
		
		content.add( titleFLaTeX );
		content.add( titleGLaTeX );
		content.add( equationLaTeX );

		content.add( galF );
		content.add( galG );

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
	
	JSurferRenderPanel surfF() { return surface2panel.get( Surface.F ); }
	JSurferRenderPanel surfM() { return surface2panel.get( Surface.M ); }
	JSurferRenderPanel surfG() { return surface2panel.get( Surface.G ); }

	public void refreshLayout()
	{
		surfF().setBounds( computeBounds( content, 10, 5 * aspectRatio, 23, 23 * aspectRatio ) );		
		surfM().setBounds( computeBounds( content, 50 - ( 25 / 2 ), 3 * aspectRatio, 25, 25 * aspectRatio ) ); // center horizontally
		surfG().setBounds( computeBounds( content, 100 - 10 - 23, 5 * aspectRatio, 23, 23 * aspectRatio ) );

		titleFLaTeX.setBounds( computeBounds( content, 10, 2 * aspectRatio, 23, 2.75 * aspectRatio ) );
		titleGLaTeX.setBounds( computeBounds( content, 100 - 10 -23, 2 * aspectRatio, 23, 2.75 * aspectRatio ) );

		double elPrefWidth = 95.0;
		double elPrefHeight = 35.0 * aspectRatio;
		equationLaTeX.setPreferredSize( new Dimension( (int) (elPrefWidth * 1920 / 100.0), (int) (elPrefHeight * 1080 / 100.0) ) );
		equationLaTeX.setBounds( computeBounds( content, 50 - elPrefWidth / 2, 55, elPrefWidth, elPrefHeight ) ); // center horizontally

		galF.setBounds( computeBounds( content, 2, 5 * aspectRatio, 5, 25 * aspectRatio ) );
		galG.setBounds( computeBounds( content, 100 - 2 - 5, 5 * aspectRatio, 5, 25 * aspectRatio ) );

		repaint();
	}

	private static Rectangle computeBounds( Component p, double x, double y, double w, double h )
	{
		x = x / 100; y = y / 100; w = w / 100; h = h / 100;
		return new Rectangle( (int) ( p.getWidth() * x ), (int) ( p.getHeight() * y ), (int) ( p.getWidth() * w ), (int) ( p.getHeight() * h ) );
	}

	protected void hideCursor()
	{
		// Transparent 16 x 16 pixboolean fullscreenel cursor image.
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
    	BufferedImage image = new BufferedImage( getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB );
        Graphics2D graphics2D = image.createGraphics();
        this.equationLaTeX.reparse();
        this.paint( graphics2D );
        javax.imageio.ImageIO.write( image, "png", out );    	
    }
    
    private static final String staticLaTeXSrc = "" +
			"\\newcommand{\\nl}{\\\\\\sf\\small}\n" +
			"\\begin{array}{rcl@{}c@{}rcl}\n" +
			"&\\sf\\small\\text{\\hphantom{MMMMMMMMMMMMMMMMMMMM}}&&&&\\sf\\small\\ \\hspace{20em}\\ &\\\\" +
			"\\fgcolor{Gray}{\\left[\\vspace{12em} \\right.}\n" +
			"&\n" +
			"\\begin{array}{c}\n" +
			"	\\sf\\bf\\Large\\fgcolor{Gray}\\text{Formula for \\titleF}\\\\\\\\" +
			"	\\sf\\bf\\Large\\text{\\titleF}={\\sf\\small\\raisebox{3ex}{\\scalebox{1}[-1]{\\includejavaimage[width=5ex,interpolation=bicubic]{imageF}}}}\\\\\\\\" +
			"	\\sf\\small\\formulaF\n" +
			"\\end{array}\n" +
			"&\n" +
			"\\fgcolor{Gray}{\\left.\\vspace{12em} \\right]}\n" +
			"&\n" +
			"\\sf\\small\\raisebox{3ex}{\\scalebox{1}[-1]{\\includejavaimage[width=5ex,interpolation=bicubic]{imageF}}}\\cdot\\ \\FMPMt+(1-\\FMPMt)\\:\\cdot\\raisebox{3ex}{\\scalebox{1}[-1]{\\includejavaimage[width=5ex,interpolation=bicubic]{imageG}}}\n" +
			"&\n" +
			"\\fgcolor{Gray}{\\left[\\vspace{12em} \\right.}\n" +
			"&\n" +
			"\\begin{array}{c}\n" +
			"	\\sf\\bf\\Large\\fgcolor{Gray}\\text{Formula for \\titleG}\\\\\\\\" +
			"	\\sf\\bf\\Large\\text{\\titleG}={\\sf\\small\\raisebox{3ex}{\\scalebox{1}[-1]{\\includejavaimage[width=5ex,interpolation=bicubic]{imageG}}}}\\\\\\\\" +
			"	\\sf\\small\\formulaG\n" +
			"\\end{array}\n" +
			"&\n" +
			"\\fgcolor{Gray}{\\left.\\vspace{12em} \\right]}\n" +
			"\\\\\n" +
			"&\\hspace{30em}&&&&\\hspace{30em}&\n" +
			"\\end{array}\n";
    
    private String setupLaTeXSrc()
    {    	
    	titleFLaTeX.reparse();
    	
    	DecimalFormat f = new DecimalFormat("0.00");
    	StringBuilder sb = new StringBuilder();
    	
    	// surface names
    	sb.append( "\\newcommand{\\titleF}{");
    	sb.append( "Zitrus" );
    	sb.append( "}\n\\newcommand{\\titleG}{" );
    	sb.append( "Heart" );
    	sb.append( "}\n" );

    	// parameters
    	for( Parameter p : Parameter.values() )
    	{
    		if( p.isActive() )
    		{
    			sb.append( p.getLaTeXColorDefinition() );
    			sb.append( "\\newcommand{\\FMP" );
    			sb.append( p.getSurface().name() );
    			sb.append( p.getName() );
    			sb.append( "}{\\fgcolor{" );
    			sb.append( p.getLaTeXColorName() );
    			sb.append( "}\\ovalbox{\\fgcolor{black}{" );
    			sb.append( f.format( p.getValue() ) );
    			sb.append( "}}}\n" );
    		}
    	}
    	for( Surface s : Surface.values() )
    	{
    		sb.append( "\\newcommand{\\formula" );
    		sb.append( s.name() );
    		sb.append( "}{" );
    		sb.append( surface2latex.get( s ) );
    		sb.append( "}\n" );
    	}
    	//sb.append("\\begin{array}{c}\\formulaF \\\\ \\FMPMt \\\\ \\formulaG\\end{array}");
    	//sb.append( "a" );
    	sb.append( staticLaTeXSrc );
    	return sb.toString();
    }
    
    public void setDefaults( Surface surf, Properties props )
    {
    	AlgebraicSurfaceRenderer asr = surface2panel.get( surf ).getAlgebraicSurfaceRenderer();

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
    	JSurferRenderPanel jsp = surface2panel.get( p.getSurface() );
    	jsp.getAlgebraicSurfaceRenderer().setParameterValue( Character.toString( p.getName() ), p.getValue() );
    	jsp.scheduleSurfaceRepaint();

    	AlgebraicSurfaceRenderer asr_M =  surface2panel.get( Surface.M ).getAlgebraicSurfaceRenderer();
    	if( p.getSurface() == Surface.M )
    	{
    		// interpolate colors of the morph
    		float t = (float) Parameter.M_t.getValue();
    		asr_M.setParameterValue( Character.toString( p.getName() ), p.getValue() );

    		AlgebraicSurfaceRenderer asr_F =  surface2panel.get( Surface.F ).getAlgebraicSurfaceRenderer();
    		AlgebraicSurfaceRenderer asr_G =  surface2panel.get( Surface.G ).getAlgebraicSurfaceRenderer();
    		
    		asr_M.setFrontMaterial( Material.lerp( asr_F.getFrontMaterial(), asr_G.getFrontMaterial(), t ) );
    		asr_M.setBackMaterial( Material.lerp( asr_F.getBackMaterial(), asr_G.getBackMaterial(), t ) );
    	}
    	else
    	{
    		// the parameter at morph, too
    		asr_M.setParameterValue( p.name(), p.getValue() );
    	}
		surface2panel.get( Surface.M ).scheduleSurfaceRepaint();
		equationLaTeX.setLaTeXSrc( setupLaTeXSrc() );
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
    		surface2panel.get( s ).scheduleSurfaceRepaint();
    	}
    	catch( Exception e )
    	{
    		e.printStackTrace();
    	}

    	// set the morphed surface
    	AlgebraicSurfaceRenderer asr_F = surface2panel.get( Surface.F ).getAlgebraicSurfaceRenderer();
    	AlgebraicSurfaceRenderer asr_M = surface2panel.get( Surface.M ).getAlgebraicSurfaceRenderer();
    	AlgebraicSurfaceRenderer asr_G = surface2panel.get( Surface.G ).getAlgebraicSurfaceRenderer();
    	
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
		surface2panel.get( Surface.M ).scheduleSurfaceRepaint();
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
		equationLaTeX.setLaTeXSrc( setupLaTeXSrc() );
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
    	AlgebraicSurfaceRenderer asr = surface2panel.get( surf ).getAlgebraicSurfaceRenderer();
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
        
        System.out.println( props.getProperty( "surface_equation_latex" ) );
        System.out.println( props.getProperty( "surface_equation_latex" ).replaceAll( "\\\\FMP", "\\\\FMP" + surf.name() ).replaceAll( "\\\\\\\\", "\\\\nl" ) );
        surface2latex.put( surf, props.getProperty( "surface_equation_latex" ).replaceAll( "\\\\FMP", "\\\\FMP" + surf.name() ).replaceAll( "\\\\\\\\", "\\\\nl" ) );
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
