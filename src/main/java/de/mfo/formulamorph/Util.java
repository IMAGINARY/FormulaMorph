package de.mfo.formulamorph;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.Map;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;


import de.mfo.jsurf.rendering.*;
import de.mfo.jsurf.rendering.cpu.CPUAlgebraicSurfaceRenderer;
import de.mfo.jsurf.util.BasicIO;

public class Util
{

    public static CPUAlgebraicSurfaceRenderer loadJSurfFromString( String s )
    	throws Exception
    {
    	CPUAlgebraicSurfaceRenderer asr = new CPUAlgebraicSurfaceRenderer();
    	loadJSurfFromString( asr, s );
    	return asr;
    }
	
    public static CPUAlgebraicSurfaceRenderer loadJSurfFromFile( URL url )
        	throws Exception
        {
        	CPUAlgebraicSurfaceRenderer asr = new CPUAlgebraicSurfaceRenderer();
        	loadJSurfFromFile( asr, url );
        	return asr;
        }

    public static CPUAlgebraicSurfaceRenderer loadJSurfFromProperties( Properties props )
        	throws Exception
        {
        	CPUAlgebraicSurfaceRenderer asr = new CPUAlgebraicSurfaceRenderer();
        	loadJSurfFromProperties( asr, props );
        	return asr;
        }

    public static void loadJSurfFromString( AlgebraicSurfaceRenderer asr, String s )
            throws Exception
    {
        Properties props = new Properties();
        props.load( new ByteArrayInputStream( s.getBytes() ) );
        loadJSurfFromProperties( asr, props );
    }

    public static void loadJSurfFromFile( AlgebraicSurfaceRenderer asr, URL url )
            throws IOException, Exception
    {
        Properties props = new Properties();
        props.load( url.openStream() );
        loadJSurfFromProperties( asr, props );
    }
    
    public static void loadJSurfFromProperties( AlgebraicSurfaceRenderer asr, Properties props )
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
                //System.out.println("LoadRenderPar: " + parameterName + "=" + entry.getValue() + " (" + Float.parseFloat( (String) entry.getValue()) + ") "+ asr.getParameterValue( parameterName));
            }
        }

        asr.getCamera().loadProperties( props, "camera_", "" );
        asr.getFrontMaterial().loadProperties(props, "front_material_", "");
        asr.getBackMaterial().loadProperties(props, "back_material_", "");
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
    
    public static void setOptimalCameraDistance( Camera c )
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
    
    public static BufferedImage render( AlgebraicSurfaceRenderer asr, int width, int height )
    {
    	int[] rgbBuffer = new int[ width * height ];
    	
    	asr.draw( rgbBuffer, width, height);

        DirectColorModel colormodel = new DirectColorModel( 24, 0xff0000, 0xff00, 0xff );
        SampleModel sampleModel = colormodel.createCompatibleSampleModel( width, height );
        DataBufferInt data = new DataBufferInt( rgbBuffer, width * height );
        WritableRaster raster = WritableRaster.createWritableRaster( sampleModel, data, new Point( 0, 0 ) );
        BufferedImage b = new BufferedImage( colormodel, raster, false, null ); 
        
        AffineTransform flipV = AffineTransform.getScaleInstance(1, -1);
		flipV.translate(0, -b.getHeight(null));
		AffineTransformOp flipVOp = new AffineTransformOp( flipV, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		
        return flipVOp.filter( b, null );
    }
}
