package com.delta.cmsmf.cms;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import com.delta.cmsmf.cms.pool.DctmSessionManager;
import com.delta.cmsmf.utils.ClasspathPatcher;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

public abstract class AbstractTest {

	protected enum DocumentumType {
		//
		DM_USER(CmsObjectType.USER),
		DM_GROUP(CmsObjectType.GROUP),
		DM_ACL(CmsObjectType.ACL),
		DM_TYPE(CmsObjectType.TYPE),
		DM_FORMAT(CmsObjectType.FORMAT),
		DM_FOLDER(CmsObjectType.FOLDER),
		DM_DOCUMENT(CmsObjectType.DOCUMENT),
		DM_CONTENT(CmsObjectType.CONTENT, "dmr_content");

		public final CmsObjectType cmsType;
		public final String dmTable;

		private DocumentumType(CmsObjectType cmsType) {
			this(cmsType, null);
		}

		private DocumentumType(CmsObjectType cmsType, String dmTable) {
			this.cmsType = cmsType;
			if (dmTable == null) {
				dmTable = name().toLowerCase();
			}
			this.dmTable = dmTable;
		}

		private static Map<CmsObjectType, DocumentumType> MAP = new EnumMap<CmsObjectType, DocumentumType>(
			CmsObjectType.class);

		public static DocumentumType decode(CmsObject<?> object) {
			if (object == null) { throw new IllegalArgumentException("Must provide an object to decode from"); }
			return DocumentumType.decode(object.getType());
		}

		public static DocumentumType decode(CmsObjectType type) {
			synchronized (DocumentumType.MAP) {
				if (DocumentumType.MAP.isEmpty()) {
					for (DocumentumType t : DocumentumType.values()) {
						DocumentumType.MAP.put(t.cmsType, t);
					}
				}
			}
			if (type == null) { throw new IllegalArgumentException("Must provide a type to decode"); }
			DocumentumType ret = DocumentumType.MAP.get(type);
			if (ret == null) {
				switch (type) {
					case DOCUMENT_REF:
						return DocumentumType.DM_DOCUMENT;
					default:
						throw new IllegalArgumentException(String.format("Unsupported type [%s]", type));
				}
			}
			return ret;
		}
	}

	protected final Logger log = Logger.getLogger(AbstractTest.class);

	private static final String H2_DRIVER = "org.h2.Driver";
	private static final String H2_URL = "jdbc:h2:mem:cmsmf";

	protected static final ConnectionFactory CONNECTION_FACTORY;

	private static final DctmSessionManager SOURCE_SESSION_MANAGER;
	private static final DctmSessionManager TARGET_SESSION_MANAGER;

	protected static final ResultSetHandler<Integer> HANDLER_COUNT = new ResultSetHandler<Integer>() {
		@Override
		public Integer handle(ResultSet rs) throws SQLException {
			if (!rs.next()) { return -1; }
			return rs.getInt(1);
		}
	};

	protected static final ResultSetHandler<Boolean> HANDLER_EXISTS = new ResultSetHandler<Boolean>() {
		@Override
		public Boolean handle(ResultSet rs) throws SQLException {
			return rs.next();
		}
	};

	static {
		if (!DbUtils.loadDriver(AbstractTest.H2_DRIVER)) { throw new RuntimeException(String.format(
			"Failed to locate the JDBC driver class [%s]", AbstractTest.H2_DRIVER)); }
		CONNECTION_FACTORY = new DriverManagerConnectionFactory(AbstractTest.H2_URL, null);

		try {
			String userDir = System.getProperty("user.dir");
			System.err.printf("Adding [%s] to the classpath...%n", userDir);
			ClasspathPatcher.addToClassPath(userDir);
		} catch (IOException e) {
			throw new RuntimeException("Failed to add the current working directory to the classpath", e);
		}

		URL properties = Thread.currentThread().getContextClassLoader().getResource("test.properties");
		if (properties == null) { throw new RuntimeException("Failed to load test.properties"); }
		PropertiesConfiguration cfg = null;
		try {
			cfg = new PropertiesConfiguration(properties);
		} catch (ConfigurationException e) {
			throw new RuntimeException("Failed to load the properties configuration", e);
		}

		String docbase = cfg.getString("source.docbase");
		String username = cfg.getString("source.username");
		String password = cfg.getString("source.password");
		SOURCE_SESSION_MANAGER = new DctmSessionManager(docbase, username, password);
		try {
			// Test it out
			AbstractTest.SOURCE_SESSION_MANAGER.releaseSession(AbstractTest.SOURCE_SESSION_MANAGER.acquireSession());
		} catch (Throwable t) {
			throw new RuntimeException(String.format("Failed to initialize the source session manager at [%s|%s|%s]",
				docbase, username, password));
		}

		docbase = cfg.getString("target.docbase", docbase);
		username = cfg.getString("target.username", username);
		password = cfg.getString("target.password", password);
		TARGET_SESSION_MANAGER = new DctmSessionManager(docbase, username, password);
		try {
			// Test it out
			AbstractTest.TARGET_SESSION_MANAGER.releaseSession(AbstractTest.TARGET_SESSION_MANAGER.acquireSession());
		} catch (Throwable t) {
			throw new RuntimeException(String.format("Failed to initialize the target session manager at [%s|%s|%s]",
				docbase, username, password));
		}
	}

	private DataSource dataSource = null;
	private ObjectPool<PoolableConnection> pool = null;
	private final File fsDir;

	protected AbstractTest() {
		File bd;
		try {
			bd = new File(System.getProperty("user.dir")).getCanonicalFile();
		} catch (IOException e) {
			throw new RuntimeException("Failed to canonicalize the working directory");
		}
		this.fsDir = new File(bd, "test-fs");
	}

	protected final File getFsDir() {
		return this.fsDir;
	}

	@Before
	public void setUp() throws Throwable {
		baseSetup();
	}

	protected final void baseSetup() {
		System.out.printf("Preparing the memory-based connection pool");
		this.pool = new GenericObjectPool<PoolableConnection>();
		@SuppressWarnings("unused")
		final PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
			AbstractTest.CONNECTION_FACTORY, this.pool, null, null, false, true);
		this.dataSource = new PoolingDataSource(this.pool);
		this.log.info("Memory-based connection pool ready");
		try {
			FileUtils.deleteDirectory(this.fsDir);
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to delete the directory [%s]",
				this.fsDir.getAbsolutePath()), e);
		}
		this.log.info("FS directory cleaned out");
		this.fsDir.mkdirs();
	}

	@After
	public void tearDown() throws Throwable {
		baseTearDown();
	}

	protected final void closeQuietly(IDfCollection collection) {
		if (collection == null) { return; }
		try {
			collection.close();
		} catch (DfException e) {
		}
	}

	protected final DctmSessionManager getSourceSessionManager() {
		return AbstractTest.SOURCE_SESSION_MANAGER;
	}

	protected final IDfSession acquireSourceSession() {
		return AbstractTest.SOURCE_SESSION_MANAGER.acquireSession();
	}

	protected final void releaseSourceSession(IDfSession session) {
		AbstractTest.SOURCE_SESSION_MANAGER.releaseSession(session);
	}

	protected final DctmSessionManager getTargetSessionManager() {
		return AbstractTest.TARGET_SESSION_MANAGER;
	}

	protected final IDfSession acquireTargetSession() {
		return AbstractTest.TARGET_SESSION_MANAGER.acquireSession();
	}

	protected final void releaseTargetSession(IDfSession session) {
		AbstractTest.TARGET_SESSION_MANAGER.releaseSession(session);
	}

	protected final void baseTearDown() {
		FileUtils.deleteQuietly(this.fsDir);
		try {
			this.pool.close();
		} catch (Exception e) {
		}
		this.pool = null;
		this.dataSource = null;
		this.log.info("Closing the memory-based connection pool");
	}

	protected final DataSource getDataSource() {
		return this.dataSource;
	}
}