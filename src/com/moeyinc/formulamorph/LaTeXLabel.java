package com.moeyinc.formulamorph;

import java.awt.Graphics; 
import java.awt.Insets;
import javax.swing.JComponent;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

public class LaTeXLabel extends JComponent
{
	private String texSrc;
	private TeXFormula texFormula;
	private Insets insets;
	
	
	public LaTeXLabel( String laTeXSrc ) { super(); this.setInsets(0,0,0,0); this.setLaTeXSrc( laTeXSrc ); }
	public LaTeXLabel() { this( "" ); };
	
	public void setLaTeXSrc( String laTeXSrc )
	{
		this.texSrc = laTeXSrc;
		try
		{
			texFormula = new TeXFormula( this.texSrc );
		}
		catch( Exception e )
		{
			System.out.println( "Problem with LaTeX source:" );
			System.out.println( laTeXSrc );
			e.printStackTrace();
		}
		this.repaint();
	}
	
	public void reparse()
	{
		this.setLaTeXSrc( this.texSrc );
	}
	
	public void setInsets( int top, int left, int bottom, int right )
	{
		if( insets == null )
			insets = new Insets( top, left, bottom, right );
		else
			this.insets.set( top, left, bottom, right );
		repaint();
	}
	
	public void setInsets( Insets insets )
	{
		this.setInsets( insets.top, insets.left, insets.bottom, insets.right );
	}
	
	public void paint( Graphics g )
	{
		super.paint( g );
		if( this.isOpaque() )
		{
			g.setColor( this.getBackground() );
			g.fillRect(0, 0, this.getWidth(), this.getHeight() );			
		}
		
		// scale font size according to ratio of actual size vs. preferred size
		float xScale = this.getPreferredSize() != null ? this.getWidth() / ( float ) this.getPreferredSize().width : 1.0f; 
		float yScale = this.getPreferredSize() != null ? this.getHeight() / ( float ) this.getPreferredSize().height : 1.0f;
		float scale = ( xScale < yScale ? xScale : yScale );
//		System.out.println( scale );
		TeXIcon texIcon = texFormula.createTeXIcon( TeXConstants.STYLE_TEXT, ( int ) ( 36.5f * scale ) );
		texIcon.setInsets( this.getInsets() );
		texIcon.paintIcon( this, g, 0, 0 );
	}
}
