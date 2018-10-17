package org.scm4j.installer;

import org.apache.commons.io.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Progress extends Dialog {

	private Runnable runnable;
	protected Throwable result;
	protected Shell shell;
	private Text textLog;
	private String shellName;
	private ProgressBar progressBar;
	private Button btnClose;

	/**
	 * Create the dialog.
	 */
	public Progress(Shell parent, String text, Runnable runnable) {
		super(parent);
		this.shellName = text;
		setText(text);
		this.runnable = runnable;
	}

	/**
	 * Open the dialog.
	 *
	 * @return the result
	 */
	public Object open() {
		Display display = getParent().getDisplay();

		createContents(display);

		shell.open();
		shell.layout();

		startWork();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents(Display display) {
		shell = new Shell(getParent(), SWT.RESIZE | SWT.TITLE | SWT.PRIMARY_MODAL);
		shell.setText(getText());
		shell.setSize(800, 400);
		shell.setLayout(new FormLayout());

		Rectangle monitorBounds = display.getPrimaryMonitor().getBounds();
		Rectangle shellSize = shell.getBounds();
		shell.setLocation((monitorBounds.width - shellSize.width) / 2 + monitorBounds.x,
				(monitorBounds.height - shellSize.height) / 2 + monitorBounds.y);

		progressBar = new ProgressBar(shell, SWT.NONE);
		FormData fd_progressBar = new FormData();
		fd_progressBar.top = new FormAttachment(0, 10);
		fd_progressBar.left = new FormAttachment(0, 10);
		fd_progressBar.right = new FormAttachment(100, -10);
		progressBar.setLayoutData(fd_progressBar);

		textLog = new Text(shell, SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		FormData fd_textLog = new FormData();
		fd_textLog.right = new FormAttachment(progressBar, 0, SWT.RIGHT);
		fd_textLog.top = new FormAttachment(progressBar, 6);
		fd_textLog.left = new FormAttachment(0, 10);
		textLog.setLayoutData(fd_textLog);

		btnClose = new Button(shell, SWT.NONE);
		btnClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
		btnClose.setEnabled(false);
		fd_textLog.bottom = new FormAttachment(btnClose, -6);
		FormData fd_btnClose = new FormData();
		fd_btnClose.width = 75;
		fd_btnClose.bottom = new FormAttachment(100, -10);
		fd_btnClose.right = new FormAttachment(100, -10);
		btnClose.setLayoutData(fd_btnClose);
		btnClose.setText("Close");

	}

	private void startWork() {

		new ProcessThread().start();

	}

	class ProcessThread extends Thread {

		public void run() {

			PrintStream standardOut = System.out;
			PrintStream standardErr = System.err;
			OutputStream textLogOutputStream = new OutputStream() {
				private String child = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) + '-'
						+ shellName;
				private File parentFile = new File(Settings.getWorkingFolder(), "logs");
				private File currentLogFile = new File(parentFile, child);
				private StringBuilder stringBuilder = new StringBuilder();

				@Override
				public synchronized void write(int b) {
					stringBuilder.append(String.valueOf((char) b));
					if (b == '\n') {
						String finalString = stringBuilder.toString();
						try {
							FileUtils.writeStringToFile(currentLogFile, finalString, "UTF-8", true);
						} catch (IOException e) {
							//
						}
						Display display = getParent().getDisplay();
						if (finalString.matches("^\\d{2}-\\d{2}-\\d{2}.*?(\r)\n$"))
							display.syncExec(() -> textLog.append(finalString));
						stringBuilder.setLength(0);
					}
				}
			};
			PrintStream textLogPrintStream = new PrintStream(textLogOutputStream);
			try {
				System.setOut(textLogPrintStream);
				System.setErr(textLogPrintStream);

				if (runnable != null) {
					try {
						runnable.run();
					} catch (Throwable e) {
						e.printStackTrace();
						result = e;
					}
				}

				// update progressBar
				Display display = getParent().getDisplay();
				display.syncExec(() -> {
					progressBar.setSelection(100);
					if (result != null) {
						textLog.append("\n" + result.toString() + "\n");
						progressBar.setState(SWT.ERROR);
						btnClose.setEnabled(true);
					} else {
						shell.close();
					}
				});
			} finally {
				System.setOut(standardOut);
				System.setErr(standardErr);
			}
		}
	}

}
