package com.moeyinc.formulamorph;

import javax.swing.JComponent;
import javax.swing.ImageIcon;
import java.awt.Graphics;
import java.awt.Image;

public class ImageScaler extends JComponent {

	private Image image;
	private Image scaledImage;
	
	public ImageScaler()
	{
		this( null );
	}

	public ImageScaler( Image image )
	{
		super();
		this.image = image;
	}
	
	public void setImage( Image image ) { this.image = image; this.scaledImage = null; }
	public Image getImage() { return this.image; }
	
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
        		scaledImage = image.getScaledInstance( getWidth(), getHeight(), Image.SCALE_SMOOTH );
        	g.drawImage( scaledImage, 0, 0, getWidth(), getHeight(), null ); 
        }
    }
}
