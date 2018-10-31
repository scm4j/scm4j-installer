package org.scm4j.installer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

public class Wait {

	private final Shell parent;
	private Shell shell;
	private Exception exception;

	public Wait(Shell parent) {
		this.parent = parent;
	}

	public void open(String message, Runnable run) throws Exception {
		Display display = parent.getDisplay();
		createContents(message);
		Common.centerWindow(display.getPrimaryMonitor().getBounds(), shell);
		shell.open();
		shell.layout();

		new Thread(() -> {
			try {
				run.run();
			} catch (Exception e) {
				exception = e;
			}
			display.syncExec(() -> shell.dispose());
		}).start();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		if (exception != null)
			throw exception;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents(String message) {
		shell = new Shell(parent, SWT.BORDER);
		shell.setSize(450, 70);
		shell.setText(message);
		
		Label lblTitle = new Label(shell, SWT.NONE);
		lblTitle.setBounds(10, 12, 424, 22);
		lblTitle.setText(message);
		Common.resizeFonts(Display.getDefault(), lblTitle, 12);

		ProgressBar progressBar = new ProgressBar(shell, SWT.INDETERMINATE);
		progressBar.setBounds(10, 35, 424, 20);
		progressBar.setMaximum(1);
	}
}
