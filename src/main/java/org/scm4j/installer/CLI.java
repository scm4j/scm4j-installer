package org.scm4j.installer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.eclipse.swt.widgets.Shell;
import org.scm4j.deployer.api.DeploymentResult;
import org.scm4j.deployer.engine.DeployerEngine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class CLI {

	private static final String COMMAND_DOWNLOAD = "download";
	private static final String COMMAND_DEPLOY = "deploy";

	private static final String SILENT_MODE_PROPERTY_NAME = "installer.silent";

	public static void main(String[] args) {

		String command = null;
		String product = null;
		String version = null;

		Options options = new Options()
				.addOption("r", "result-folder", true, "Store stdout, stderr and exitcode to specified folder")
				.addOption("i", "silent", false, "Sets the silent operation mode")
				.addOption("s", "stacktrace", false, "Print out the stacktrace");

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmdLine = null;
		String errorMessage = null;
		File exitcodeFile = null;
		try {
			cmdLine = parser.parse(options, args);
			String[] argsWoOptions = cmdLine.getArgs();
			if (argsWoOptions.length == 0) {
				errorMessage = "Missing required command";
			} else if (argsWoOptions[0].equalsIgnoreCase(COMMAND_DOWNLOAD) || argsWoOptions[0].equalsIgnoreCase(COMMAND_DEPLOY)) {
				command = argsWoOptions[0].toLowerCase();
				product = argsWoOptions.length > 1 ? argsWoOptions[1] : "";
				version = argsWoOptions.length > 2 ? argsWoOptions[2] : "";
			} else {
				errorMessage = "Unknown command: " + argsWoOptions[0];
			}
		} catch (ParseException e) {
			errorMessage = e.getMessage();
		}
		String outputFolderName = cmdLine.getOptionValue("r");
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
		}
		if (errorMessage != null) {
			System.err.println(errorMessage);
			formatter.printHelp(Settings.getProductName() + " [options] " + COMMAND_DOWNLOAD + "|" + COMMAND_DEPLOY
					+ " <product> <version>", options);
			writeExitCodeToFileOrJustExit(1, exitcodeFile);
		}

		Shell shell = null;
		if (cmdLine.hasOption("i")) {
			System.setProperty(SILENT_MODE_PROPERTY_NAME, Boolean.toString(true));
		} else {
			shell = new Shell();
		}

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
					if (result == DeploymentResult.OK || result == DeploymentResult.ALREADY_INSTALLED) {
						writeExitCodeToFileOrJustExit(0, exitcodeFile);
					} else {
						writeExitCodeToFileOrJustExit(2, exitcodeFile);
					}
				}
			}
		} catch (Exception e) {
			if (cmdLine.hasOption("s")) {
				e.printStackTrace();
			} else {
				System.err.println(e.getMessage());
			}
			writeExitCodeToFileOrJustExit(2, exitcodeFile);
		}
	}

	private static void writeExitCodeToFileOrJustExit(int exitcode, File exitcodeFile) {
		try {
			if (exitcodeFile != null)
				FileUtils.writeStringToFile(exitcodeFile, Integer.toString(exitcode), "UTF-8");
			System.exit(exitcode);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
