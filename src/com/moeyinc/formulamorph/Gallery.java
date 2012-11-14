package com.moeyinc.formulamorph;

import java.awt.Image;
import java.io.*;
import java.util.*;

import javax.vecmath.Matrix4d;


import de.mfo.jsurfer.gui.JSurfFilter;
import de.mfo.jsurfer.rendering.AlgebraicSurfaceRenderer;
import de.mfo.jsurfer.rendering.LightSource;
import de.mfo.jsurfer.rendering.cpu.AntiAliasingPattern;
import de.mfo.jsurfer.rendering.cpu.CPUAlgebraicSurfaceRenderer;
import de.mfo.jsurfer.rendering.cpu.CPUAlgebraicSurfaceRenderer.AntiAliasingMode;
import de.mfo.jsurfer.util.BasicIO;


public class Gallery
{
	public enum Level { BASIC, INTERMEDIATE, ADVANCED };
	
	public class GalleryItem extends ImageScaler
	{
		private File jsurfFile;
		private Properties jsurf;
		public GalleryItem( File f )
			throws FileNotFoundException, Exception
		{
			super();
			jsurfFile = f;
			reload();			
		}
		
		public Properties jsurfProperties() { return jsurf; }
		public Level level() { return Gallery.this.level; }
		
		public void reload()
			throws FileNotFoundException, Exception
		{
    		// load .jsurf property file
    		jsurf = new Properties();
    		jsurf.load( new FileInputStream( jsurfFile ) );
    		
    		// render thumbnail
    		CPUAlgebraicSurfaceRenderer asr = Util.loadJSurfFromProperties( jsurf );
    		Gallery.this.setDefaults( asr );
    		
    		this.setImage( Util.render( asr, 101, 101 ) );
		}
	}
	
	private Properties defaults;
	private ArrayList< GalleryItem > items;
	private List< GalleryItem > unmodifiableItems;
	private Level level;
	
	public Gallery( Level level, File folder )
		throws IOException, Exception
	{
		System.out.println( "loading " + level.name() + " gallery " + folder.getAbsolutePath() );		
		
		this.level = level;
		
		File defaultsFile = new File( folder, "defaults.jsurf" );
		System.out.println( "\tloading defaults " + defaultsFile.getParentFile().getName() + File.separator + defaultsFile.getName() );
		defaults = new Properties();
		defaults.load( new FileInputStream( defaultsFile ) );
		
		CPUAlgebraicSurfaceRenderer asr = new CPUAlgebraicSurfaceRenderer();
		
		
		items = new ArrayList< GalleryItem >();
		unmodifiableItems = Collections.unmodifiableList( items );

		File[] files = folder.listFiles( new FilenameFilter() { public boolean accept( File dir, String filename ) { return filename.endsWith( ".jsurf" ); } } );
		Arrays.sort( files, new Comparator< File >() {
			public int compare(File f0, File f1 ) {
				return f0.getName().compareTo( f1.getName() );
			}
		});
		
	    for( final File fileEntry : files )
	    {
	        if(fileEntry.isFile() && !fileEntry.getName().equals("defaults.jsurf") )
	        {
	        	try
	        	{
	        		System.out.println( "\tloading " + fileEntry.getParentFile().getName() + File.separator + fileEntry.getName() );
	        		items.add( new GalleryItem( fileEntry ) );
	        	}
	        	catch( FileNotFoundException fnfe )
	        	{
	        		System.out.println( "\t" + fileEntry.getParentFile().getName() + File.separator + fileEntry.getName() + " not found" );
	        		// do nothing .. maybe the file has been removed
	        	}
	        }
	    }
	}
	
	public Level level() { return level; }
	
	public List< GalleryItem > getItems()
	{
		return unmodifiableItems;
	}
	
	private void listFilesForFolder(final File folder) {
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            listFilesForFolder(fileEntry);
	        } else {
	            System.out.println(fileEntry.getName());
	        }
	    }
	}

    protected void setDefaults( CPUAlgebraicSurfaceRenderer asr )
    {
        asr.getCamera().loadProperties( defaults, "camera_", "" );
        Util.setOptimalCameraDistance( asr.getCamera() );

        for( int i = 0; i < AlgebraicSurfaceRenderer.MAX_LIGHTS; i++ )
        {
            asr.getLightSource( i ).setStatus(LightSource.Status.OFF);
            asr.getLightSource( i ).loadProperties( defaults, "light_", "_" + i );
        }
        asr.setBackgroundColor( BasicIO.fromColor3fString( defaults.getProperty( "background_color" ) ) );

//        identity.setIdentity();
//        asr.setTransform( BasicIO.fromMatrix4dString( defaults.getProperty( "rotation_matrix" ) ) );
        Matrix4d scaleMatrix = new Matrix4d();
        scaleMatrix.setIdentity();
        scaleMatrix.setScale( 1.0 / Double.parseDouble( defaults.getProperty( "scale_factor" ) ) );
        asr.setSurfaceTransform( scaleMatrix );
        
		asr.setAntiAliasingMode( AntiAliasingMode.ADAPTIVE_SUPERSAMPLING );
		asr.setAntiAliasingPattern( AntiAliasingPattern.RG_2x2 );        
    }	
}
