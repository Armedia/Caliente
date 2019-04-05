package com.armedia.caliente.tools.dfc.pool;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

public class DfcSessionPool extends BaseShareableLockable {

	private static final Logger LOG = LoggerFactory.getLogger(DfcSessionPool.class);

	static String getId(IDfSession obj) {
		if (obj != null) {
			try {
				return obj.getSessionId();
			} catch (DfException e) {
				if (DfcSessionPool.LOG.isTraceEnabled()) {
					DfcSessionPool.LOG.trace("Exception caught determining the ID of a session", e);
				}
			}
		}
		return "(unknown)";
	}

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final GenericObjectPool<IDfSession> pool;
	private final DfcSessionFactory factory;
	private boolean open = false;

	protected static GenericObjectPoolConfig<IDfSession> getDefaultPoolConfig(CfgTools settings) {
		GenericObjectPoolConfig<IDfSession> config = new GenericObjectPoolConfig<>();
		config.setLifo(true);
		config.setMaxTotal(-1);
		config.setMaxWaitMillis(0); // wait indefinitely
		config.setBlockWhenExhausted(false);
		config.setMinIdle(1);
		// Make sure garbage gets collected quickly
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);
		config.setMaxIdle(Math.max(config.getMinIdle(), config.getMaxTotal() / 2));
		config.setTimeBetweenEvictionRunsMillis(30000);
		config.setMinEvictableIdleTimeMillis(45000);
		return config;
	}

	protected static CfgTools createCfg(Map<String, ?> settings) {
		if ((settings == null) || settings.isEmpty()) { return CfgTools.EMPTY; }
		return new CfgTools(settings);
	}

	protected static CfgTools createCfg(String docbase, String username, String password, CfgTools cfg) {
		Map<String, Object> m = new HashMap<>();
		if (cfg != null) {
			for (String s : cfg.getSettings()) {
				m.put(s, cfg.getObject(s));
			}
		}
		if (docbase != null) {
			m.put(DfcSessionFactory.DOCBASE, docbase);
		}
		if (username != null) {
			m.put(DfcSessionFactory.USERNAME, username);
		}
		if (password != null) {
			m.put(DfcSessionFactory.PASSWORD, password);
		}
		return new CfgTools(m);
	}

	public DfcSessionPool() throws Exception {
		this(DfcSessionPool.createCfg(null));
	}

	public DfcSessionPool(String docbase, String user, String password) throws DfException {
		this(docbase, user, password, DfcSessionPool.createCfg(null));
	}

	public DfcSessionPool(String docbase, String user, String password, Map<String, ?> settings) throws DfException {
		this(docbase, user, password, DfcSessionPool.createCfg(settings));
	}

	public DfcSessionPool(String docbase, String user, String password, CfgTools settings) throws DfException {
		this(DfcSessionPool.createCfg(docbase, user, password, settings));
	}

	public DfcSessionPool(Map<String, ?> settings) throws DfException {
		this(DfcSessionPool.createCfg(settings));
	}

	public DfcSessionPool(CfgTools cfg) throws DfException {
		cfg = Tools.coalesce(cfg, CfgTools.EMPTY);
		this.factory = new DfcSessionFactory(cfg);
		this.pool = new GenericObjectPool<>(this.factory);
		configurePool(cfg);
		this.open = true;
	}

	public final IDfSession acquireSession() throws DfException {
		return shareLocked(() -> {
			if (!this.open) { throw new IllegalStateException("This session factory is not open"); }
			try {
				return this.pool.borrowObject();
			} catch (Exception e) {
				if (DfException.class.isInstance(e)) { throw DfException.class.cast(e); }
				throw new DfException("The backend pool was unable to return a pooled session", e);
			}
		});
	}

	public final void configurePool(CfgTools cfg) {
		shareLockedUpgradable(() -> !this.open,
			() -> this.pool.setConfig(getPoolConfig(Tools.coalesce(cfg, CfgTools.EMPTY))));
	}

	public final void configurePool(Map<String, ?> settings) {
		configurePool(DfcSessionPool.createCfg(settings));
	}

	public final DfcSessionFactory getFactory() {
		return this.factory;
	}

	public final void releaseSession(IDfSession session) {
		// We specifically don't check for openness b/c we don't care...
		if (session == null) { throw new IllegalArgumentException("Must provide a session to return"); }
		try {
			this.pool.returnObject(session);
		} catch (Exception e) {
			if (this.log.isTraceEnabled()) {
				this.log.error("Exception caught returning a session [{}] to the pool", DfcSessionPool.getId(session),
					e);
			}
		}
	}

	protected void doClose() {
	}

	public final void close() {
		shareLockedUpgradable(() -> this.open, () -> {
			try {
				this.pool.close();
				doClose();
			} catch (Exception e) {
				if (this.log.isDebugEnabled()) {
					this.log.error("Exception caught closing this a factory", e);
				} else {
					this.log.error("Exception caught closing this a factory: {}", e.getMessage());
				}
			} finally {
				this.open = false;
			}
		});
	}

	protected GenericObjectPoolConfig<IDfSession> getPoolConfig(CfgTools settings) {
		return DfcSessionPool.getDefaultPoolConfig(settings);
	}
}