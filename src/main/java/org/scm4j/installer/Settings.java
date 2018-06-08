package org.scm4j.installer;

import java.io.File;
import java.net.URISyntaxException;

public final class Settings {

	public static final String WORKING_FOLDER = "C:/ProgramData/unTill/installer";
	public static final String PRODUCT_NAME = "scm4j-installer";
	public static final String PRODUCT_LIST_URL = "https://dev.untill.com/artifactory/repo";
	private static final String DEFAULT_INSTALLER_URL = "C:/tools/untill/installer";

	private Settings() {
	}

	private static File getRunningFolder() {
		try {
			return new File(Settings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
					.getParentFile().getParentFile();
		} catch (URISyntaxException e) {
			return new File(System.getProperty("user.dir")).getParentFile();
		}
	}

	private static boolean isPortable() {
		String fileName = getRunningFolder().getPath().replace('\\', '/');
		return !fileName.startsWith(DEFAULT_INSTALLER_URL);
	}

	public static File getWorkingFolder() {
		return new File(Settings.WORKING_FOLDER);
	}

	public static File getPortableFolder() {
		if (isPortable()) {
			return new File(getRunningFolder().getPath().replace('\\', '/'));
		} else {
			return null;
		}
	}
}
