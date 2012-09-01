package com.moeyinc.formulamorph;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

public class GUI extends JFrame
{
	JPanel content; // fixed 16:9 top container
	static final double aspectRatio = 16.0 / 9.0;

	JPanel surfF;
	JPanel surfM; // morphed surface
	JPanel surfG;
	
	JPanel equationLaTeX;
//	JLabel equationLaTeX;
	
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
		surfF = new JPanel(); surfF.setBackground( Color.gray );
		surfM = new JPanel(); surfM.setBackground( Color.gray );
		surfG = new JPanel(); surfG.setBackground( Color.gray );

		equationLaTeX = new JPanel();//JLabel( "equation typeset with LaTeX" );
 equationLaTeX.setBackground( Color.gray );

		galF = new JPanel(); galF.setBackground( Color.gray );
		galG = new JPanel(); galG.setBackground( Color.gray );

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
		System.out.println("layout");
		surfF.setBounds( computeBounds( content, 10, 5 * aspectRatio, 23, 23 * aspectRatio ) );
		surfM.setBounds( computeBounds( content, 50 - ( 25 / 2 ), 3 * aspectRatio, 25, 25 * aspectRatio ) ); // center horizontally
		surfG.setBounds( computeBounds( content, 100 - 10 - 23, 5 * aspectRatio, 23, 23 * aspectRatio ) );

		equationLaTeX.setBounds( computeBounds( content, 50 - 90 / 2, 65, 90, 15 * aspectRatio ) ); // center horizontally

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
        setUndecorated(isFullScreen);
        setResizable(!isFullScreen);
        if (isFullScreen) {
            device.setFullScreenWindow( this );
			hideCursor();
            validate();
        }
    }
}
