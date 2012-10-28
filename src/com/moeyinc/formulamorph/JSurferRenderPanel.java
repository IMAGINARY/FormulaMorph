/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.moeyinc.formulamorph;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.image.*;
import javax.vecmath.*;

// input/output
import java.net.URL;
import java.util.*;
import java.io.*;

import de.mfo.jsurfer.rendering.*;
import de.mfo.jsurfer.rendering.cpu.*;
import de.mfo.jsurfer.parser.*;
import de.mfo.jsurfer.util.*;
import static de.mfo.jsurfer.rendering.cpu.CPUAlgebraicSurfaceRenderer.AntiAliasingMode;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.*;
import javax.media.opengl.awt.GLJPanel;


import java.awt.BorderLayout;

import java.util.concurrent.*;

public class JSurferRenderPanel extends JComponent
{
    class ImgBuffer
    {
        public int[] rgbBuffer;
        public int width;
        public int height;

        public ImgBuffer( int w, int h ) { rgbBuffer = new int[ w * h ]; width = w; height = h; }
    }

    CPUAlgebraicSurfaceRenderer asr;
    ImgBuffer currentSurfaceImage;
    
    boolean resizeImageWithComponent;
    boolean renderCoordinatenSystem;
    Dimension renderSize;
    Dimension minLowResRenderSize;
    Dimension maxLowResRenderSize;
    RotateSphericalDragger rsd;
    Matrix4d scale;
    RenderWorker rw;
    GLJPanel glcanvas;

    class RenderWorker extends Thread
    {
        Semaphore semaphore = new Semaphore( 0 );
        boolean finish = false;
        boolean is_drawing_hi_res = false;
        double time_per_pixel = 1000.0;
        final double desired_fps = 15.0;
        boolean skip_hi_res = false;

        public void finish()
        {
            finish = true;
        }

        public void scheduleRepaint()
        {
            // schedule redraw
            semaphore.release();

            // try to ensure, that high resolution drawing is canceled
            if( is_drawing_hi_res )
                JSurferRenderPanel.this.asr.stopDrawing();
        }

        public void stopHighResolutionRendering()
        {
            semaphore.drainPermits(); // remove all currently available permits
            skip_hi_res = true;

            // try to ensure, that current high resolution rendering is canceled
            if( is_drawing_hi_res )
                JSurferRenderPanel.this.asr.stopDrawing();
        }

        void notiftyImageUpdateListeners( ImgBuffer imgbuf )
        {
        	final ImgBuffer imgbuf_final = imgbuf;
        	SwingUtilities.invokeLater( new Runnable() {
        		public void run()
        		{
                	JSurferRenderPanel.this.notifyImageUpdateListeners( imgbuf_final );        			
        		}
        	});
        }
        
        @Override
        public void run()
        {
            this.setPriority( Thread.MIN_PRIORITY );
            while( !finish )
            {
                try
                {
                    int available_permits = semaphore.availablePermits();
                    semaphore.acquire( Math.max( 1, available_permits ) ); // wait for new task and grab all permits
                    skip_hi_res = false;
                    long minPixels = JSurferRenderPanel.this.minLowResRenderSize.width * JSurferRenderPanel.this.minLowResRenderSize.height;
                    long maxPixels = JSurferRenderPanel.this.maxLowResRenderSize.width * JSurferRenderPanel.this.maxLowResRenderSize.height;
                    maxPixels = Math.max( 1, Math.min( maxPixels, JSurferRenderPanel.this.getWidth() * JSurferRenderPanel.this.getHeight() ) );
                    minPixels = Math.max( 1, Math.min( minPixels, maxPixels ) );
                    long numPixelsAt15FPS = ( long ) ( 1.0 / ( desired_fps * time_per_pixel ) );
                    long pixelsToUse = Math.max( minPixels, Math.min( maxPixels, numPixelsAt15FPS ) );
                    int widthToUse = (int) Math.sqrt( pixelsToUse );
                    widthToUse += widthToUse % 2 == 0 ? 1 : 0;
                    JSurferRenderPanel.this.renderSize = new Dimension( widthToUse, widthToUse );

                    // render low res
                    {
                        ImgBuffer ib = draw( renderSize.width, renderSize.height, AntiAliasingMode.ADAPTIVE_SUPERSAMPLING, AntiAliasingPattern.QUINCUNX, true );
                        if( ib != null )
                        {
                        	currentSurfaceImage = ib;
                            notiftyImageUpdateListeners( ib );
                        }
                    }
                    
                    if( semaphore.tryAcquire( 100, TimeUnit.MILLISECONDS ) ) // wait some time, then start with high res drawing
                    {
                        semaphore.release();
                        continue;
                    }
                    else if( skip_hi_res )
                        continue;

                    // render high res, if no new low res rendering is scheduled
                    {
                        is_drawing_hi_res = true;
                        ImgBuffer ib = draw( JSurferRenderPanel.this.getWidth(), JSurferRenderPanel.this.getHeight(), AntiAliasingMode.ADAPTIVE_SUPERSAMPLING, AntiAliasingPattern.OG_4x4, false );
                        if( ib != null )
                        {
                        	currentSurfaceImage = ib;
                            notiftyImageUpdateListeners( ib );
                        }
                        
                        is_drawing_hi_res = false;
                    }

                    if( semaphore.availablePermits() > 0 ) // restart, if user has changes the view
                        continue;
                    else if( skip_hi_res )
                        continue;

                    // render high res with even better quality
                    {
                        //System.out.println( "drawing hi res");
                        is_drawing_hi_res = true;
                        ImgBuffer ib = draw( JSurferRenderPanel.this.getWidth(), JSurferRenderPanel.this.getHeight(), AntiAliasingMode.SUPERSAMPLING, AntiAliasingPattern.OG_4x4, false );
                        if( ib != null )
                        {
                        	currentSurfaceImage = ib;
                            notiftyImageUpdateListeners( ib );
                        }

                        is_drawing_hi_res = false;
                        //System.out.println( "finised hi res");
                    }
                }
                catch( InterruptedException ie )
                {
                }
            }
        }

        public ImgBuffer draw( int width, int height, CPUAlgebraicSurfaceRenderer.AntiAliasingMode aam, AntiAliasingPattern aap )
        {
            return draw( width, height, aam, aap, false );
        }

        public ImgBuffer draw( int width, int height, CPUAlgebraicSurfaceRenderer.AntiAliasingMode aam, AntiAliasingPattern aap, boolean save_fps )
        {
        	// ensure positive size
        	width = Math.max( 1, width );
        	height = Math.max( 1, height );
        	
            // create color buffer
            ImgBuffer ib = new ImgBuffer( width, height );

            // do rendering
            /*
            Matrix4d rotation = new Matrix4d();
            rotation.invert( rsd.getRotation() );
            Matrix4d id = new Matrix4d();
            id.setIdentity();
            Matrix4d tm = new Matrix4d( rsd.getRotation() );
            tm.mul( scale );
            asr.setTransform( rsd.getRotation() );
            asr.setSurfaceTransform( scale );
            */
            asr.setAntiAliasingMode( aam );
            asr.setAntiAliasingPattern( aap );

            try
            {
                long t_start = System.nanoTime();
                asr.draw( ib.rgbBuffer, width, height );
                long t_end = System.nanoTime();
                double fps = 1000000000.0 / ( t_end - t_start );
                System.err.println( fps + "fps at " + width +"x" + height );
                if( save_fps )
                    time_per_pixel = ( ( t_end - t_start ) / 1000000000.0 ) / ( width * height );
                return ib;
            }
            catch( RenderingInterruptedException rie )
            {
            	System.err.println("\t# interrupted");
                return null;
            }
            catch( Throwable t )
            {
                t.printStackTrace();
                return null;
            }
        }
    }

    public interface  ImageUpdateListener {
    	public void imageUpdated( Image img );
    }
	private Set< ImageUpdateListener > imgListeners = new HashSet< ImageUpdateListener >();
	private boolean enableScheduleSurfaceRepaint;
	public void addImageUpdateListener( ImageUpdateListener iul ) { imgListeners.add( iul ); }
	public void removeImageUpdateListener( ImageUpdateListener iul ) { imgListeners.remove( iul ); }	
	void notifyImageUpdateListeners( ImgBuffer imgbuf )
	{
    	DirectColorModel r8g8b8_colormodel = new DirectColorModel( 24, 0xff0000, 0xff00, 0xff );
    	MemoryImageSource mis = new MemoryImageSource( imgbuf.width, imgbuf.height, r8g8b8_colormodel, imgbuf.rgbBuffer, 0, imgbuf.width );
        mis.setAnimated( true );
        mis.setFullBufferUpdates( true );    	
    	Image img = Toolkit.getDefaultToolkit().createImage( mis ); 
    					
		for( ImageUpdateListener iul : imgListeners )
			iul.imageUpdated( img );
	}
    
    public JSurferRenderPanel()
    {
        renderCoordinatenSystem = false;
        minLowResRenderSize = new Dimension( 151, 151 );
        maxLowResRenderSize = new Dimension( 513, 513 );
        //renderSize = minLowResRenderSize;

        resizeImageWithComponent = false;

        asr = new CPUAlgebraicSurfaceRenderer();

        scale = new Matrix4d();
        scale.setIdentity();

        ComponentAdapter ca = new ComponentAdapter() {
            public void componentResized( ComponentEvent ce ) { JSurferRenderPanel.this.componentResized( ce ); }
        };
        addComponentListener( ca );

        setDoubleBuffered( true );
        setFocusable( false );

        glcanvas = createGLCanvas();
        setLayout( new BorderLayout() );
        add( glcanvas, BorderLayout.CENTER );

        this.addImageUpdateListener( new ImageUpdateListener() { 
        	public void imageUpdated( Image img ) {
        		JSurferRenderPanel.this.repaint();
        	}
        });
        rw = new RenderWorker();
        rw.start();
        
        currentSurfaceImage = null;
    }

    protected GLJPanel createGLCanvas()
    {
        GLCapabilities caps = new GLCapabilities( GLProfile.get( GLProfile.GL2 ) );
        //GLCapabilities caps = new GLCapabilities( GLProfile.getDefault() );
        caps.setSampleBuffers( true );
        caps.setNumSamples( 4 );
        glcanvas = new GLJPanel( caps );
        glcanvas.addGLEventListener( new GLEventListener() {

            int textureId;

            @Override
            public void reshape( GLAutoDrawable glautodrawable, int x, int y, int width, int height ) {
                GL2 gl = glautodrawable.getGL().getGL2();
                gl.glViewport(x, y, width, height);
                gl.glMatrixMode( GL2.GL_PROJECTION );
                gl.glLoadIdentity();
                if( width > height )
                    gl.glOrtho( 0, width / ( double ) height, 0, 1, -2, 2 );
                else
                    gl.glOrtho( 0, 1, 0, height / ( double ) width, -2, 2 );
            }

            @Override
            public void init( GLAutoDrawable glautodrawable )
            {
                GL2 gl = glautodrawable.getGL().getGL2();
                
                // create texture
                int[] tmpId = new int[ 1 ];
                gl.glGenTextures( 1, tmpId, 0 );
                textureId = tmpId[ 0 ];

                // more initialization
                //gl.glClearColor( 1.0f, 0.0f, 0.0f, 1.0f );
                gl.glClearColor( 1.0f, 1.0f, 1.0f, 1.0f );
                gl.glClearDepth( 1 );
                gl.glEnable( GL2.GL_DEPTH_TEST );

                // antialias lines
                gl.glLineWidth( 2 );
                gl.glEnable( GL2.GL_LINE_SMOOTH );
                gl.glHint( GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST );
                gl.glEnable( GL2.GL_BLEND );
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

                gl.glEnable( GL2.GL_TEXTURE_2D );

                gl.glEnable( GL2.GL_CULL_FACE );
                gl.glCullFace( GL2.GL_BACK );

                gl.glShadeModel( GL2.GL_SMOOTH );

                gl.glHint( GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST );

                gl.glEnable( GL2.GL_NORMALIZE );
                gl.glEnable( GL2.GL_MULTISAMPLE );

                gl.glEnable( GL2.GL_LIGHT0 );
                gl.glDisable( GL2.GL_LIGHT1 );
                gl.glDisable( GL2.GL_LIGHT2 );
                gl.glDisable( GL2.GL_LIGHT3 );
                gl.glDisable( GL2.GL_LIGHT4 );
                gl.glDisable( GL2.GL_LIGHT5 );
                gl.glDisable( GL2.GL_LIGHT6 );
                gl.glDisable( GL2.GL_LIGHT7 );

                float ambientLight0[] = { 0.4f, 0.4f, 0.4f, 1.0f };
                float diffuseLight0[] = { 1f, 1f, 1f, 1.0f };
                float specularLight0[] = { 1f, 1f, 1f, 1.0f };

                gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambientLight0, 0 );
                gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_DIFFUSE, diffuseLight0, 0 );
                gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_SPECULAR, specularLight0, 0 );
            }

            @Override
            public void dispose( GLAutoDrawable glautodrawable ) {
            }

            private void drawCylinder( GL2 gl, double bottomRadius, double topRadius, double height )
            {
                GLU glu = new GLU();
                GLUquadric gluQuadric = glu.gluNewQuadric();
                glu.gluQuadricNormals( gluQuadric, GLU.GLU_SMOOTH );
                glu.gluCylinder( gluQuadric, bottomRadius, topRadius, height, 32, 32 );

                glu.gluQuadricOrientation( gluQuadric, GLU.GLU_INSIDE );
                glu.gluDisk( gluQuadric, 0.0, bottomRadius, 32, 5 );
                glu.gluQuadricOrientation( gluQuadric, GLU.GLU_OUTSIDE );

                gl.glPushAttrib( GL2.GL_MATRIX_MODE );
                gl.glMatrixMode( GL2.GL_MODELVIEW );
                gl.glPushMatrix();
                gl.glTranslated( 0, 0, height );
                glu.gluDisk( gluQuadric, 0.0, topRadius, 32, 5 );
                gl.glPopMatrix();
                gl.glPopAttrib();
            }

            private void drawCoordinateSystem( GL2 gl )
            {
                gl.glMatrixMode( GL2.GL_MODELVIEW );

                gl.glTranslated(1- 0.08, 0.08, 0 );
                gl.glScaled( 0.08, 0.08, 0.08 );

                Matrix4d r = new Matrix4d( JSurferRenderPanel.this.getAlgebraicSurfaceRenderer().getSurfaceTransform() );
                r.transpose();

                double[] rf = { r.m00, r.m10, r.m20, r.m30,
                               r.m01, r.m11, r.m21, r.m31,
                               r.m02, r.m12, r.m22, r.m32,
                               r.m03, r.m13, r.m23, r.m33 };

//                gl.glScaled( 1, -1, -1 );
                gl.glMultMatrixd( rf, 0 );
//                gl.glScaled( 1, -1, -1 );

                double radiusCyl = 0.04;
                double tipLength = 0.33;
                double radiusTip = 0.125;


                float ambientMatX[] = { 0.1745f, 0.01175f, 0.01175f, 1f };
                float diffuseMatX[] = { 0.61424f, 0.04136f, 0.04136f, 1f };
                float specularMatX[] = { 0.727811f, 0.626959f, 0.626959f, 1f };
                float shininessX[] = { 76.8f };

                float ambientMatY[] = { 0.01175f, 0.1745f, 0.08725f, 1f };
                float diffuseMatY[] = { 0.04136f, 0.61424f, 0.30712f, 1f };
                float specularMatY[] = { 0.626959f, 0.727811f, 0.626959f, 1f };
                float shininessY[] = { 76.8f };

                float ambientMatZ[] = { 0.01175f, 0.01175f, 0.1745f, 1f };
                float diffuseMatZ[] = { 0.04136f, 0.04136f, 0.61424f, 1f };
                float specularMatZ[] = { 0.626959f, 0.626959f, 0.727811f, 1f };
                float shininessZ[] = { 76.8f };

                gl.glEnable( GL2.GL_LIGHTING );
                gl.glDisable( GL2.GL_TEXTURE_2D );

                gl.glPushMatrix();
                gl.glBegin( GL2.GL_QUADS );
                    gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, ambientMatZ, 0 );
                    gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, diffuseMatZ, 0 );
                    gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, specularMatZ, 0 );
                    gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, shininessZ, 0 );
                gl.glEnd();
                drawCylinder( gl, radiusCyl, radiusCyl, 1.0 - tipLength ); // z-axis
                gl.glTranslated( 0, 0, 1 - tipLength );
                drawCylinder( gl, radiusTip, 0, tipLength ); // tip of y-axis
                gl.glPopMatrix();

                gl.glPushMatrix();
                gl.glRotated( 90.0, 0, 1, 0 );
                gl.glBegin( GL2.GL_QUADS );
                    gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, ambientMatX, 0 );
                    gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, diffuseMatX, 0 );
                    gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, specularMatX, 0 );
                    gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, shininessX, 0 );
                gl.glEnd();
                drawCylinder( gl, radiusCyl, radiusCyl, 1.0 - tipLength ); // x-axis
                gl.glTranslated( 0, 0, 1 - tipLength );
                drawCylinder( gl, radiusTip, 0, tipLength ); // tip of x-axis
                gl.glPopMatrix();

                gl.glPushMatrix();
                gl.glRotated( 90.0, -1, 0, 0 );
                gl.glBegin( GL2.GL_QUADS );
                    gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, ambientMatY, 0 );
                    gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, diffuseMatY, 0 );
                    gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, specularMatY, 0 );
                    gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, shininessY, 0 );
                gl.glEnd();
                drawCylinder( gl, radiusCyl, radiusCyl, 1.0 - tipLength ); // Y-axis
                gl.glTranslated( 0, 0, 1 - tipLength );
                drawCylinder( gl, radiusTip, 0, tipLength ); // tip of z-axis
                gl.glPopMatrix();
            }

            @Override
            public void display( GLAutoDrawable glautodrawable ) {
                GL2 gl = glautodrawable.getGL().getGL2();

                gl.glEnable( GL2.GL_TEXTURE_2D );
                gl.glBindTexture( GL2.GL_TEXTURE_2D, textureId );

                gl.glPixelStorei( GL2.GL_UNPACK_ALIGNMENT, 1 );

                ImgBuffer tmpImg = currentSurfaceImage;
                if( tmpImg != null )
                {
                    gl.glTexImage2D( GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, tmpImg.width, tmpImg.height, 0, GL2.GL_BGRA, GL2.GL_UNSIGNED_BYTE, java.nio.IntBuffer.wrap( tmpImg.rgbBuffer ) );
                }

                gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP );
                gl.glTexParameteri ( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP );
                gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR );
                gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR );
                Color3f bg_color = JSurferRenderPanel.this.asr.getBackgroundColor();
                float[] borderColor={ bg_color.x, bg_color.y, bg_color.z, 1.0f };
                gl.glTexParameterfv( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_BORDER_COLOR, borderColor, 0 ); // set texture border to background color to guarantee correct texture interpolation at the boundary

                gl.glTexEnvf( GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_DECAL );

                gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );

                gl.glMatrixMode( GL2.GL_MODELVIEW );
                gl.glLoadIdentity();
                
                float position0[] = { 0f, 0f, 10f, 1.0f };
                gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_POSITION, position0, 0 );

                gl.glDisable( GL2.GL_LIGHTING );
                gl.glEnable( GL2.GL_TEXTURE_2D );

                int w = glautodrawable.getWidth();
                int h = glautodrawable.getHeight();

                gl.glMatrixMode( GL2.GL_PROJECTION );
                gl.glPushMatrix();
                gl.glLoadIdentity();
                gl.glOrtho(0, w, 0, h, -2, 2 );

                gl.glBegin( GL2.GL_QUADS );
                    gl.glColor3d( 1, 1, 1 );
                    gl.glTexCoord2d( 1.0, 0.0 );
                    gl.glVertex3d( w, 0, -1.5 );
                    gl.glTexCoord2d( 1.0, 1.0 );
                    gl.glVertex3d( w, h, -1.5 );
                    gl.glTexCoord2d( 0.0, 1.0 );
                    gl.glVertex3d( 0, h, -1.5 );
                    gl.glTexCoord2d( 0.0, 0.0 );
                    gl.glVertex3d( 0, 0, -1.5 );
                gl.glEnd();
                gl.glPopMatrix();

                if( renderCoordinatenSystem )
                    drawCoordinateSystem( gl );
            }
        });
        return glcanvas;
    }

    public AlgebraicSurfaceRenderer getAlgebraicSurfaceRenderer()
    {
        return this.asr;
    }

    public void setResizeImageWithComponent( boolean resize )
    {
        resizeImageWithComponent = resize;
    }

    public boolean getResizeWithComponent()
    {
        return resizeImageWithComponent;
    }

    public void repaintImage()
    {
        scheduleSurfaceRepaint();
    }

    public Dimension getPreferredSize()
    {
        return new Dimension( minLowResRenderSize.width, minLowResRenderSize.height );
    }


    public void setMinLowResRenderSize( Dimension d )
    {
        this.minLowResRenderSize = d;
    }

    public void setMaxLowResRenderSize( Dimension d )
    {
        this.maxLowResRenderSize = d;
    }

    public Dimension getMinLowResRenderSize()
    {
        return this.minLowResRenderSize;
    }

    public Dimension getMaxLowResRenderSize()
    {
        return this.maxLowResRenderSize;
    }

    public Dimension getRenderSize()
    {
        return this.renderSize;
    }

    public void saveToPNG( java.io.File f, int width, int height )
            throws java.io.IOException
    {
        Dimension oldMinDim = getMinLowResRenderSize();
        Dimension oldMaxDim = getMaxLowResRenderSize();
        setMinLowResRenderSize( new Dimension( width, height ) );
        setMaxLowResRenderSize( new Dimension( width, height ) );
        scheduleSurfaceRepaint();
        try
        {
            saveToPNG( f, (ImgBuffer) rw.draw( width, height, CPUAlgebraicSurfaceRenderer.AntiAliasingMode.ADAPTIVE_SUPERSAMPLING, AntiAliasingPattern.OG_4x4 ) );
        }
        catch( java.util.concurrent.CancellationException ce ) {}
        setMinLowResRenderSize( oldMinDim );
        setMaxLowResRenderSize( oldMaxDim );
        scheduleSurfaceRepaint();
    }
    public void saveString(java.io.File file, java.lang.String string)
            throws java.io.IOException
    {
        java.io.FileWriter writer=new java.io.FileWriter(file ,false);
        writer.write(string);
        writer.flush();
        writer.close();
    }
    static BufferedImage createBufferedImageFromRGB( ImgBuffer ib )
    {
        int w = ib.width;
        int h = ib.height;

        DirectColorModel colormodel = new DirectColorModel( 24, 0xff0000, 0xff00, 0xff );
        SampleModel sampleModel = colormodel.createCompatibleSampleModel( w, h );
        DataBufferInt data = new DataBufferInt( ib.rgbBuffer, w * h );
        WritableRaster raster = WritableRaster.createWritableRaster( sampleModel, data, new Point( 0, 0 ) );
        return new BufferedImage( colormodel, raster, false, null );
    }

    public void saveToPNG( java.io.File f )
            throws java.io.IOException
    {
        saveToPNG( f, currentSurfaceImage );
    }

    public static void saveToPNG( java.io.File f, ImgBuffer imgbuf )
            throws java.io.IOException
    {
        BufferedImage bufferedImage = createBufferedImageFromRGB( imgbuf );
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -bufferedImage.getHeight(null));
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        bufferedImage = op.filter(bufferedImage, null);
        javax.imageio.ImageIO.write( bufferedImage, "png", f );
    }

    public void setScheduleSurfaceRepaintEnabled( boolean enabled )
    {
    	this.enableScheduleSurfaceRepaint = enabled;
    }
    
    public boolean getScheduleSurfaceRepaintEnabled()
    {
    	return this.enableScheduleSurfaceRepaint;
    }

    public void scheduleSurfaceRepaint()
    {
    	if( this.enableScheduleSurfaceRepaint )
    		rw.scheduleRepaint();
    }

    protected void componentResized( ComponentEvent ce )
    {
        scheduleSurfaceRepaint();
        repaint();
        validate();
    }
    public void drawCoordinatenSystem(boolean b)
    {
        renderCoordinatenSystem=b;
    }
}
