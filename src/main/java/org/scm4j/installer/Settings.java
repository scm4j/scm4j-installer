package org.scm4j.installer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Settings {

	private static String defaultProductListUrl = "https://dev.untill.com/artifactory/repo";
	private static String propertiesFileName = "installer.properties";
	private final static String PROPERTY_SITE_DATA_DIR = "siteDataDir";
	private final static String PROPERTY_PRODUCT_LIST_URL = "productListUrl";

	public static void setDefaultProductListUrl(String defaultProductListUrl) {
		Settings.defaultProductListUrl = defaultProductListUrl;
	}

	public static void setPropertiesFileName(String propertiesFileName) {
		Settings.propertiesFileName = propertiesFileName;
	}

	private File runningPath;
	private Properties properties;
	private String siteDataDir;
	private String productListUrl;

	public Settings() {
		runningPath = getRunningPath();

		properties = new Properties();
		File propertiesFile = new File(runningPath, propertiesFileName);
		if (propertiesFile.exists()) {
			try (InputStream inputStream = new FileInputStream(propertiesFile)) {
				properties.load(inputStream);
			} catch (IOException e) {
				// TODO show warning?
				e.printStackTrace();
			}
		}
		siteDataDir = properties.getProperty(PROPERTY_SITE_DATA_DIR);
		if (siteDataDir == null)
			siteDataDir = runningPath.getPath();
		productListUrl = properties.getProperty(PROPERTY_PRODUCT_LIST_URL, defaultProductListUrl);
	}

	public String getSiteDataDir() {
		return siteDataDir;
	}

	public String getProductListUrl() {
		return productListUrl;
	}

	private File getRunningPath() {
		try {
			return new File(Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
					.getParentFile();
		} catch (Exception e) {
			// TODO show warning?
			e.printStackTrace();
			return new File(System.getProperty("user.dir"));
		}
	}

}
