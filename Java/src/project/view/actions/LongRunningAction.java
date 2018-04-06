package project.view.actions;

import project.controller.Controller;
import project.view.View;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * This is a model for an action that triggers a long running operation in
 * Swing, while keeping to our MVC structure.
 * <p>
 * The idea is that this action class handles only the GUI side of things, and
 * forwards the real work to the controller. This action creates a dialog while
 * the work is ongoing, and keeps a progress bar updated throughout.
 * </p>
 * <p>
 * The dialog box can be modal or non modal, and multiple instances of the work
 * can be running at the same time without interfering, or, via appropriate
 * enabling and disabling of the action, it can be restricted so that only one
 * instance can run at once. The approach also supports handling partial result
 * feedback during the processing.
 * </p>
 */
public class LongRunningAction extends AbstractAction
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -5547257080295928486L;
	private View				view;
	private Controller			controller;

	{
		putValue(NAME, "Process the image...");
		putValue(SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/longrunning.png")));
		putValue(SHORT_DESCRIPTION, "Executes the next step of music recognition");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control L"));
	}

	public LongRunningAction(View view, Controller controller)
	{
		this.view = view;
		this.controller = controller;
	}

	public void actionPerformed(ActionEvent e)
	{
		JDialog dialog = new JDialog(view, "Working ...");
		JProgressBar progressBar = new JProgressBar(0, 100);

		final LongOperation longOperation = new LongOperation();

		JLabel label = new JLabel("Progress: ");
		progressBar.setPreferredSize(new Dimension(175, 20));
		progressBar.setString("Working");
		progressBar.setStringPainted(true);
		progressBar.setValue(0);
		progressBar.setIndeterminate(true);
		longOperation.addPropertyChangeListener(
				new LongOpPropertyChangeListener(dialog, progressBar));

		JPanel centerPanel = new JPanel();
		centerPanel.add(label);
		centerPanel.add(progressBar);

		dialog.getContentPane().add(centerPanel, BorderLayout.CENTER);

		JButton button = new JButton("Cancel");
		button.setActionCommand("cancel");
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				if (evt.getActionCommand().equals("cancel"))
				{
					longOperation.cancel(true);
				}
			}
		});

		JPanel southPanel = new JPanel();
		southPanel.add(button);
		dialog.getContentPane().

				add(southPanel, BorderLayout.SOUTH);

		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.setLocationRelativeTo(view);

		// Note that this works both as a modal or non-modal dialog. If you make
		// it non-modal, you must think carefully about whether you want to
		// disable
		// any actions that might conflict with the operation and, if necessary,
		// re-enable them afterwards. The view code allow for all options:
		// consider
		// carefully the controller code that gets called.

		// uncomment to make modal:
		// dialog.setModal(true);

		// uncomment the following line if you are using a non-modal dialog and
		// want
		// to disallow multiple different parallel executions of this action
		// this.setEnabled(false);

		dialog.pack();
		longOperation.execute();
		dialog.setVisible(true);
	}

	/**
	 * This listener class handles updating the progress bar for the operation
	 * as well as cleaning up and processing partial and final results. An
	 * object of this class should be added to the SwingWorker class that
	 * handles the operation
	 */
	private class LongOpPropertyChangeListener implements PropertyChangeListener
	{
		private JDialog			dialog;
		private JProgressBar	progressBar;

		public LongOpPropertyChangeListener(JDialog dialog,
				JProgressBar progressBar)
		{
			this.dialog = dialog;
			this.progressBar = progressBar;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt)
		{
			if ("progress".equals(evt.getPropertyName()))
			{
				progressBar.setIndeterminate(false);
				progressBar.setValue((Integer) evt.getNewValue());

			}
			else if ("state".equals(evt.getPropertyName()))
			{
				if (evt.getNewValue() == SwingWorker.StateValue.DONE)
				{
					// replace with something appropriate
					System.out.println("Long Operation Done");

					dialog.setVisible(false);
					dialog.dispose();
					LongRunningAction.this.setEnabled(true);
				}
				else if (evt.getNewValue() == SwingWorker.StateValue.PENDING)
					// replace with something appropriate
					System.out.println("Long Operation Pending");
				else if (evt.getNewValue() == SwingWorker.StateValue.STARTED)
					// replace with something appropriate
					System.out.println("Long Operation Started");
				else
					// replace with something appropriate
					System.out
							.println("Long Operation: unrecognised StateValue");

			}
		}
	}

	/**
	 * The SwingWorker class to handle the operation. The first generic type
	 * parameter specifies the type of the final result. The second specifies
	 * the element type of partial results: partial results will be a List
	 * (actually ArrayList) of those element types.
	 */
	class LongOperation extends SwingWorker<Integer, Integer>
	{

		@Override
		protected Integer doInBackground() throws Exception
		{
			// NOTE: this method runs in a different thread to the Swing Event
			// Dispatch
			// Thread (EDT) hence MUST NOT directly interact with any swing
			// component.
			// Thus, for example, no direct updating of the JProgressBar.

			controller.processImage();

			List<Integer> partialResults = new ArrayList<Integer>();
			Integer[] result = { 0 };
			int percentDone;
			while ((percentDone = controller.executeLongOpStep(result,
					partialResults)) < 100)
			{
				if (isCancelled())
				{
					// might need to call something in the controller as well to
					// clean up
					return 0;
				}
				// can't do the following because we are not running in the EDT:
				// progressBar.setValue(percentageDone);
				// Instead set the progress property and leave Swing's
				// propertyChangeListener to notice the change.
				setProgress(percentDone);

				if (!partialResults.isEmpty())
				{
					publish(partialResults
							.toArray(new Integer[partialResults.size()]));
					partialResults.clear();
				}

			}
			// only get to here on successful completion, which means
			return result[0];
		}

		@Override
		protected void process(List<Integer> chunks)
		{
			// This process runs in the EDT, so it CAN update the view and
			// interact with
			// Swing Components: replace with something appropriate
			System.err.println("process: " + chunks.toString());
		}

		@Override
		protected void done()
		{
			// This process runs in the EDT, so it CAN update the view and
			// interact with
			// Swing Components.

			try
			{
				// Even if the operation does not produce any result, you should
				// still call
				// get() to ensure that any exceptions from the long running
				// operation are
				// handled properly

				// replace with something appropriate
				System.err.println(get());
			}
			catch (CancellationException e)
			{
				// replace with something appropriate
				System.err.println("Cancellation Exception");

			}
			catch (InterruptedException e)
			{
				// replace with something appropriate
				System.err.println("Interrupted Exception: " + e.getMessage());
			}
			catch (ExecutionException e)
			{
				// replace with something appropriate
				//System.err.println("Execution Exception: " + e.getMessage());
				e.printStackTrace();
			}
			// finally
			// {
			// // This could go here, but better in the action listener
			// // LongRunningAction.this.setEnabled(true);
			// }
		}

	}
}
