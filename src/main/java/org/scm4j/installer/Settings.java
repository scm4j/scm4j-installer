package org.scm4j.installer;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URLDecoder;

public final class Settings {

	public static final String WORKING_FOLDER = "C:/ProgramData/unTill/installer";
	public static final String PRODUCT_LIST_URL = "https://dev.untill.com/artifactory/repo";
	private static final String DEFAULT_INSTALLER_URL = "C:/tools/untill/installer";
	private static String productName = "scm4j-installer";

	private Settings() {
	}

	public static void setProductName(String productName) {
		Settings.productName = productName;
	}

	public static String getProductName() {
		return productName;
	}

	private static File getRunningFolder() {
		try {
			return new File(Settings.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
					.getParentFile();
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

	public static void copyJreIfNotExists() {
		String jreVersion = "jre-1.8.0_171";
		File jreFile = new File(DEFAULT_INSTALLER_URL, jreVersion);
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
