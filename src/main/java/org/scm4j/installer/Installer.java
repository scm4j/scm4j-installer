package org.scm4j.installer;

import java.beans.Beans;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.scm4j.deployer.engine.DeployerEngine;

public class Installer {

	private Settings settings;
	private DeployerEngine deployerEngine;

	protected Shell shlInstaller;
	private Table tableProducts;
	private Table tableVersions;
	private Button btnRefresh;
	private Button btnDownload;
	private Button btnInstall;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
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
		Display display = Display.getDefault();
		createContents();
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
		settings = new Settings();

		getProducts();

		// TODO ...
	}

	private DeployerEngine getDeployerEngine() {
		if (deployerEngine == null)
			deployerEngine = new DeployerEngine(new File(settings.getSiteDataDir()), settings.getProductListUrl());
		return deployerEngine;
	}

	private void getProducts() {
		try {
			fillProducts(getDeployerEngine().listProducts());
		} catch (Exception e) {
			showError("Error getting product list", e);
		}
		refreshVersions(false);
	}

	private void fillProducts(List<String> products) {
		String selectedProduct = null;
		if (tableProducts.getSelectionIndex() != -1)
			selectedProduct = tableProducts.getItem(tableProducts.getSelectionIndex()).getText();
		tableProducts.removeAll();
		for (String productName: products) {
			TableItem item = new TableItem(tableProducts, SWT.NONE);
			item.setText(productName);
			if (selectedProduct != null && selectedProduct.equals(productName))
				tableProducts.setSelection(item);
		}
	}

	private void fillVersions(Map<String, Boolean> versions, boolean refresh) {
		String selectedVersion = null;
		if (refresh && tableVersions.getSelectionIndex() != -1)
			selectedVersion = tableVersions.getItem(tableVersions.getSelectionIndex()).getText();
		tableVersions.removeAll();
		for (String version: versions.keySet()) {
			TableItem item = new TableItem(tableVersions, SWT.NONE);
			item.setText(version);
			item.setText(1, versions.get(version) ? "yes" : "no");
			if (selectedVersion != null && selectedVersion.equals(version))
				tableVersions.setSelection(item);
		}
		if (tableVersions.getSelectionIndex() == -1 && tableVersions.getItemCount() > 0)
			tableVersions.setSelection(tableVersions.getItemCount() - 1);
	}

	private void refresh() {
		try {
			List<String> products = getDeployerEngine().refreshProducts();
			fillProducts(products);
			refreshVersions(true);
		} catch (Exception e) {
			showError("Error refreshing product list", e);
		}
	}

	private void refreshVersions(boolean refresh) {
		try {
			if (tableProducts.getSelectionIndex() != -1) {
				TableItem item = tableProducts.getItem(tableProducts.getSelectionIndex());
				String product = item.getText();
				Map<String, Boolean> versions = refresh ? getDeployerEngine().refreshProductVersions(product)
						: getDeployerEngine().listProductVersions(product);
				fillVersions(versions, refresh);
			} else {
				tableVersions.clearAll();
			}
		} catch (Exception e) {
			showError("Error getting product versions", e);
		}
		refreshButtons();
	}

	private void refreshButtons() {
		boolean disabled = tableProducts.getSelectionIndex() == -1 || tableVersions.getSelectionIndex() == -1;
		btnDownload.setEnabled(!disabled);
		btnInstall.setEnabled(!disabled);
	}

	private void download() {
		try {
			if (tableProducts.getSelectionIndex() == -1 || tableVersions.getSelectionIndex() == -1)
				return;
			String product = tableProducts.getItem(tableProducts.getSelectionIndex()).getText();
			String version = tableVersions.getItem(tableVersions.getSelectionIndex()).getText();
			getDeployerEngine().download(product, version);
			getProducts();
		} catch (Exception e) {
			showError("Error downloading product", e);
		}
	}

	private void deploy() {
		try {
			if (tableProducts.getSelectionIndex() == -1 || tableVersions.getSelectionIndex() == -1)
				return;
			String product = tableProducts.getItem(tableProducts.getSelectionIndex()).getText();
			String version = tableVersions.getItem(tableVersions.getSelectionIndex()).getText();
			getDeployerEngine().deploy(product, version);
			getProducts();
		} catch (Exception e) {
			showError("Error deploying product", e);
		}
	}

	private void showError(String message, final Throwable exception) {
		MessageBox messageBox = new MessageBox(shlInstaller, SWT.ICON_ERROR | SWT.OK);
		messageBox.setText(shlInstaller.getText());
		if (message == null)
			message = "";
		if (exception != null) {
			StringWriter sw = new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));
			message += (message.isEmpty() ? "" : "\r\n") + sw.toString();
		}
		messageBox.setMessage(message);
		messageBox.open();
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shlInstaller = new Shell();
		shlInstaller.setSize(450, 300);
		shlInstaller.setText("Installer");
		shlInstaller.setLayout(new FormLayout());
		
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
				refreshVersions(false);
			}
		});
		tableProducts.setHeaderVisible(true);
		tableProducts.setLinesVisible(true);
		
		TableColumn tblclmnProductName = new TableColumn(tableProducts, SWT.NONE);
		tblclmnProductName.setWidth(300);
		tblclmnProductName.setText("Product name");
		
		tableVersions = new Table(sashForm, SWT.BORDER | SWT.FULL_SELECTION);
		tableVersions.setHeaderVisible(true);
		tableVersions.setLinesVisible(true);
		
		TableColumn tblclmnVersion = new TableColumn(tableVersions, SWT.NONE);
		tblclmnVersion.setWidth(200);
		tblclmnVersion.setText("Version");
		
		TableColumn tblclmnDownloaded = new TableColumn(tableVersions, SWT.NONE);
		tblclmnDownloaded.setWidth(100);
		tblclmnDownloaded.setText("Downloaded");
		sashForm.setWeights(new int[] {1, 1});
		
		Composite compositeButtons = new Composite(shlInstaller, SWT.NONE);
		fd_sashForm.right = new FormAttachment(compositeButtons, -10);
		compositeButtons.setLayout(new FormLayout());
		FormData fd_compositeButtons = new FormData();
		fd_compositeButtons.top = new FormAttachment(0, 10);
		fd_compositeButtons.bottom = new FormAttachment(100, -10);
		fd_compositeButtons.width = 100;
		fd_compositeButtons.right = new FormAttachment(100, -10);
		compositeButtons.setLayoutData(fd_compositeButtons);
		
		btnRefresh = new Button(compositeButtons, SWT.NONE);
		btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refresh();
			}
		});
		FormData fd_btnRefresh = new FormData();
		fd_btnRefresh.right = new FormAttachment(0, 100);
		fd_btnRefresh.top = new FormAttachment(0);
		fd_btnRefresh.left = new FormAttachment(0);
		btnRefresh.setLayoutData(fd_btnRefresh);
		btnRefresh.setText("Refresh");
		
		btnDownload = new Button(compositeButtons, SWT.NONE);
		btnDownload.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				download();
			}
		});
		FormData fd_btnDownload = new FormData();
		fd_btnDownload.right = new FormAttachment(0, 100);
		fd_btnDownload.top = new FormAttachment(0, 31);
		fd_btnDownload.left = new FormAttachment(0);
		btnDownload.setLayoutData(fd_btnDownload);
		btnDownload.setText("Download");
		
		btnInstall = new Button(compositeButtons, SWT.NONE);
		btnInstall.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deploy();
			}
		});
		FormData fd_btnInstall = new FormData();
		fd_btnInstall.right = new FormAttachment(0, 100);
		fd_btnInstall.top = new FormAttachment(0, 62);
		fd_btnInstall.left = new FormAttachment(0);
		btnInstall.setLayoutData(fd_btnInstall);
		btnInstall.setText("Install");

	}
}
