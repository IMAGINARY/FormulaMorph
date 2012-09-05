package com.moeyinc.formulamorph;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.vecmath.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.moeyinc.formulamorph.Parameters.ActiveParameterListener;
import com.moeyinc.formulamorph.Parameters.Name;
import com.moeyinc.formulamorph.Parameters.Surface;

import de.mfo.jsurfer.rendering.Camera;
import de.mfo.jsurfer.rendering.LightSource;
import de.mfo.jsurfer.util.BasicIO;
import de.mfo.jsurfer.rendering.AlgebraicSurfaceRenderer;;

public class GUI extends JFrame
{
	JPanel content; // fixed 16:9 top container
	static final double aspectRatio = 16.0 / 9.0;

	JSurferRenderPanel surfF;
	JSurferRenderPanel surfM; // morphed surface
	JSurferRenderPanel surfG;
	
	LaTeXLabel equationLaTeX;
	
	JPanel galF;
	JPanel galG;

	public GUI()
	{
		super( "FormulaMorph Main Window" );

		// setup the contained which has fixed 16:9 aspect ratio
		content = new JPanel();
		content.setBackground(Color.white);
		content.setLayout( null ); 

		// init components
		
		try {
			surfF = new JSurferRenderPanel(); //surfF.setBackground( Color.gray );
			loadFromFile( surfF.getAlgebraicSurfaceRenderer(), new File( "gallery/barthsextic.jsurf" ).toURI().toURL() );
			surfM = new JSurferRenderPanel(); //surfM.setBackground( Color.gray );
			loadFromFile( surfM.getAlgebraicSurfaceRenderer(), new File( "gallery/barthsextic.jsurf" ).toURI().toURL() );
			surfG = new JSurferRenderPanel(); //surfG.setBackground( Color.gray );
			loadFromFile( surfG.getAlgebraicSurfaceRenderer(), new File( "gallery/heart.jsurf" ).toURI().toURL() );
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		equationLaTeX = new LaTeXLabel( setupLaTeXSrc() );
 //equationLaTeX.setBackground( Color.gray ); equationLaTeX.setOpaque( true );

		galF = new JPanel(); galF.setBackground( Color.lightGray );
		galG = new JPanel(); galG.setBackground( Color.lightGray );

		// add components
		content.add( surfF );
		content.add( surfM );
		content.add( surfG );
		
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
	}

	public void refreshLayout()
	{
		surfF.setBounds( computeBounds( content, 10, 5 * aspectRatio, 23, 23 * aspectRatio ) );
		surfM.setBounds( computeBounds( content, 50 - ( 25 / 2 ), 3 * aspectRatio, 25, 25 * aspectRatio ) ); // center horizontally
		surfG.setBounds( computeBounds( content, 100 - 10 - 23, 5 * aspectRatio, 23, 23 * aspectRatio ) );

		double elPrefWidth = 95.0;
		double elPrefHeight = 15 * aspectRatio;
		equationLaTeX.setPreferredSize( new Dimension( (int) (elPrefWidth * 1920 / 100.0), (int) (elPrefHeight * 1080 / 100.0) ) );
		equationLaTeX.setBounds( computeBounds( content, 50 - elPrefWidth / 2, 65, elPrefWidth, elPrefHeight ) ); // center horizontally

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
    
    private static final String staticLaTeXSrc = "" +
			"\\newcommand{\\nl}{\\\\\\sf}\n" +
			"\\begin{array}{rcl@{}c@{}rcl}\n" +
			"\\left[\\vspace{7em} \\right.\n" +
			"&\n" +
			"\\begin{array}{c}\n" +
			"	\\sf \\formulaF\n" +
			"\\end{array}\n" +
			"&\n" +
			"\\left.\\vspace{7em} \\right]\n" +
			"&\n" +
			"\\sf\\cdot\\ \\pT+(1-\\pT)\\:\\cdot\n" +
			"&\n" +
			"\\left[\\vspace{7em} \\right.\n" +
			"&\n" +
			"\\begin{array}{c}\n" +
			"	\\sf \\formulaG\n" +
			"\\end{array}\n" +
			"&\n" +
			"\\left.\\vspace{7em} \\right]\n" +
			"\\\\\n" +
			"&\\hspace{30em}&&&&\\hspace{30em}&\n" +
			"\\end{array}\n";
    
    private String setupLaTeXSrc()
    {
    	String result = "" +
    			"\\newcommand{\\valAl}{0.01}\n" +
    			"\\newcommand{\\valT}{0.25}\n" +
    			"\\newcommand{\\pAl}{\\colorbox{yellow}{\\valAl}}\n" +
    			"\\newcommand{\\pT}{\\colorbox{red}{\\valT}}\n" +
    			"\\newcommand{\\formulaF}{4((\\frac{1+\\sqrt{5}}{2})^2x^2-y^2)\\cdot((\\frac{1+\\sqrt{5}}{2})^2y^2-z^2)\\nl\\cdot((\\frac{1+\\sqrt{5}}{2})^2z^2-x^2)-(2+\\sqrt{5})\\nl\\cdot(x^2+y^2+z^2-1)^2-\\pAl}\n" +
    			"\\newcommand{\\formulaG}{xyz+x^2+y^2+z^2+1}\n" +
    			staticLaTeXSrc;
    	return result;
    }
    
	public void setParameterValue( Name name, double value )
	{
		
	}

	private Set< ActiveParameterListener > apls; 
	public void addActiveParameterListener( ActiveParameterListener apl ) { apls.add( apl ); }
	public void removeActiveParameterListener( ActiveParameterListener apl ) { apls.remove( apl ); }	
	
	public void setSurface( Surface surface, int index_in_gallery ) {}
    

    public void loadFromString( AlgebraicSurfaceRenderer asr, String s )
            throws Exception
    {
        Properties props = new Properties();
        props.load( new ByteArrayInputStream( s.getBytes() ) );
        loadFromProperties( asr, props );
    }

    public void loadFromFile( AlgebraicSurfaceRenderer asr, URL url )
            throws IOException, Exception
    {
        Properties props = new Properties();
        props.load( url.openStream() );
        loadFromProperties( asr, props );
    }

    public void loadFromProperties( AlgebraicSurfaceRenderer asr, Properties props )
            throws Exception
    {
        asr.setSurfaceFamily( props.getProperty( "surface_equation" ) );

        Set< Map.Entry< Object, Object > > entries = props.entrySet();
        String parameter_prefix = "surface_parameter_";
        for( Map.Entry< Object, Object > entry : entries )
        {
            String name = (String) entry.getKey();
            if( name.startsWith( parameter_prefix ) )
            {
                String parameterName = name.substring( parameter_prefix.length() );
                asr.setParameterValue( parameterName, Float.parseFloat( ( String ) entry.getValue() ) );
                System.out.println("LoadRenderPar: " + parameterName + "=" + entry.getValue() + " (" + Float.parseFloat( (String) entry.getValue()) + ") "+ asr.getParameterValue( parameterName));
            }
        }

        asr.getCamera().loadProperties( props, "camera_", "" );
        setOptimalCameraDistance( asr.getCamera() );
        asr.getFrontMaterial().loadProperties(props, "front_material_", "");
        asr.getBackMaterial().loadProperties(props, "back_material_", "");
        for( int i = 0; i < asr.MAX_LIGHTS; i++ )
        {
            asr.getLightSource( i ).setStatus(LightSource.Status.OFF);
            asr.getLightSource( i ).loadProperties( props, "light_", "_" + i );
        }
        asr.setBackgroundColor( BasicIO.fromColor3fString( props.getProperty( "background_color" ) ) );
        asr.setTransform( BasicIO.fromMatrix4dString( props.getProperty( "rotation_matrix" ) ) );
        Matrix4d scaleMatrix = new Matrix4d();
        scaleMatrix.setIdentity();
        scaleMatrix.setScale( 1.0 / Double.parseDouble( props.getProperty( "scale_factor" ) ) );
        asr.setSurfaceTransform( scaleMatrix );
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
    
    public void saveToFile( AlgebraicSurfaceRenderer asr, URL url )
            throws IOException
    {
        Properties props = new Properties();
        props.setProperty( "surface_equation", asr.getSurfaceFamilyString() );

        Set< String > paramNames = asr.getAllParameterNames();
        for( String paramName : paramNames )
        {
            try
            {
                props.setProperty( "surface_parameter_" + paramName, "" + asr.getParameterValue( paramName ) );
            }
            catch( Exception e ) {}
        }

        asr.getCamera().saveProperties( props, "camera_", "" );
        asr.getFrontMaterial().saveProperties(props, "front_material_", "");
        asr.getBackMaterial().saveProperties(props, "back_material_", "");
        for( int i = 0; i < asr.MAX_LIGHTS; i++ )
            asr.getLightSource( i ).saveProperties( props, "light_", "_" + i );
        props.setProperty( "background_color", BasicIO.toString( asr.getBackgroundColor() ) );

        //props.setProperty( "scale_factor", ""+this.getScale() );
        //props.setProperty( "rotation_matrix", BasicIO.toString( rsd.getRotation() ));

        File property_file = new File( url.getFile() );
        props.store( new FileOutputStream( property_file ), "jSurfer surface description" );
    }
	
	
}
