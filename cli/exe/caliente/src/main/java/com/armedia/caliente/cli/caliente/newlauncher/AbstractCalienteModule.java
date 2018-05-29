package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.File;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.launcher.CalienteMain;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.tools.LocalOrganizationStrategy;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.caliente.tools.CmfCrypt;

public abstract class AbstractCalienteModule<L, E extends TransferEngine<?, ?, ?, ?, ?, L>> implements CalienteMain {

	protected final String server;
	protected final String user;
	protected final String password;
	protected final String domain;

	protected AbstractCalienteModule(E engine, boolean requiresStorage, boolean clearMetadata, boolean clearContent)
		throws Throwable {
		if (engine == null) { throw new IllegalArgumentException("Must provide an engine to operate with"); }

		this.server = CLIParam.server.getString();
		this.user = CLIParam.user.getString();
		String pass = CLIParam.password.getString();
		CmfCrypt crypto = this.engine.getCrypto();
		this.password = (pass != null ? crypto.encrypt(crypto.decrypt(pass)) : null);
		this.domain = CLIParam.domain.getString();

		this.warningTracker = new CalienteWarningTracker(this.console, true);
	}

	protected void customizeObjectStoreProperties(StoreConfiguration cfg) {
		// Do nothing by default
	}

	protected void customizeContentStoreProperties(StoreConfiguration cfg) {
		// Do nothing by default
	}

	protected File getMetadataFilesLocation() {
		return new File(Setting.DB_DIRECTORY.getString());
	}

	protected File getContentFilesLocation() {
		return new File(Setting.CONTENT_DIRECTORY.getString());
	}

	public static AbstractCalienteModule<?, ?> getInstance() {
		return AbstractCalienteModule.instance;
	}

	@Override
	public CmfObjectStore<?, ?> getObjectStore() {
		return this.cmfObjectStore;
	}

	protected String getContentStrategyName() {
		return LocalOrganizationStrategy.NAME;
	}
}