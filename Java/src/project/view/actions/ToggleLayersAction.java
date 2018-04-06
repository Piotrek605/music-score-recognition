package project.view.actions;

import project.controller.Controller;
import project.view.View;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ToggleLayersAction extends AbstractAction
{
	private View				view;
	private Controller			controller;

	{
		putValue(NAME, "Toggle layers");
		putValue(SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/layers.png")));
		putValue(SHORT_DESCRIPTION, "Show/hide several stages on top of each other");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control T"));
	}

	public ToggleLayersAction(View view, Controller controller)
	{
		this.view = view;
		this.controller = controller;
	}

	public void actionPerformed(ActionEvent e)
	{
		controller.toggleLayers();
	}
}
