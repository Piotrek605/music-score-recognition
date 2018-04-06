package project.view.actions;

import project.controller.Controller;
import project.view.View;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ZoomOutAction extends AbstractAction
{
	private View				view;
	private Controller			controller;

	{
		putValue(NAME, "Zoom out");
		putValue(SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/zoomout.png")));
		putValue(SHORT_DESCRIPTION, "Zooms out");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control MINUS"));
	}

	public ZoomOutAction(View view, Controller controller)
	{
		this.view = view;
		this.controller = controller;
	}

	public void actionPerformed(ActionEvent e)
	{
		controller.setScale(0.75 * controller.getScale());
	}
}
