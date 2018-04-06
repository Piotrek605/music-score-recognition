package project.view.actions;

import project.controller.Controller;
import project.view.View;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ZoomInAction extends AbstractAction
{
	private View				view;
	private Controller			controller;

	{
		putValue(NAME, "Zoom in");
		putValue(SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/zoomin.png")));
		putValue(SHORT_DESCRIPTION, "Zooms in");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control EQUALS"));
	}

	public ZoomInAction(View view, Controller controller)
	{
		this.view = view;
		this.controller = controller;
	}

	public void actionPerformed(ActionEvent e)
	{
		controller.setScale(1.5 * controller.getScale());
	}
}
