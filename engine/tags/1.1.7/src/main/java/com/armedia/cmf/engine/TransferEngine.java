package com.armedia.cmf.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectCounter;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.commons.utilities.ArrayIterator;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public abstract class TransferEngine<S, T, V, C extends TransferContext<S, T, V>, L> {

	private static final String CONTENT_QUALIFIER = "${CONTENT_QUALIFIER}$";

	private static final Map<String, Map<String, Object>> REGISTRY = new HashMap<String, Map<String, Object>>();
	private static final Map<String, PluggableServiceLocator<?>> LOCATORS = new HashMap<String, PluggableServiceLocator<?>>();

	private static synchronized <E extends TransferEngine<?, ?, ?, ?, ?>> void registerSubclass(Class<E> subclass) {

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

	private static StoredObjectType decodeObjectType(Object o) {
		if (o == null) { return null; }
		if (o instanceof StoredObjectType) { return StoredObjectType.class.cast(o); }
		if (o instanceof String) {
			try {
				return StoredObjectType.decodeString(String.valueOf(o));
			} catch (IllegalArgumentException e) {
				// Do nothing...
			}
		}
		return null;
	}

	private static Iterable<?> getAsIterable(final Object o) {
		if (o == null) { return Collections.emptyList(); }
		if (o instanceof Iterable) { return Iterable.class.cast(o); }
		if (o instanceof String) { return new StrTokenizer(o.toString(), ',').getTokenList(); }
		if (o.getClass().isArray()) {
			if (!o.getClass().getComponentType().isPrimitive()) { return new Iterable<Object>() {
				private final Object[] arr = (Object[]) o;

				@Override
				public Iterator<Object> iterator() {
					return new ArrayIterator<Object>(this.arr);
				}

			}; }
		}
		return Collections.emptyList();
	}

	protected static synchronized <E extends TransferEngine<?, ?, ?, ?, ?>> E getTransferEngine(Class<E> subclass,
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

		private final StoredObjectCounter<R> counter;

		protected ListenerDelegator(StoredObjectCounter<R> counter) {
			if (counter == null) { throw new IllegalArgumentException("Must provide a counter"); }
			this.counter = counter;
		}

		public final StoredObjectCounter<R> getStoredObjectCounter() {
			return this.counter;
		}

		public final Map<R, Integer> getCummulative() {
			return this.counter.getCummulative();
		}

		public final Map<StoredObjectType, Map<R, Integer>> getCounters() {
			return this.counter.getCounters();
		}

		public final Map<R, Integer> getCounters(StoredObjectType type) {
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

	private final ThreadLocal<Map<StoredObjectType, Boolean>> supported = new ThreadLocal<Map<StoredObjectType, Boolean>>();

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

	protected final void configure(CfgTools cfg) throws TransferEngineException {
		Map<StoredObjectType, Boolean> m = this.supported.get();
		if (m == null) {
			m = new EnumMap<StoredObjectType, Boolean>(StoredObjectType.class);
			this.supported.set(m);
		}
		m.clear();
		for (Object o : TransferEngine.getAsIterable(cfg.getObject(TransferEngineSetting.EXCLUDE_TYPES))) {
			StoredObjectType t = TransferEngine.decodeObjectType(o);
			if (t != null) {
				m.put(t, Boolean.FALSE);
			}
		}
		for (StoredObjectType t : StoredObjectType.values()) {
			if (!m.containsKey(t)) {
				m.put(t, Boolean.TRUE);
			}
		}
		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Supported types for this %s instance: %s", getClass().getSimpleName(), m));
		}
	}

	protected final void cleanup() {
		this.supported.set(null);
	}

	public final boolean isSupported(StoredObjectType type) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to check for"); }
		Map<StoredObjectType, Boolean> m = this.supported.get();
		Boolean result = (m != null ? Tools.coalesce(m.get(type), Boolean.TRUE) : Boolean.TRUE);
		return result && checkSupported(type);
	}

	protected boolean checkSupported(StoredObjectType type) {
		return true;
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

	protected final String getContentQualifier(StoredObject<V> marshaled) {
		if (marshaled == null) { throw new IllegalArgumentException("Must provide a marshaled object to analyze"); }
		StoredProperty<V> contentQualifier = marshaled.getProperty(TransferEngine.CONTENT_QUALIFIER);
		if (contentQualifier == null) { return null; }
		return Tools.toString(contentQualifier.getValue());
	}

	protected final void setContentQualifier(StoredObject<V> marshaled, String qualifier) {
		StoredProperty<V> p = new StoredProperty<V>(TransferEngine.CONTENT_QUALIFIER, StoredDataType.STRING, true);
		p.setValue(getValue(StoredDataType.STRING, qualifier));
		marshaled.setProperty(p);
	}

	protected final ExecutorService newExecutor(int threadCount) {
		return new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());
	}

	protected abstract V getValue(StoredDataType type, Object value);

	protected abstract ObjectStorageTranslator<T, V> getTranslator();

	protected abstract SessionFactory<S> newSessionFactory(CfgTools cfg) throws Exception;

	protected abstract ContextFactory<S, T, V, C, ?> newContextFactory(CfgTools cfg) throws Exception;

	protected abstract Set<String> getTargetNames();
}