package project.view.actions;

import project.controller.Controller;
import project.view.View;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ExitAction extends AbstractAction
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 7049149677012029336L;
	private View				view;
	private Controller			controller;

	{
		putValue(NAME, "Quit");
		putValue(SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/exit.png")));
		putValue(SHORT_DESCRIPTION, "Quits the program");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control Q"));

	}

	public ExitAction(View view, Controller controller)
	{
		this.view = view;
		this.controller = controller;
	}

	public void actionPerformed(ActionEvent e)
	{
		int response = JOptionPane.showConfirmDialog(view,
				"Do you really want to quit?", "Exit",
				JOptionPane.YES_NO_OPTION);
		if (response == JOptionPane.YES_OPTION)
			controller.exit(0);
	}
}
