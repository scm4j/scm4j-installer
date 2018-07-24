package org.scm4j.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import com.sun.deploy.config.OSType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.internal.win32.OSVERSIONINFO;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.scm4j.deployer.api.DeploymentResult;
import org.scm4j.deployer.engine.DeployerEngine;
import org.slf4j.Logger;

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
			StringWriter sw = new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));
			message += (message.isEmpty() ? "" : "\r\n") + sw.toString();
		}
		messageBox.setMessage(message);
		messageBox.open();
	}

	public static void downloadWithProgress(Shell shell, DeployerEngine deployerEngine, String product, String version) {
		Progress progress = new Progress(shell, "Downloading", () ->
				deployerEngine.download(product, version));
		Common.checkError(progress, shell, "Error downloading product");
	}

	public static void deployWithProgress(Shell shell, DeployerEngine deployerEngine, String product, String version) {
		Progress progress = new Progress(shell, "Deploying", () -> {
			DeploymentResult result = deployerEngine.deploy(product, version);
			String productAndVersion = product + "-" + version;
			switch (result) {
			case OK:
			case ALREADY_INSTALLED:
			case NEWER_VERSION_EXISTS:
				System.out.println(productAndVersion + ' ' + result.toString());
				break;
			case NEED_REBOOT:
				System.err.println(productAndVersion + " need reboot");
				break;
			case REBOOT_CONTINUE:
				int exitcode = 0;
				try {
					exitcode = Common.createBatAndTaskForWindowsTaskScheduler(product, version);
				} catch (Exception e) {
					System.err.println(e.toString() + "\n" + productAndVersion + " deploying failed!");
				}
				if (exitcode != 0)
					System.err.println("Can't create task to exec after reboot, task FAILED");
				else
					System.err.println("Installation will be successful after reboot");
				break;
			case FAILED:
			case INCOMPATIBLE_API_VERSION:
				System.err.println(productAndVersion + " deploying failed!");
				break;
			default:
				throw new RuntimeException("Invalid result!");
			}
		});
		checkError(progress, shell, "Error deploying product");
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
		List<String> taskEntry = Arrays.asList("schtasks", "/Create", "/ru", "\"System\"", "/tn", taskAndBatName, "/sc",
				"ONSTART", "/tr", '\"' + tempBatFile.getPath() + '\"');
		if(!SystemUtils.IS_OS_WINDOWS_XP) {
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
		String stdout = readStdout(p);
		LOG.info("stdout from task creation " + stdout);
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

	public static void checkError(Progress progress, Shell shell, String message) {
		Object result = progress.open();
		if (result != null) {
			if (result instanceof Throwable)
				Common.showError(shell, message, (Throwable) result);
			// TODO else ?
		}
	}

	public static String readStdout(Process p) throws Exception{
		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				p.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null)
				sb.append(line).append("\n");
			p.waitFor();
			return sb.toString();
		}
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
}
