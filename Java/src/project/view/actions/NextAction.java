package project.view.actions;

import project.controller.Controller;
import project.view.View;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class NextAction extends AbstractAction {

    private View view;
    private Controller controller;

    {
        putValue(NAME, "Next stage");
        putValue(SMALL_ICON, new ImageIcon(
                getClass().getResource("/project/icons/next.png")));
        putValue(SHORT_DESCRIPTION, "Shows the next stage of the image processing");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control PERIOD"));
    }

    public NextAction(View view, Controller controller)
    {
        this.view = view;
        this.controller = controller;
    }

    public void actionPerformed(ActionEvent e)
    {
        controller.displayNext();
    }
}
