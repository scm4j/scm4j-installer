package org.scm4j.installer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.scm4j.deployer.engine.DeployerEngine;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Common {

	private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Common.class);

	private Common() {
	}

	public static void showError(Shell shell, String message, final Throwable exception) {
		MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		messageBox.setText(shell.getText());
		if (message == null)
			message = "";
		if (exception != null) {
			message += ' ' + exception.toString();
		}
		messageBox.setMessage(message);
		messageBox.open();
	}

	public static void showInfo(Shell shell, String message) {
		showMessage(shell, message, SWT.ICON_INFORMATION);
	}

	public static void showWarn(Shell shell, String message) {
		showMessage(shell, message, SWT.ICON_WARNING);
	}

	public static void showMessage(Shell shell, String message, int swtStyle) {
		MessageBox messageBox = new MessageBox(shell, swtStyle | SWT.OK);
		messageBox.setText(shell.getText());
		messageBox.setMessage(message);
		messageBox.open();
	}

	public static void downloadWithProgress(Shell shell, DeployerEngine deployerEngine, String product, String version) {
		Progress progress = new Progress(shell, "Downloading", () ->
				deployerEngine.download(product, version));
		Common.checkError(progress, shell, "Error downloading product");
	}

	public static int createBatAndTaskForWindowsTaskScheduler(String product, String version, String outputFolderName)
			throws Exception {
		return createBatAndTaskForWindowsTaskScheduler("start cmd /c \"" + Settings.getRunningFile().getPath()
				+ " deploy " + product + ' ' + version + " -a -i -r \"" + outputFolderName + "\"\"");
	}

	public static int createBatAndTaskForWindowsTaskScheduler(String product, String version)
			throws Exception {
		return createBatAndTaskForWindowsTaskScheduler("start cmd /c \"" + Settings.getRunningFile().getPath()
				+ " deploy " + product + ' ' + version + " -a\"");
	}

	public static int createBatAndTaskForWindowsTaskScheduler(String taskCommand) throws Exception {
		String taskAndBatName = "afterReboot" + System.nanoTime();
		File tempBatFile = new File(System.getProperty("java.io.tmpdir"), taskAndBatName + ".bat");
		List<String> taskEntry = new ArrayList<>(Arrays.asList("schtasks", "/Create", "/ru", "\"System\"", "/tn",
				taskAndBatName, "/sc", "ONSTART", "/tr", '\"' + tempBatFile.getPath() + '\"'));
		if (!SystemUtils.IS_OS_WINDOWS_XP) {
			taskEntry.add("/rl");
			taskEntry.add("highest");
		}
		List<String> batCommands = Arrays.asList("@echo off", taskCommand,
				"schtasks /delete /tn " + taskAndBatName + " /f", "(goto) 2>nul & del \"%~f0\"");
		FileUtils.writeLines(tempBatFile, "UTF-8", batCommands);
		LOG.info("Bat write successfully");
		ProcessBuilder builder = new ProcessBuilder(taskEntry).redirectErrorStream(true);
		Process p = builder.start();
		int exitcode = p.waitFor();
		LOG.info("exit code from task creation is " + exitcode);
		return exitcode;
	}

	public static void restartPc() {
		try {
			Runtime.getRuntime().exec("shutdown -r");
		} catch (IOException e) {
			//ok
		}
		System.exit(0);
	}

	public static boolean checkError(Progress progress, Shell shell, String message) {
		Object result = progress.open();
		if (result != null) {
			if (result instanceof Throwable) {
				Common.showError(shell, message, (Throwable) result);
				return true;
			}
		}
		return false;
	}

	public static void centerWindow(Display display, Shell shlLoading) {
		Rectangle monitorBounds = display.getPrimaryMonitor().getBounds();
		Rectangle shellSize = shlLoading.getBounds();
		shlLoading.setLocation((monitorBounds.width - shellSize.width) / 2 + monitorBounds.x,
				(monitorBounds.height - shellSize.height) / 2 + monitorBounds.y);
	}

	public static void copyJreIfNotExists() {
		String jreVersion = "jre-1.8.0_171";
		File jreFile = new File(Settings.DEFAULT_INSTALLER_URL, jreVersion);
		if (!jreFile.exists()) {
			jreFile.mkdirs();
			try {
				String path = Settings.class.getProtectionDomain().getCodeSource().getLocation().getPath();
				String decodedPath = URLDecoder.decode(path, "UTF-8");
				File jarFile = new File(decodedPath);
				File installerJreFile = new File(jarFile.getParentFile().getParentFile(), jreVersion);
				FileUtils.copyDirectory(installerJreFile, jreFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void resizeFonts(Display display, Control ctrl, int size) {
		FontData[] fDates = ctrl.getFont().getFontData();
		for (FontData fData : fDates)
			fData.setHeight(size);

		Font newFont = new Font(display, fDates);
		ctrl.setFont(newFont);
	}
}
