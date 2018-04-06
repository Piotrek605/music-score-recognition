package project.view;

import project.controller.Controller;
import project.model.Model;

import javax.swing.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

class Canvas extends JPanel
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -7552020673193783095L;
	private Model				model;
	private View				view;

	private CanvasMouseListener	mouseListener;

	/**
	 * The default constructor should NEVER be called. It has been made private
	 * so that no other class can create a Canvas except by initialising it
	 * properly (i.e. by calling the parameterised constructor)
	 */
	@SuppressWarnings("unused")
	private Canvas()
	{
	}

	/**
	 * Create a <code>Canvas</code> object initialised to the given
	 * <code>View</code> and <code>Model</code>
	 *
	 * @param view
	 *            The View object that encapsulates the whole GUI
	 * @param model
	 *            The Model object that encapsulates the (view-independent) data
	 *            of the application
	 * @param controller
	 *            The Controller object that handles all operations
	 */
	public Canvas(Model model, View view, Controller controller)
	{
		this.view = view;
		this.model = model;
		mouseListener = new CanvasMouseListener(this.model, this.view,
				controller);
		addMouseListener(mouseListener);
	}

	/**
	 * The method that is called to paint the contents of this component
	 *
	 * @param g
	 *            The <code>Graphics</code> object used to do the actual drawing
	 */
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		// Using g or g2, draw on the full size "canvas":
		Graphics2D g2 = (Graphics2D) g;
		g2.scale(model.getScale(), model.getScale());
		//
		// The ViewPort is the part of the canvas that is displayed.
		// By scrolling the ViewPort, you move it across the full size canvas,
		// showing only the ViewPort sized window of the canvas at any one time.

		if (model.isActive())
		{
			BufferedImage image = model.getImage();
			List<Rectangle> rects = model.getRects();

			int histogramWidth = 100;

			// Draw the display image on the full size canvas
			g2.drawImage(image, histogramWidth, 0, null);

			if (!rects.isEmpty())
			{
				Color col = g2.getColor();
				g2.setColor(Color.BLUE);
				for (Rectangle rect : rects)
				{
					g2.draw(rect);
				}
				g2.setColor(col);
			}
			// In case there is some animation going on (e.g. mouse dragging),
			// call this to
			// paint the intermediate images
			mouseListener.paint(g);

			BufferedImage histogramImage = new BufferedImage(histogramWidth+1, image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
			int[] histogram = model.getHistogram();
			for(int i=0; i<image.getHeight(); i++) {
				for(int j=0; j<histogramWidth; j++) {
					int value = 0;
					if(j >= histogram[i] * histogramWidth / image.getWidth()) {
						value = 255;
					}
					histogramImage.setRGB(j, i, (value<<16) | (value<<8) | value);
				}
			}
			for(int i=0; i<image.getHeight(); i++) {
				histogramImage.setRGB(histogramWidth, i, (0<<16) | (0<<8) | 0);
			}
			g2.drawImage(histogramImage, 0, 0, null);
		}
	}

	/**
	 * Get the preferred size of the canvas.
	 * 
	 * @return The <code>Dimension</code> object containing the size of the
	 *         underlying image in the model, if one exits, or
	 *         <code>(0,0)</code> if it does not.
	 */
	public Dimension getPreferredSize()
	{
		return model.getDimensions();
	}
}
