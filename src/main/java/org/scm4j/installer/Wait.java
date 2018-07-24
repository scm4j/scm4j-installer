package org.scm4j.installer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

public class Wait extends Dialog {

	protected Shell shell;

	/**
	 * Create the dialog.
	 * @param parent
	 */
	public Wait(Shell parent) {
		super(parent);
		if (parent != null)
			setText(parent.getText());
	}

	public void open() {
		Display display = getParent().getDisplay();
		createContents();
		Common.centerWindow(display, shell);
		shell.open();
		shell.layout();
	}

	public void close() {
		shell.close();
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), SWT.BORDER);
		shell.setSize(450, 67);
		shell.setText(getText());
		
		Label lblTitle = new Label(shell, SWT.NONE);
		lblTitle.setBounds(10, 10, 424, 15);
		lblTitle.setText("Loading...");
		
		ProgressBar progressBar = new ProgressBar(shell, SWT.INDETERMINATE);
		progressBar.setBounds(10, 31, 424, 17);

	}
}
