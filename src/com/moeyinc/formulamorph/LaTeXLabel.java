package com.moeyinc.formulamorph;

import java.awt.Graphics; 
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

public class LaTeXLabel extends JComponent
{		
	public enum HAlignment { LEFT, CENTER, RIGHT };
	public enum VAlignment { BOTTOM, CENTER, CENTER_BASELINE, TOP };
	
	private boolean reparseSrcOnRepaint = false;
	private String texSrc;
	private TeXFormula texFormula;
	private Insets insets;
	private HAlignment halign = HAlignment.CENTER;
	private VAlignment valign = VAlignment.CENTER;
	
	public LaTeXLabel( String laTeXSrc ) { super(); this.setInsets(0,0,0,0); this.setLaTeXSrc( laTeXSrc ); }
	public LaTeXLabel() { this( "" ); };
	
	public void setLaTeXSrc( String laTeXSrc ) { setLaTeXSrc( laTeXSrc, true ); }
	public void setLaTeXSrc( String laTeXSrc, boolean repaint )
	{
		try
		{
			this.texSrc = laTeXSrc;
			texFormula = new TeXFormula( laTeXSrc );
		}
		catch( Exception e )
		{
			System.out.println( "Problem with LaTeX source:" );
			System.out.println( laTeXSrc );
			e.printStackTrace();
		}
		if( repaint )
			this.repaint();
	}
	
	public void reparse()
	{
		this.setLaTeXSrc( this.texSrc );
	}
	
	public void reparseOnRepaint()
	{
		reparseSrcOnRepaint = true;
	}
	
	public void setHAlignment( HAlignment halign ) { this.halign = halign; repaint(); }
	public void setVAlignment( VAlignment halign ) { this.valign = valign; repaint(); }
	public HAlignment getHAlignment() { return halign; }
	public VAlignment getVAlignment() { return valign; }
	
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
		if( reparseSrcOnRepaint )
		{
			this.setLaTeXSrc( this.texSrc, false );
			reparseSrcOnRepaint = false;
		}
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
		TeXIcon texIcon = texFormula.createTeXIcon( TeXConstants.STYLE_TEXT, 30f * scale );
		texIcon.setInsets( this.getInsets() );
		
		int x, y;
		switch( halign )
		{
			case LEFT: x = 0; break;
			case RIGHT: x = this.getWidth() - texIcon.getIconWidth(); break;
			case CENTER:
			default: x = ( this.getWidth() - texIcon.getIconWidth() ) / 2; break;
		}
		switch( valign )
		{
			case TOP: y = 0; break;
			case BOTTOM: y = this.getHeight() - texIcon.getIconHeight(); break;
			case CENTER_BASELINE: y = ( this.getHeight() - texIcon.getIconHeight() + texIcon.getIconDepth()  ) / 2; break;
			case CENTER: 
			default: y = ( this.getHeight() - texIcon.getIconHeight() ) / 2; break;
		}
		
		texIcon.paintIcon( this, g, x, y );
	}
}
