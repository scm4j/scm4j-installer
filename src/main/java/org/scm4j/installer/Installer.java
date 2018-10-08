package org.scm4j.installer;

import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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

public class Installer {

	private DeployerEngine deployerEngine;

	protected Shell shlInstaller;
	private Table tableProducts;
	private Button btnInstall;
	private Button btnUninstall;

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
		refreshButtons();
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
		boolean disabled = tableProducts.getSelectionIndex() == -1;
		btnInstall.setEnabled(!disabled);
		btnUninstall.setEnabled(tableProducts.getSelectionIndex() != -1);
	}

	private void deploy() {
		if (tableProducts.getSelectionIndex() == -1)
			return;
		String product = tableProducts.getItem(tableProducts.getSelectionIndex()).getText(0);
		String version = tableProducts.getItem(tableProducts.getSelectionIndex()).getText(1);

		try {
			Common.copyJreIfNotExists();
		} catch (RuntimeException e) {
			Common.showError(shlInstaller, "jre doesn't present in one package back, please download"
					+ "it somewhere!", e);
		}
		Common.deployWithProgress(shlInstaller, getDeployerEngine(), product, version);
		getProducts();
	}

	private void undeploy() {
		if (tableProducts.getSelectionIndex() == -1)
			return;
		String product = tableProducts.getItem(tableProducts.getSelectionIndex()).getText(0);

		Progress progress = new Progress(shlInstaller, "Undeploying", () -> {
			DeploymentResult result = getDeployerEngine().deploy(product, "");
			if (result != DeploymentResult.OK)
				System.err.println(result);
		});
		Common.checkError(progress, shlInstaller, "Error undeploying product");
		getProducts();
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
		Rectangle shellSize = shlInstaller.getBounds();
		shlInstaller.setLocation((monitorBounds.width - shellSize.width) / 2 + monitorBounds.x,
				(monitorBounds.height - shellSize.height) / 2 + monitorBounds.y);

		SashForm sashForm = new SashForm(shlInstaller, SWT.VERTICAL);
		FormData fd_sashForm = new FormData();
		fd_sashForm.bottom = new FormAttachment(100, -10);
		fd_sashForm.top = new FormAttachment(0, 10);
		fd_sashForm.left = new FormAttachment(0, 10);
		sashForm.setLayoutData(fd_sashForm);

		tableProducts = new Table(sashForm, SWT.BORDER | SWT.FULL_SELECTION);
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
				deploy();
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
				Shell messageShell = new Shell(shlInstaller);
				int style = SWT.YES | SWT.NO | SWT.ICON_QUESTION;
				MessageBox mb = new MessageBox(messageShell, style);
				mb.setText("Confirmation");
				mb.setMessage("Do you really want to undeploy " + tableProducts
						.getItem(tableProducts.getSelectionIndex()).getText(0));
				int val = mb.open();
				switch (val) {
				case SWT.YES:
					undeploy();
					break;
				case SWT.NO:
					messageShell.close();
					break;
				default:
					messageShell.close();
				}
			}
		});
		btnUninstall.setText("Uninstall");
		FormData fd_btnUninstall = new FormData();
		fd_btnUninstall.top = new FormAttachment(btnInstall, 6);
		fd_btnUninstall.left = new FormAttachment(0);
		fd_btnUninstall.right = new FormAttachment(0, 100);
		btnUninstall.setLayoutData(fd_btnUninstall);

	}
}
