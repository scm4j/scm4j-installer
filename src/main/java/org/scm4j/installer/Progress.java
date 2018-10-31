package org.scm4j.installer;

import org.apache.commons.io.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
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

public class Progress {

	private final Runnable runnable;
	private final Shell parent;
	protected Throwable result;
	protected Shell shell;
	private final String shellName;
	private Text log;

	/**
	 * Create the dialog.
	 */
	public Progress(Shell parent, String text, Runnable runnable) {
		this.parent = parent;
		this.shellName = text;
		this.runnable = runnable;
	}

	/**
	 * Open the dialog.
	 *
	 * @return the result
	 */
	public Object open() {
		Display display = parent.getDisplay();

		createContents(display);

		shell.open();
		shell.layout();

		new ProcessThread().start();

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
		shell = new Shell(parent, SWT.TITLE | SWT.APPLICATION_MODAL);
		shell.setText(shellName);
		shell.setSize(600, 100);
		shell.setLayout(new FormLayout());
		Common.centerWindow(display.getPrimaryMonitor().getBounds(), shell);

		ProgressBar progressBar = new ProgressBar(shell, SWT.INDETERMINATE);
		FormData fd_progressBar = new FormData();
		fd_progressBar.top = new FormAttachment(0, 30);
		fd_progressBar.left = new FormAttachment(0, 10);
		fd_progressBar.right = new FormAttachment(100, -10);
		progressBar.setLayoutData(fd_progressBar);

		log = new Text(shell, SWT.READ_ONLY);
		FormData fd_log = new FormData();
		fd_log.top = new FormAttachment(0, 10);
		fd_log.left = new FormAttachment(0, 10);
		fd_log.right = new FormAttachment(100, -10);
		fd_log.bottom = new FormAttachment(progressBar, 10);
		log.setLayoutData(fd_log);
	}

	class ProcessThread extends Thread {

		public void run() {
			PrintStream standardOut = System.out;
			PrintStream standardErr = System.err;
			OutputStream textLogOutputStream = new OutputStream() {
				private final String child =
						LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) + '-'
								+ shellName + ".txt";
				private final File parentFile = new File(Settings.getWorkingFolder(), "logs");
				private final File currentLogFile = new File(parentFile, child);
				private final StringBuilder stringBuilder = new StringBuilder();

				@Override
				public synchronized void write(int b) {
					stringBuilder.append(String.valueOf((char) b));
					if (b == '\n') {
						String finalString = stringBuilder.toString();
						Display display = shell.getDisplay();
						if (finalString.matches("^\\d{2}-\\d{2}-\\d{2}.*?(\r)\n$"))
							display.asyncExec(() -> log.setText(finalString.substring(15)));
						stringBuilder.setLength(0);
						try {
							FileUtils.writeStringToFile(currentLogFile, finalString, "UTF-8", true);
						} catch (IOException e) {
							//
						}
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
				Display display = shell.getDisplay();
				display.syncExec(() -> shell.close());
			} finally {
				System.setOut(standardOut);
				System.setErr(standardErr);
			}
		}
	}

}
