package com.armedia.cmf.engine;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectCounter;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfType;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public abstract class TransferEngine<S, V, C extends TransferContext<S, V, F>, F extends ContextFactory<S, V, C, ?>, D extends TransferDelegateFactory<S, V, C, ?>, L> {
	private static final String REFERRENT_ID = "${REFERRENT_ID}$";
	private static final String REFERRENT_KEY = "${REFERRENT_KEY}$";
	private static final String REFERRENT_TYPE = "${REFERRENT_TYPE}$";
	private static final String CONTENT_QUALIFIERS = "${CONTENT_QUALIFIERS}$";
	private static final String CONTENT_PROPERTIES = "${CONTENT_PROPERTIES}$";

	private static final Map<String, Map<String, Object>> REGISTRY = new HashMap<String, Map<String, Object>>();
	private static final Map<String, PluggableServiceLocator<?>> LOCATORS = new HashMap<String, PluggableServiceLocator<?>>();

	private static synchronized <E extends TransferEngine<?, ?, ?, ?, ?, ?>> void registerSubclass(Class<E> subclass) {

		final String key = subclass.getCanonicalName();
		Map<String, Object> m = TransferEngine.REGISTRY.get(key);
		if (m != null) { return; }

		m = new HashMap<String, Object>();
		PluggableServiceLocator<E> locator = new PluggableServiceLocator<E>(subclass);
		locator.setHideErrors(true);
		for (E e : locator) {
			boolean empty = true;
			for (String s : e.getTargetNames()) {
				empty = false;
				Object first = m.get(s);
				if (first != null) {
					// Log the error, and skip the dupe
					continue;
				}
				m.put(s, e);
			}
			if (empty) {
				// Log a warning, then continue
			}
		}
		TransferEngine.REGISTRY.put(key, m);
		TransferEngine.LOCATORS.put(key, locator);
	}

	protected static synchronized <E extends TransferEngine<?, ?, ?, ?, ?, ?>> E getTransferEngine(Class<E> subclass,
		String targetName) {
		if (subclass == null) { throw new IllegalArgumentException("Must provide a valid engine subclass"); }
		if (StringUtils.isEmpty(targetName)) { throw new IllegalArgumentException(
			"Must provide a non-empty, non-null target name"); }
		TransferEngine.registerSubclass(subclass);
		Map<String, Object> m = TransferEngine.REGISTRY.get(subclass.getCanonicalName());
		if (m == null) { return null; }
		return subclass.cast(m.get(targetName));
	}

	protected class ListenerDelegator<R extends Enum<R>> {
		protected final Logger log = TransferEngine.this.log;

		private final CmfObjectCounter<R> counter;

		protected ListenerDelegator(CmfObjectCounter<R> counter) {
			if (counter == null) { throw new IllegalArgumentException("Must provide a counter"); }
			this.counter = counter;
		}

		public final CmfObjectCounter<R> getStoredObjectCounter() {
			return this.counter;
		}

		public final Map<R, Integer> getCummulative() {
			return this.counter.getCummulative();
		}

		public final Map<CmfType, Map<R, Integer>> getCounters() {
			return this.counter.getCounters();
		}

		public final Map<R, Integer> getCounters(CmfType type) {
			return this.counter.getCounters(type);
		}
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private static final int DEFAULT_THREAD_COUNT = 16;
	private static final int MIN_THREAD_COUNT = 1;
	private static final int MAX_THREAD_COUNT = 32;

	private static final int DEFAULT_BACKLOG_SIZE = 1000;
	private static final int MIN_BACKLOG_SIZE = 10;
	private static final int MAX_BACKLOG_SIZE = 100000;

	private final List<L> listeners = new ArrayList<L>();

	private int backlogSize = TransferEngine.DEFAULT_BACKLOG_SIZE;
	private int threadCount = TransferEngine.DEFAULT_THREAD_COUNT;

	public TransferEngine() {
		this(TransferEngine.DEFAULT_THREAD_COUNT, TransferEngine.DEFAULT_BACKLOG_SIZE);
	}

	public TransferEngine(int threadCount) {
		this(threadCount, TransferEngine.DEFAULT_BACKLOG_SIZE);
	}

	public TransferEngine(int threadCount, int backlogSize) {
		this.backlogSize = Tools.ensureBetween(TransferEngine.MIN_BACKLOG_SIZE, backlogSize,
			TransferEngine.MAX_BACKLOG_SIZE);
		this.threadCount = Tools.ensureBetween(TransferEngine.MIN_THREAD_COUNT, threadCount,
			TransferEngine.MAX_THREAD_COUNT);
	}

	protected boolean checkSupported(Set<CmfType> excludes, CmfType type) {
		return !excludes.contains(type);
	}

	public final synchronized boolean addListener(L listener) {
		if (listener != null) { return this.listeners.add(listener); }
		return false;
	}

	public final synchronized boolean removeListener(L listener) {
		if (listener != null) { return this.listeners.remove(listener); }
		return false;
	}

	protected final synchronized Collection<L> getListeners() {
		return new ArrayList<L>(this.listeners);
	}

	protected final synchronized int getBacklogSize() {
		return this.backlogSize;
	}

	protected final synchronized int setBacklogSize(int backlogSize) {
		int old = this.backlogSize;
		this.backlogSize = Tools.ensureBetween(TransferEngine.MIN_BACKLOG_SIZE, backlogSize,
			TransferEngine.MAX_BACKLOG_SIZE);
		return old;
	}

	protected final synchronized int getThreadCount() {
		return this.threadCount;
	}

	protected final synchronized int setThreadCount(int threadCount) {
		int old = this.threadCount;
		this.threadCount = Tools.ensureBetween(TransferEngine.MIN_THREAD_COUNT, threadCount,
			TransferEngine.MAX_THREAD_COUNT);
		return old;
	}

	protected final List<ContentInfo> getContentInfo(CmfObject<V> marshaled) throws Exception {
		if (marshaled == null) { throw new IllegalArgumentException("Must provide a marshaled object to analyze"); }
		CmfProperty<V> qualifiers = marshaled.getProperty(TransferEngine.CONTENT_QUALIFIERS);
		CmfProperty<V> properties = marshaled.getProperty(TransferEngine.CONTENT_PROPERTIES);
		List<ContentInfo> info = new ArrayList<ContentInfo>();
		if ((qualifiers != null) && (properties != null)) {
			if (qualifiers.getValueCount() != properties.getValueCount()) { throw new Exception(String.format(
				"Attribute count mismatch - %d qualifiers and %d properties for %s [%s](%s)",
				qualifiers.getValueCount(), properties.getValueCount(), marshaled.getType(), marshaled.getLabel(),
				marshaled.getId())); }
			for (int i = 0; i < qualifiers.getValueCount(); i++) {
				V q = qualifiers.getValue(i);
				V p = properties.getValue(i);
				info.add(new ContentInfo(Tools.toString(q), Tools.toString(p)));
			}
		}
		return info;
	}

	protected final void setContentInfo(CmfObject<V> marshaled, List<ContentInfo> contents) {
		if (marshaled == null) { throw new IllegalArgumentException("Must provide a marshaled object to analyze"); }
		if (contents == null) { return; }
		CmfProperty<V> q = new CmfProperty<V>(TransferEngine.CONTENT_QUALIFIERS, CmfDataType.STRING, true);
		marshaled.setProperty(q);
		CmfProperty<V> p = new CmfProperty<V>(TransferEngine.CONTENT_PROPERTIES, CmfDataType.STRING, true);
		marshaled.setProperty(p);
		for (ContentInfo d : contents) {
			q.addValue(getValue(CmfDataType.STRING, d.getQualifier()));
			p.addValue(getValue(CmfDataType.STRING, d.encodeProperties()));
		}
	}

	protected final ExecutorService newExecutor(int threadCount) {
		return new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());
	}

	protected abstract String getProductName(S session) throws Exception;

	protected abstract String getProductVersion(S session) throws Exception;

	protected abstract V getValue(CmfDataType type, Object value);

	protected abstract CmfAttributeTranslator<V> getTranslator();

	protected abstract SessionFactory<S> newSessionFactory(CfgTools cfg) throws Exception;

	protected abstract F newContextFactory(S session, CfgTools cfg) throws Exception;

	protected abstract D newDelegateFactory(S session, CfgTools cfg) throws Exception;

	protected abstract Set<String> getTargetNames();

	public final ExportTarget getReferrent(CmfObject<V> marshaled) {
		if (marshaled == null) { throw new IllegalArgumentException("Must provide a marshaled object to analyze"); }
		CmfProperty<V> referrentType = marshaled.getProperty(TransferEngine.REFERRENT_TYPE);
		CmfProperty<V> referrentId = marshaled.getProperty(TransferEngine.REFERRENT_ID);
		CmfProperty<V> referrentKey = marshaled.getProperty(TransferEngine.REFERRENT_KEY);
		if ((referrentType == null) || (referrentId == null) || (referrentKey == null)) { return null; }
		String type = Tools.toString(referrentType.getValue(), true);
		String id = Tools.toString(referrentId.getValue(), true);
		String key = Tools.toString(referrentKey.getValue(), true);
		if ((type == null) || (id == null) || (key == null)) { return null; }
		return new ExportTarget(CmfType.decodeString(type), id, key);
	}

	public final void setReferrent(CmfObject<V> marshaled, ExportTarget referrent) throws ExportException {
		// Now, add the properties to reference the referrent object
		if (referrent != null) {
			final CmfAttributeTranslator<V> translator = getTranslator();
			CmfProperty<V> referrentType = new CmfProperty<V>(TransferEngine.REFERRENT_TYPE, CmfDataType.STRING, false);
			try {
				referrentType.setValue(translator.getValue(CmfDataType.STRING, referrent.getType().name()));
				marshaled.setProperty(referrentType);
				CmfProperty<V> referrentId = new CmfProperty<V>(TransferEngine.REFERRENT_ID, CmfDataType.STRING, false);
				referrentId.setValue(translator.getValue(CmfDataType.STRING, referrent.getId()));
				marshaled.setProperty(referrentId);
				CmfProperty<V> referrentKey = new CmfProperty<V>(TransferEngine.REFERRENT_KEY, CmfDataType.STRING,
					false);
				referrentId.setValue(translator.getValue(CmfDataType.STRING, referrent.getSearchKey()));
				marshaled.setProperty(referrentKey);
			} catch (ParseException e) {
				// This should never happen...
				throw new ExportException("Failed to store the referrent information", e);
			}
		}
	}
}