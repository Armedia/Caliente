package com.delta.cmsmf.mainEngine;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.constants.CMSMFProperties;
import com.delta.cmsmf.exception.CMSMFFatalException;
import com.delta.cmsmf.properties.PropertiesManager;
import com.documentum.fc.client.DfClient;
import com.documentum.fc.client.DfQuery;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfLoginInfo;
import com.documentum.fc.tools.RegistryPasswordUtils;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public abstract class CMSMFMain {

	/** The logger object used for logging. */
	protected final Logger logger = Logger.getLogger(getClass());

	private static CMSMFMain instance = null;

	private final PoolableObjectFactory<IDfSession> connectionFactory = new PoolableObjectFactory<IDfSession>() {

		@Override
		public IDfSession makeObject() throws Exception {
			return CMSMFMain.this.dfClient.newSession(CMSMFMain.this.docbase, CMSMFMain.this.loginInfo);
		}

		@Override
		public void destroyObject(IDfSession obj) throws Exception {
			if (obj != null) {
				obj.disconnect();
			}
		}

		@Override
		public boolean validateObject(IDfSession obj) {
			if (obj == null) { return false; }
			try {
				obj.getSessionId();
			} catch (DfException e) {
				return false;
			}
			if (!obj.isConnected()) { return false; }

			IDfQuery q = new DfQuery();
			q.setDQL("select date(now) as systime from dm_server_config");
			IDfCollection c = null;
			try {
				c = q.execute(obj, IDfQuery.DF_EXECREAD_QUERY);
				if (!c.next()) { return false; }
				c.getString("systime");
			} catch (DfException e) {
				return false;
			} finally {
				if (c != null) {
					try {
						c.close();
					} catch (DfException e) {
						// Ignore...
					}
				}
			}
			return true;
		}

		@Override
		public void activateObject(IDfSession obj) throws Exception {
		}

		@Override
		public void passivateObject(IDfSession obj) throws Exception {
		}
	};

	private ObjectPool<IDfSession> connectionPool;

	private static final int DEFAULT_MAX_THREADS = 4;

	private final IDfClient dfClient;

	private final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
	private final ThreadPoolExecutor executor;

	private final String docbase;
	private final String userName;
	private final String password;
	private final IDfLoginInfo loginInfo;

	/** The directory location where stream files will be created. */
	protected final File streamFilesDirectoryLocation;

	/** The directory location where content files will be created. */
	protected final File contentFilesDirectoryLocation;

	protected final boolean testMode;

	CMSMFMain() throws Throwable {
		// First things first...
		CMSMFMain.instance = this;

		// Convert the command-line parameters into configuration properties
		Properties parameters = new Properties();
		for (CLIParam p : CLIParam.values()) {
			String value = CMSMFLauncher.getParameter(p);
			if (value == null) {
				continue;
			}
			if (p.property != null) {
				final String key = p.property.name;
				if (key != null) {
					parameters.setProperty(key, value);
				}
			}
		}

		this.logger.info(String.format("Launching CMSMF %s mode%n", CMSMFLauncher.getParameter(CLIParam.mode)));

		// If we have command-line parameters, these supersede all other configurations, even if
		// we have a configuration file explicitly listed.
		if (!parameters.isEmpty()) {
			PropertiesManager.addPropertySource(parameters);
		}

		// A configuration file has been specifed, so use its values ahead of the defaults
		if (CMSMFLauncher.getParameter(CLIParam.cfg) != null) {
			PropertiesManager.addPropertySource(CMSMFLauncher.getParameter(CLIParam.cfg));
		}

		// Finally, the catch-all, default configuration
		PropertiesManager.addPropertySource(CMSMFAppConstants.FULLY_QUALIFIED_CONFIG_FILE_NAME);
		PropertiesManager.init();

		this.testMode = (CMSMFLauncher.getParameter(CLIParam.test) != null);

		// Set the filesystem location where files will be created or read from
		this.streamFilesDirectoryLocation = new File(PropertiesManager.getProperty(CMSMFProperties.STREAMS_DIRECTORY,
			"")).getCanonicalFile();
		this.logger.info(String.format("Using streams directory: [%s]", this.contentFilesDirectoryLocation));

		// Set the filesystem location where the content files will be created or read from
		this.contentFilesDirectoryLocation = new File(PropertiesManager.getProperty(CMSMFProperties.CONTENT_DIRECTORY,
			"")).getCanonicalFile();
		this.logger.info(String.format("Using content directory: [%s]", this.contentFilesDirectoryLocation));

		int maxThreads = CMSMFMain.DEFAULT_MAX_THREADS;
		String str = CMSMFLauncher.getParameter(CLIParam.threads);
		try {
			maxThreads = Integer.parseInt(str);
			if (maxThreads < 1) {
				maxThreads = CMSMFMain.DEFAULT_MAX_THREADS;
			}
		} catch (NumberFormatException e) {
			maxThreads = CMSMFMain.DEFAULT_MAX_THREADS;
		}

		// We need to support one more session than threads, because we will have a main thread that
		// will require a session, while the worker threads each have their own.
		this.connectionPool = new GenericObjectPool<IDfSession>(this.connectionFactory, maxThreads,
			GenericObjectPool.WHEN_EXHAUSTED_GROW, 30000, 0, true, true, 30000, maxThreads, 60000, false);
		this.executor = new ThreadPoolExecutor(1, maxThreads, 30, TimeUnit.SECONDS, this.workQueue);

		this.userName = CMSMFLauncher.getParameter(CLIParam.user);
		this.docbase = CMSMFLauncher.getParameter(CLIParam.docbase);
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### CMS Migration Process Started #####");
		}

		final IDfClient dfClient;
		try {
			dfClient = DfClient.getLocalClient();
			if (dfClient == null) {
				// If I don't have a local client then something was not
				// installed
				// correctly so throw an error
				String msg = "No local client was established.  You may want to check the installation of "
					+ "Documentum or this application on this machine.";
				this.logger.error(msg);
				throw new RuntimeException(msg);
			}
		} catch (DfException e) {
			String msg = "No local client was established.  You may want to check the installation of "
				+ "Documentum or this application on this machine.";
			this.logger.error(msg);
			throw new RuntimeException(msg, e);
		}
		this.dfClient = dfClient;

		String passTmp = CMSMFLauncher.getParameter(CLIParam.password);
		if (passTmp != null) {
			try {
				passTmp = RegistryPasswordUtils.decrypt(passTmp);
				if (this.logger.isEnabledFor(Level.INFO)) {
					this.logger.info(String.format("Password decrypted successfully"));
				}
			} catch (Throwable t) {
				// Not encrypted, use literal
				if (this.logger.isEnabledFor(Level.INFO)) {
					this.logger.info(String.format("Password decryption failed, using as literal"));
				}
			}
		}
		this.password = passTmp;

		this.loginInfo = new DfLoginInfo();
		if (this.userName != null) {
			this.loginInfo.setUser(this.userName);
		}
		if (this.password != null) {
			this.loginInfo.setPassword(this.password);
		}

		try {
			run();
			if (this.logger.isEnabledFor(Level.INFO)) {
				this.logger.debug("##### CMS Migration Process finished #####");
			}
		} finally {
			this.executor.shutdownNow();
		}
	}

	protected final void queueWork(Runnable worker) {
		if (worker != null) {
			this.executor.execute(worker);
		}
	}

	public final IDfSession getSession() throws NoSuchElementException, IllegalStateException, Exception {
		return this.connectionPool.borrowObject();
	}

	public final void closeSession(IDfSession session) {
		if (session == null) { return; }
		try {
			this.connectionPool.returnObject(session);
		} catch (Throwable t) {
			// ignore...
		}
	}

	public static CMSMFMain getInstance() {
		return CMSMFMain.instance;
	}

	public final File getStreamFilesDirectory() {
		return this.streamFilesDirectoryLocation;
	}

	public final File getContentFilesDirectory() {
		return this.contentFilesDirectoryLocation;
	}

	public final boolean isTestMode() {
		return this.testMode;
	}

	protected abstract void run() throws IOException, CMSMFFatalException;
}
