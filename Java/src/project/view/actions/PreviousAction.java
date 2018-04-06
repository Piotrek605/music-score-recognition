package project.view.actions;

import project.controller.Controller;
import project.view.View;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PreviousAction extends AbstractAction {

    private View view;
    private Controller controller;

    {
        putValue(NAME, "Previous stage");
        putValue(SMALL_ICON, new ImageIcon(
                getClass().getResource("/project/icons/previous.png")));
        putValue(SHORT_DESCRIPTION, "Shows the previous stage of the image processing");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control COMMA"));
    }

    public PreviousAction(View view, Controller controller)
    {
        this.view = view;
        this.controller = controller;
    }

    public void actionPerformed(ActionEvent e)
    {
        controller.displayPrevious();
    }
}
