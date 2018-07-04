package org.scm4j.installer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.eclipse.swt.widgets.Shell;
import org.scm4j.deployer.api.DeploymentResult;
import org.scm4j.deployer.engine.DeployerEngine;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class CLI {

	private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CLI.class);
	private static final String COMMAND_DOWNLOAD = "download";
	private static final String COMMAND_DEPLOY = "deploy";

	private static final String SILENT_MODE_PROPERTY_NAME = "installer.silent";
	private static final String AFTER_REBOOT_PROPERTY_NAME = "installer.restarted";

	public static void main(String[] args) {

		String command = null;
		String product = null;
		String version = null;

		Options options = new Options()
				.addOption("r", "result-folder", true, "Store stdout, stderr and exitcode"
						+ " to specified folder")
				.addOption("i", "silent", false, "Sets the silent operation mode, "
						+ "you must use option -r <folder> with this mode")
				.addOption("a", "after-reboot", false, "Sets the after reboot mode");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmdLine = null;
		String errorMessage = null;
		String outputFolderName = null;
		File exitcodeFile = null;
		Shell shell = new Shell();
		try {
			cmdLine = parser.parse(options, args);
			String[] argsWoOptions = cmdLine.getArgs();
			if (argsWoOptions.length == 0) {
				errorMessage = "Missing required command";
			} else if (argsWoOptions[0].equalsIgnoreCase(COMMAND_DOWNLOAD)
					|| argsWoOptions[0].equalsIgnoreCase(COMMAND_DEPLOY)) {
				command = argsWoOptions[0].toLowerCase();
				product = argsWoOptions.length > 1 ? argsWoOptions[1] : "";
				version = argsWoOptions.length > 2 ? argsWoOptions[2] : "";
			} else {
				errorMessage = "Unknown command: " + argsWoOptions[0];
			}
		} catch (ParseException e) {
			errorMessage = e.getMessage();
		}
		if (cmdLine.hasOption("r"))
			outputFolderName = cmdLine.getOptionValue("r");
		if (outputFolderName != null) {
			try {
				File outputFolder = new File(outputFolderName);
				if (!outputFolder.exists())
					outputFolder.mkdirs();
				File errOut = new File(outputFolder, "stderr.txt");
				errOut.createNewFile();
				File stdOut = new File(outputFolder, "stdout.txt");
				stdOut.createNewFile();
				exitcodeFile = new File(outputFolder, "exitcode.txt");
				PrintStream errStream = new PrintStream(errOut);
				PrintStream outStream = new PrintStream(stdOut);
				System.setErr(errStream);
				System.setOut(outStream);
			} catch (Exception e) {
				errorMessage = "Can't create folder for logs in " + outputFolderName;
			}
		} else if (cmdLine.hasOption("i")) {
			errorMessage = "Can't use silent mode without -r <folder>";
		}
		if (errorMessage != null) {
			if (cmdLine.hasOption("r"))
				System.err.println(errorMessage);
			else
				Common.showError(shell, errorMessage, null);
			writeExitCodeToFileOrJustExit(1, exitcodeFile);
		}

		if (cmdLine.hasOption("i")) {
			System.setProperty(SILENT_MODE_PROPERTY_NAME, Boolean.toString(true));
		}

		if (cmdLine.hasOption("a"))
			System.setProperty(AFTER_REBOOT_PROPERTY_NAME, Boolean.toString(true));

		try {
			DeployerEngine deployerEngine = new DeployerEngine(Settings.getPortableFolder(),
					Settings.getWorkingFolder(), Settings.PRODUCT_LIST_URL);
			if (command.equalsIgnoreCase(COMMAND_DOWNLOAD)) {
				if (!cmdLine.hasOption("i")) {
					Common.downloadWithProgress(shell, deployerEngine, product, version);
				} else {
					deployerEngine.download(product, version);
				}
				writeExitCodeToFileOrJustExit(0, exitcodeFile);
			}
			if (command.equalsIgnoreCase(COMMAND_DEPLOY)) {
				Settings.copyJreIfNotExists();
				if (!cmdLine.hasOption("i")) {
					Common.deployWithProgress(shell, deployerEngine, product, version);
				} else {
					DeploymentResult result = deployerEngine.deploy(product, version);
					LOG.info("result of deploy " + product + ' ' + version + " is " + result.toString());
					int exitcode;
					switch (result) {
					case OK:
					case ALREADY_INSTALLED:
					case NEWER_VERSION_EXISTS:
						writeExitCodeToFileOrJustExit(0, exitcodeFile);
						break;
					case REBOOT_CONTINUE:
						exitcode = Common.createBatAndTaskForWindowsTaskScheduler(product, version, outputFolderName);
						if (exitcode != 0)
							writeExitCodeToFileOrJustExit(1, exitcodeFile);
						else
							Common.restartPc();
						break;
					case NEED_REBOOT:
						exitcode = Common.createBatAndTaskForWindowsTaskScheduler("@echo 0 > \""
								+ exitcodeFile.getPath() + '\"');
						if (exitcode != 0)
							writeExitCodeToFileOrJustExit(1, exitcodeFile);
						else
							Common.restartPc();
						break;
					case FAILED:
					case INCOMPATIBLE_API_VERSION:
						writeExitCodeToFileOrJustExit(2, exitcodeFile);
						break;
					default:
						throw new RuntimeException("Invalid result!");
					}
				}
			}
		} catch (Exception e) {
			if (cmdLine.hasOption("r"))
				System.err.println(e.getMessage());
			else
				Common.showError(shell, e.toString(), e);
			writeExitCodeToFileOrJustExit(2, exitcodeFile);
		}
	}

	private static void writeExitCodeToFileOrJustExit(int exitcode, File exitcodeFile) {
		if (exitcodeFile != null) {
			try {
				FileUtils.writeStringToFile(exitcodeFile, Integer.toString(exitcode), "UTF-8");
			} catch (IOException e) {
			}
		}
		System.exit(exitcode);
	}
}
