package org.scm4j.installer;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.scm4j.deployer.api.DeploymentResult;
import org.scm4j.deployer.engine.DeployerEngine;

import java.beans.Beans;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Installer {

	private DeployerEngine deployerEngine;

	protected Shell shlInstaller;
	private Shell shlDeployment;
	private Table tableProducts;
	private Button btnInstall;
	private Button btnUninstall;
	private Combo cmbVersions;
	private String version;

	public static void main(String[] args) {
		if (args.length > 0) {
			CLI.main(args);
			return;
		}
		try {
			Installer window = new Installer();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = new Display();
		createContents(display);
		if (!Beans.isDesignTime()) {
			init();
		}
		shlInstaller.open();
		shlInstaller.layout();
		while (!shlInstaller.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private void init() {
		Wait wait = new Wait(shlInstaller);
		wait.open();
		try {
			List<String> productNames = getDeployerEngine().refreshProducts();
			fillProductsAndVersions(productNames);
			for (String product : productNames) {
				getDeployerEngine().refreshProductVersions(product);
			}
		} finally {
			wait.close();
		}
	}

	private DeployerEngine getDeployerEngine() {
		if (deployerEngine == null) {
			deployerEngine = new DeployerEngine(Settings.getPortableFolder(), Settings.getWorkingFolder(),
					Settings.PRODUCT_LIST_URL);
		}
		return deployerEngine;
	}

	private void getProducts() {
		try {
			fillProductsAndVersions(getDeployerEngine().listProducts());
		} catch (Exception e) {
			Common.showError(shlInstaller, "Error getting product list", e);
		}
	}

	private void fillProductsAndVersions(List<String> products) {
		tableProducts.removeAll();
		for (String productName : products) {
			try {
				List<String> versions = getDeployerEngine().listProductVersions(productName);
				Map<String, String> deployedVersion = getDeployerEngine().mapDeployedProducts(productName);
				TableItem item = new TableItem(tableProducts, SWT.NONE);
				Optional<String> latestVersion = versions.stream()
						.filter(s -> !s.contains("-SNAPSHOT"))
						.max(Comparator.naturalOrder());
				item.setText(0, productName);
				item.setText(1, deployedVersion.getOrDefault(productName, "Not installed"));
				item.setText(2, latestVersion.orElse(""));
			} catch (Exception e) {
				Common.showError(shlInstaller, "Error getting product versions", e);
			}
		}
	}

	private void refreshButtons() {
		String installedVersion = tableProducts.getItem(tableProducts.getSelectionIndex()).getText(1);
		btnInstall.setEnabled(true);
		btnUninstall.setEnabled(true);
		if (!installedVersion.isEmpty()) {
			DefaultArtifactVersion instVersion = new DefaultArtifactVersion(installedVersion);
			DefaultArtifactVersion latestVersion = new DefaultArtifactVersion(
					tableProducts.getItem(tableProducts.getSelectionIndex()).getText(2));
			if (instVersion.compareTo(latestVersion) >= 0) {
				btnInstall.setEnabled(false);
				btnUninstall.setEnabled(true);
			}
		} else {
			btnInstall.setEnabled(true);
			btnUninstall.setEnabled(false);
		}
	}

	private void deploy(String productName, String version) {
		try {
			Common.copyJreIfNotExists();
		} catch (RuntimeException e) {
			Common.showError(shlInstaller, "jre doesn't present in one package back, please download"
					+ "it somewhere!", e);
		}
		Common.deployWithProgress(shlInstaller, getDeployerEngine(), productName, version);
		getProducts();
	}

	private void undeploy(String productName) {
		if (tableProducts.getSelectionIndex() == -1)
			return;

		Progress progress = new Progress(shlInstaller, "Undeploying", () -> {
			DeploymentResult result = getDeployerEngine().deploy(productName, "");
			if (result != DeploymentResult.OK)
				System.err.println(result);
		});
		Common.checkError(progress, shlInstaller, "Error undeploying product");
		getProducts();
	}

	private void centerWindow(Rectangle parent, Shell shellToCenter) {
		Rectangle shellSize = shellToCenter.getBounds();
		shellToCenter.setLocation((parent.width - shellSize.width) / 2 + parent.x,
				(parent.height - shellSize.height) / 2 + parent.y);
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents(Display display) {
		String iconFilePath = Settings.getIconFilePath();
		shlInstaller = new Shell(display);
		if (iconFilePath != null) {
			shlInstaller.setImage(new Image(display, iconFilePath));
		}
		shlInstaller.setSize(800, 600);
		shlInstaller.setText(StringUtils.capitalize(Settings.getProductName()));
		shlInstaller.setLayout(new FormLayout());

		Rectangle monitorBounds = display.getPrimaryMonitor().getBounds();
		centerWindow(monitorBounds, shlInstaller);

		SashForm sashForm = new SashForm(shlInstaller, SWT.VERTICAL);
		FormData fd_sashForm = new FormData();
		fd_sashForm.bottom = new FormAttachment(100, -10);
		fd_sashForm.top = new FormAttachment(0, 10);
		fd_sashForm.left = new FormAttachment(0, 10);
		sashForm.setLayoutData(fd_sashForm);

		tableProducts = new Table(sashForm, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		tableProducts.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshButtons();
			}
		});
		tableProducts.setHeaderVisible(true);
		tableProducts.setLinesVisible(true);

		TableColumn tblclmnProductName = new TableColumn(tableProducts, SWT.NONE);
		tblclmnProductName.setWidth(300);
		tblclmnProductName.setText("Product name");

		TableColumn tblclmnDownloaded = new TableColumn(tableProducts, SWT.NONE);
		tblclmnDownloaded.setWidth(100);
		tblclmnDownloaded.setText("Installed");

		TableColumn tblclmnDeployed = new TableColumn(tableProducts, SWT.NONE);
		tblclmnDeployed.setWidth(100);
		tblclmnDeployed.setText("Latest");

		tableProducts.setSize(tableProducts.computeSize(SWT.DEFAULT, 200));
		tableProducts.pack();

		Composite compositeButtons = new Composite(shlInstaller, SWT.NONE);
		fd_sashForm.right = new FormAttachment(compositeButtons, -10);
		compositeButtons.setLayout(new FormLayout());
		FormData fd_compositeButtons = new FormData();
		fd_compositeButtons.top = new FormAttachment(0, 10);
		fd_compositeButtons.bottom = new FormAttachment(100, -10);
		fd_compositeButtons.width = 100;
		fd_compositeButtons.right = new FormAttachment(100, -10);
		compositeButtons.setLayoutData(fd_compositeButtons);

		btnInstall = new Button(compositeButtons, SWT.NONE);
		btnInstall.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String productName = tableProducts.getItem(tableProducts.getSelectionIndex()).getText(0);
				shlDeployment = new Shell(shlInstaller);
				shlDeployment.setText("Installation");
				shlDeployment.setSize(400, 200);
				centerWindow(shlInstaller.getBounds(), shlDeployment);
				shlDeployment.setLayout(new FormLayout());

				Label lblInstalledVersion = new Label(shlDeployment, SWT.NONE);
				lblInstalledVersion.setText("Installed version: ");
				FormData fd_lblInstalledVersion = new FormData();
				fd_lblInstalledVersion.top = new FormAttachment(0, 10);
				fd_lblInstalledVersion.left = new FormAttachment(0, 10);
				lblInstalledVersion.setLayoutData(fd_lblInstalledVersion);

				Label lblVersion = new Label(shlDeployment, SWT.NONE);
				FormData fd_txtInstalledVersion = new FormData();
				fd_txtInstalledVersion.top = new FormAttachment(0, 10);
				fd_txtInstalledVersion.left = new FormAttachment(lblInstalledVersion, 15);
				fd_txtInstalledVersion.right = new FormAttachment(100, -10);
				lblVersion.setLayoutData(fd_txtInstalledVersion);
				lblVersion.setText("XXX.X");

				Label lblSelect = new Label(shlDeployment, SWT.NONE);
				lblSelect.setText("Select version:");
				FormData fd_lblSelect = new FormData();
				fd_lblSelect.top = new FormAttachment(lblInstalledVersion, 6);
				fd_lblSelect.left = new FormAttachment(0, 10);
				lblSelect.setLayoutData(fd_lblSelect);

				// Create a dropdown Combo
				cmbVersions = new Combo(shlDeployment, SWT.DROP_DOWN | SWT.READ_ONLY);
				FormData fd_cmbVersions = new FormData();
				fd_cmbVersions.top = new FormAttachment(lblInstalledVersion, 6);
				fd_cmbVersions.left = new FormAttachment(lblInstalledVersion, 15);
				fd_cmbVersions.right = new FormAttachment(100, -10);
				cmbVersions.setLayoutData(fd_cmbVersions);
				List<String> versions = getDeployerEngine().listProductVersions(productName).stream()
						.sorted(Comparator.reverseOrder())
						.collect(Collectors.toList());
				String[] items = versions.toArray(new String[]{});
				version = versions.get(0);
				cmbVersions.setItems(items);
				cmbVersions.select(0);
				cmbVersions.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						int versionIndex = cmbVersions.getSelectionIndex();
						version = cmbVersions.getItem(versionIndex);
					}
				});

				Button btnInstallFromCombo = new Button(shlDeployment, SWT.PUSH);
				btnInstallFromCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						createMessageBox(Action.DEPLOY, productName, version);
					}
				});
				FormData fd_btnLscDefaultUrl = new FormData();
				fd_btnLscDefaultUrl.width = 80;
				fd_btnLscDefaultUrl.bottom = new FormAttachment(100, -10);
				fd_btnLscDefaultUrl.right = new FormAttachment(cmbVersions, 0, SWT.RIGHT);
				btnInstallFromCombo.setLayoutData(fd_btnLscDefaultUrl);
				btnInstallFromCombo.setText("Install");
				shlDeployment.open();
			}
		});
		FormData fd_btnInstall = new FormData();
		fd_btnInstall.top = new FormAttachment(0);
		fd_btnInstall.left = new FormAttachment(0);
		fd_btnInstall.right = new FormAttachment(0, 100);
		btnInstall.setLayoutData(fd_btnInstall);
		btnInstall.setText("Install");

		btnUninstall = new Button(compositeButtons, SWT.NONE);
		btnUninstall.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String productName = tableProducts.getItem(tableProducts.getSelectionIndex()).getText(0);
				createMessageBox(Action.UNDEPLOY, productName, null);
			}
		});
		btnUninstall.setText("Uninstall");
		FormData fd_btnUninstall = new FormData();
		fd_btnUninstall.top = new FormAttachment(btnInstall, 6);
		fd_btnUninstall.left = new FormAttachment(0);
		fd_btnUninstall.right = new FormAttachment(0, 100);
		btnUninstall.setLayoutData(fd_btnUninstall);
	}

	private void createMessageBox(Action actionName, String productName, String version) {
		Shell messageShell = new Shell(shlInstaller);
		int style = SWT.YES | SWT.NO | SWT.ICON_QUESTION;
		MessageBox mb = new MessageBox(messageShell, style);
		mb.setText("Confirmation");
		String message = actionName == Action.DEPLOY ? "deploy " + productName + "-" + version :
				" undeploy " + productName;
		mb.setMessage("Do you really want to " + message);
		int val = mb.open();
		switch (val) {
		case SWT.YES:
			if (actionName == Action.DEPLOY) {
				deploy(productName, version);
				shlDeployment.close();
			} else {
				undeploy(productName);
			}
			break;
		case SWT.NO:
			messageShell.close();
			break;
		default:
			messageShell.close();
		}
	}

	enum Action {DEPLOY, UNDEPLOY}
}
