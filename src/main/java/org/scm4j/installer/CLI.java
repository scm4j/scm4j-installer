package org.scm4j.installer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.scm4j.deployer.api.DeploymentResult;
import org.scm4j.deployer.engine.DeployerEngine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class CLI {

	private final static String COMMAND_DOWNLOAD = "download";
	private final static String COMMAND_DEPLOY = "deploy";

	public static void main(String[] args) {

		String command = null;
		String product = null;
		String version = null;

		Options options = new Options()
				.addOption("r", "result-folder", true, "Store stdout, stderr and exitcode to specified folder")
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
		} else {
			errorMessage = "You need to specify directory for flag result-folder!";
		}
		if (errorMessage != null) {
			System.err.println(errorMessage);
			formatter.printHelp(Settings.getProductName() + " [options] " + COMMAND_DOWNLOAD + "|" + COMMAND_DEPLOY
					+ " <product> <version>", options);
			writeExitCodeToFileOrJustExit(1, exitcodeFile);
		}

		try {
			DeployerEngine deployerEngine = new DeployerEngine(Settings.getPortableFolder(),
					Settings.getWorkingFolder(), Settings.PRODUCT_LIST_URL);
			if (command.equalsIgnoreCase(COMMAND_DOWNLOAD)) {
				deployerEngine.download(product, version);
				writeExitCodeToFileOrJustExit(0, exitcodeFile);
			}
			if (command.equalsIgnoreCase(COMMAND_DEPLOY)) {
				Settings.copyJreIfNotExists();
				DeploymentResult result = deployerEngine.deploy(product, version);
				if (result == DeploymentResult.OK || result == DeploymentResult.ALREADY_INSTALLED) {
					writeExitCodeToFileOrJustExit(0, exitcodeFile);
				} else {
					writeExitCodeToFileOrJustExit(2, exitcodeFile);
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
