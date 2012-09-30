package com.moeyinc.formulamorph;

import javax.swing.JComponent;
import javax.swing.ImageIcon;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class ImageScaler extends JComponent {

	private Image image;
	private Image scaledImage;
	private boolean grayscale; 
	
	public ImageScaler()
	{
		this( null );
	}

	public ImageScaler( Image image )
	{
		super();
		this.image = image;
		this.grayscale = false;
	}
	
	public void setImage( Image image )
	{
		this.image = image;
		this.scaledImage = null;
		this.setPreferredSize( new Dimension( image.getWidth( null ), image.getHeight( null ) ) );
		this.repaint();
	}
	
	public Image getImage() { return this.image; }
	
	public void setGrayScale( boolean grayscale ) { this.grayscale = grayscale; this.scaledImage = null; this.repaint(); }
	public boolean getGrayScale() { return this.grayscale; }
	
    public void paintComponent(Graphics g)
    { 
    	if( this.isOpaque() )
    	{
    		g.setColor( this.getBackground() );
    		g.fillRect( 0, 0, this.getWidth(), this.getHeight() );
    	}
        super.paintComponent (g); 
        if( image != null )
        {
        	if( scaledImage == null || scaledImage.getWidth( null ) != image.getWidth( null ) || scaledImage.getHeight( null ) != image.getHeight( null ) )
        	{
        		scaledImage = image.getScaledInstance( getWidth(), getHeight(), Image.SCALE_SMOOTH );
        		if( grayscale )
        		{
        			BufferedImage bi = new BufferedImage( getWidth(), getHeight(), BufferedImage.TYPE_BYTE_GRAY );
        			Graphics g_bi = bi.getGraphics();  
        			g_bi.drawImage( scaledImage, 0, 0, null);  
        			g_bi.dispose();
        			scaledImage = bi;
        		}
        	}
        	g.drawImage( scaledImage, 0, 0, getWidth(), getHeight(), null ); 
        }
    }
}
