package org.scm4j.installer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Settings {

	private final static String PROPERTIES_FILE_NAME = "installer.properties";
	private final static String PROPERTY_SITE_DATA_DIR = "siteDataDir";
	private final static String PROPERTY_PRODUCT_LIST_URL = "productListUrl";
	private final static String DEFAULT_PRODUCT_LIST_URL = "https://dev.untill.com/artifactory/repo";

	private File runningPath;
	private Properties properties;
	private String siteDataDir;
	private String productListUrl;

	public Settings() {
		runningPath = getRunningPath();

		properties = new Properties();
		File propertiesFile = new File(runningPath, PROPERTIES_FILE_NAME);
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
			siteDataDir = new File(runningPath, "data").getPath();
		productListUrl = properties.getProperty(PROPERTY_PRODUCT_LIST_URL, DEFAULT_PRODUCT_LIST_URL);
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
