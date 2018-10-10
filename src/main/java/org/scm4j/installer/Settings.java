package org.scm4j.installer;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;

public final class Settings {

	public static final String WORKING_FOLDER = "C:/ProgramData/unTill/installer";
	public static final String PRODUCT_LIST_URL = "https://dev.untill.com/artifactory/repo";
	public static final String DEFAULT_INSTALLER_URL = "C:/tools/untill/installer";
	private static String productName = "scm4j-installer";
	private static InputStream iconFileStream;

	private Settings() {
	}

	public static void setProductName(String productName) {
		Settings.productName = productName;
	}

	public static String getProductName() {
		return productName;
	}

	public static InputStream getIconFileStream() {
		return iconFileStream;
	}

	public static void setIconFileStream(InputStream iconFileStream) {
		Settings.iconFileStream = iconFileStream;
	}

	private static File getRunningFolder() {
		return getRunningFile().getParentFile();
	}

	public static File getRunningFile() {
		try {
			return new File(Settings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		} catch (URISyntaxException e) {
			return new File(System.getProperty("user.dir"));
		}
	}

	private static boolean isPortable() {
		return !getRunningFolder().toPath().startsWith(DEFAULT_INSTALLER_URL);
	}

	public static File getWorkingFolder() {
		return new File(Settings.WORKING_FOLDER);
	}

	public static File getPortableFolder() {
		if (isPortable()) {
			return new File(getRunningFolder().getParentFile().getPath());
		} else {
			return null;
		}
	}

}
