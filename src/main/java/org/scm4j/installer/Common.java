package org.scm4j.installer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.scm4j.deployer.api.DeploymentResult;
import org.scm4j.deployer.engine.DeployerEngine;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class Common {

	private Common() {}

	public static void showError(Shell shell, String message, final Throwable exception) {
		MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		messageBox.setText(shell.getText());
		if (message == null)
			message = "";
		if (exception != null) {
			StringWriter sw = new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));
			message += (message.isEmpty() ? "" : "\r\n") + sw.toString();
		}
		messageBox.setMessage(message);
		messageBox.open();
	}

	public static  void downloadWithProgress(Shell shell, DeployerEngine deployerEngine, String product, String version) {
		Progress progress = new Progress(shell, "Downloading", () ->
				deployerEngine.download(product, version));
		Common.checkError(progress, shell, "Error downloading product");
	}

	public static void deployWithProgress(Shell shell, DeployerEngine deployerEngine, String product, String version) {
		Progress progress = new Progress(shell, "Deploying", () -> {
			DeploymentResult result = deployerEngine.deploy(product, version);
			String productAndVersion = product + "-" + version;
			//TODO rewrite this!
			switch (result) {
			case OK:
			case ALREADY_INSTALLED:
				break;
			case NEED_REBOOT:
				System.err.println(productAndVersion + " need reboot");
				//TODO why we need reboot? don't stop or smth else?
				break;
			case FAILED:
				System.err.println(productAndVersion + " deploying failed!");
				break;
			default:
				throw new RuntimeException("Invalid result!");
			}
		});
		checkError(progress, shell, "Error deploying product");
	}

	public static void checkError(Progress progress, Shell shell, String message) {
		Object result = progress.open();
		if (result != null) {
			if (result instanceof Throwable)
				Common.showError(shell, message, (Throwable) result);
			// TODO else ?
		}
	}
}
