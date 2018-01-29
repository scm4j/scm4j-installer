package org.scm4j.installer;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.scm4j.deployer.api.DeploymentResult;
import org.scm4j.deployer.engine.DeployerEngine;

public class CLI {

	private final static String COMMAND_DOWNLOAD = "download";
	private final static String COMMAND_DEPLOY = "deploy";

	public static void main(String[] args) {

		String command = null;
		String product = null;
		String version = null;

		Options options = new Options()
				.addOption("s", "stacktrace", false, "Print out the stacktrace");

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmdLine = null;
		String errorMessage = null;
		try {
			cmdLine = parser.parse(options, args);
			String[] argsWoOptions = cmdLine.getArgs();
			if (argsWoOptions.length == 0) {
				errorMessage = "Missing required command";
			} else if (argsWoOptions[0].equalsIgnoreCase(COMMAND_DOWNLOAD) || argsWoOptions[0].equalsIgnoreCase(COMMAND_DEPLOY)) {
				command = argsWoOptions[0].toLowerCase();
				product = argsWoOptions.length > 1 ? argsWoOptions[1] : null;
				version = argsWoOptions.length > 2 ? argsWoOptions[2] : null;
			} else {
				errorMessage = "Unknown command: " + argsWoOptions[0];
			}
		} catch (ParseException e) {
			errorMessage = e.getMessage();
		}
		if (errorMessage != null) {
			System.out.println(errorMessage);
			formatter.printHelp(Settings.getProductName() + " [options] " + COMMAND_DOWNLOAD + "|" + COMMAND_DEPLOY
					+ " <product> <version>", options);
			System.exit(1);
			return;
		}

		try {
			Settings settings = new Settings();
			DeployerEngine deployerEngine = new DeployerEngine(new File(settings.getSiteDataDir()),
					new File(settings.getSiteDataDir()), settings.getProductListUrl());
			
			if (command.equalsIgnoreCase(COMMAND_DOWNLOAD)) {
				deployerEngine.download(product, version);
			}
			if (command.equalsIgnoreCase(COMMAND_DEPLOY)) {
				DeploymentResult result = deployerEngine.deploy(product, version);
				if (result != DeploymentResult.OK) {
					System.exit(3);
					return;
				}
			}

		} catch (Exception e) {
			if (cmdLine.hasOption("stacktrace"))
				e.printStackTrace();
			else
				System.out.println(errorMessage);
			System.exit(2);
			return;
		}
	}

}
