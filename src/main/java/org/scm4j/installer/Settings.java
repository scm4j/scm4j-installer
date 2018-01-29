package org.scm4j.installer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Settings {

	private static String runningPath = System.getProperty("user.dir");
	private static String defaultProductListUrl = "https://dev.untill.com/artifactory/repo";
	private static String propertiesFileName = "installer.properties";
	private static String productName = "scm4j-installer";
	private final static String PROPERTY_SITE_DATA_DIR = "siteDataDir";
	private final static String PROPERTY_PRODUCT_LIST_URL = "productListUrl";

	public static void setRunningPath(String runningPath) {
		Settings.runningPath = runningPath;
	}

	public static void setDefaultProductListUrl(String defaultProductListUrl) {
		Settings.defaultProductListUrl = defaultProductListUrl;
	}

	public static void setPropertiesFileName(String propertiesFileName) {
		Settings.propertiesFileName = propertiesFileName;
	}

	public static void setProductName(String productName) {
		Settings.productName = productName;
	}

	public static String getProductName() {
		return productName;
	}
	private Properties properties;
	private String siteDataDir;
	private String productListUrl;

	public Settings() {
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
			siteDataDir = runningPath;
		productListUrl = properties.getProperty(PROPERTY_PRODUCT_LIST_URL, defaultProductListUrl);
	}

	public String getSiteDataDir() {
		return siteDataDir;
	}

	public String getProductListUrl() {
		return productListUrl;
	}
}
