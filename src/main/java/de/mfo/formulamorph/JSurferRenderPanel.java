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

import de.mfo.jsurf.rendering.*;
import de.mfo.jsurf.rendering.cpu.*;
import de.mfo.jsurf.parser.*;
import de.mfo.jsurf.util.*;
import static de.mfo.jsurf.rendering.cpu.CPUAlgebraicSurfaceRenderer.AntiAliasingMode;

import java.awt.BorderLayout;

import java.util.concurrent.*;

/**
 * This panel displays an algebraic surface in its center. All settings of the used
 * @see{AlgebraicSurfaceRenderer} must be made by the user of this panel.
 * Only the surface an camera transformations are set automatically by this#
 * class. Changing same directly on the @see{AlgebraicSurfaceRenderer} or
 * @see{Camera} does not affect rendering at all.
 * Additionally it keeps the aspect ratio and anti-aliases the image, if there
 * is no user interaction.
 * @author Christian Stussak <christian at knorf.de>
 */
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
            if( imgbuf_final.width == 0 || imgbuf_final.height == 0 )
                return;

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
                    minPixels = Math.min( minPixels, maxPixels );
                    long numPixelsAt15FPS = ( long ) ( 1.0 / ( desired_fps * time_per_pixel ) );
                    long pixelsToUse = Math.max( minPixels, Math.min( maxPixels, numPixelsAt15FPS ) );
                    JSurferRenderPanel.this.renderSize = new Dimension( (int) Math.sqrt( pixelsToUse ), (int) Math.sqrt( pixelsToUse ) );

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
        setFocusable( true );


        this.addImageUpdateListener( new ImageUpdateListener() { 
        	public void imageUpdated( Image img ) {
        		JSurferRenderPanel.this.repaint();
        	}
        });
        rw = new RenderWorker();
        rw.start();
        
        currentSurfaceImage = null;
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

    public void setScale( double scaleFactor )
    {
        if (scaleFactor<-2.0)scaleFactor=-2.0;
        if (scaleFactor>2.0)scaleFactor=2.0;

        scaleFactor= Math.pow( 10, scaleFactor);
        //System.out.println(" scaleFactor: "+scaleFactor);
        scale.setScale( scaleFactor );
    }

    public double getScale()
    {
        //System.out.println("getScale "+this.scale.getScale()+" "+this.scale.m00+" "+(float)Math.log10(this.scale.getScale()));
        return Math.log10(this.scale.getScale());
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

    protected void paintComponent( Graphics g )
    {
        super.paintComponent( g );
        if( g instanceof Graphics2D )
        {
            final Graphics2D g2 = ( Graphics2D ) g;
            g2.setColor( this.asr.getBackgroundColor().get() );

            ImgBuffer tmpImg = currentSurfaceImage;
            if( tmpImg == null || tmpImg.width == 0 || tmpImg.height == 0 )
            {
                g2.fillRect( 0, 0, this.getWidth(), this.getHeight() );
            }
            else
            {
                BufferedImage bi = this.createBufferedImageFromRGB( tmpImg );
                final AffineTransform t = new AffineTransform();
                t.scale( this.getWidth() / (double) bi.getWidth(), -this.getHeight() / (double) bi.getHeight() );
                t.translate( 0, -bi.getHeight() );
                g2.drawImage( bi, new AffineTransformOp( t, AffineTransformOp.TYPE_BILINEAR ), 0, 0 );
            }
        }
        else
        {
            super.paintComponents( g );
            g.drawString( "this component needs a Graphics2D for painting", 2, this.getHeight() - 2 );
        }
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
