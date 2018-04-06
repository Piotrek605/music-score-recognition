package project.view.actions;

import project.controller.Controller;
import project.utils.SimpleFileFilter;
import project.utils.UnsupportedImageTypeException;
import project.view.View;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class OpenAction extends AbstractAction
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 9036684359479464138L;
	private View				view;
	private Controller			controller;

	// Note that once we first create a file chooser object, we keep it and
	// re-use
	// it rather than creating a new one each time that we invoke this action.
	// This has the effect that the chooser dialog always starts in the last
	// directory we opened from, rather than going back to the starting
	// directory.
	private JFileChooser		imageFileChooser	= null;

	{
		putValue(NAME, "Open new project image file...");
		putValue(SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/fileopen.png")));
		putValue(SHORT_DESCRIPTION, "Opens a new image file");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control O"));
	}

	public OpenAction(View view, Controller controller)
	{
		this.view = view;
		this.controller = controller;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (imageFileChooser == null)
		{
			imageFileChooser = new JFileChooser(".");
			// Use the following line instead if you always want the file
			// chooser to
			// start in the user's home directory rather than the current
			// directory
			// imageFileChooser = new
			// JFileChooser(System.getProperty("user.dir"));
			SimpleFileFilter filter = new SimpleFileFilter();
			filter.addExtension(".png");
			filter.addExtension(".tif");
			filter.addExtension(".tiff");
			filter.addExtension(".gif");
			filter.addExtension(".jpg");
			filter.addExtension(".wbmp");
			filter.addExtension(".raw");
			filter.addExtension(".bmp");
			filter.addExtension(".pnm");
			filter.addExtension(".jpeg");
			filter.addExtension(".pbm");
			filter.setDescription("image files");
			imageFileChooser.setFileFilter(filter);
		}
		imageFileChooser.setDialogTitle("Choose an image file to open");
		int result = imageFileChooser.showOpenDialog(view);
		try
		{
			if (result == JFileChooser.APPROVE_OPTION)
			{
				File f = imageFileChooser.getSelectedFile();
				controller.loadImage(f);
			}
		}
		catch (IOException ioe)
		{
			JOptionPane.showMessageDialog(view, ioe.getMessage(),
					"Accessing Image File", JOptionPane.ERROR_MESSAGE);
		}
		catch (UnsupportedImageTypeException uite)
		{
			JOptionPane.showMessageDialog(view, uite.getMessage(),
					"Reading Image File", JOptionPane.ERROR_MESSAGE);
		}
	}

}
