package com.armedia.caliente.engine;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.filter.ObjectFilter;
import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataLoader;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.dynamic.transformer.mapper.AttributeMapper;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.tools.MappingTools;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfValueType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfArchetype;
import com.armedia.caliente.store.CmfValueMapper;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public abstract class TransferEngine< //
	LISTENER extends TransferListener, //
	RESULT extends Enum<RESULT>, //
	EXCEPTION extends TransferException, //
	SESSION, //
	VALUE, //
	CONTEXT extends TransferContext<SESSION, VALUE, CONTEXT_FACTORY>, //
	CONTEXT_FACTORY extends TransferContextFactory<SESSION, VALUE, CONTEXT, ?>, //
	DELEGATE_FACTORY extends TransferDelegateFactory<SESSION, VALUE, CONTEXT, ?>, //
	ENGINE_FACTORY extends TransferEngineFactory<LISTENER, RESULT, EXCEPTION, SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ?> //
> implements Callable<CmfObjectCounter<RESULT>> {

	private static final String REFERRENT_ID = "${REFERRENT_ID}$";
	private static final String REFERRENT_KEY = "${REFERRENT_KEY}$";
	private static final String REFERRENT_TYPE = "${REFERRENT_TYPE}$";

	private static final Pattern SETTING_NAME_PATTERN = Pattern.compile("^[\\w&&[^\\d]][\\w]*$");

	protected abstract class ListenerPropagator<R extends Enum<R>> implements InvocationHandler {
		protected final Logger log = TransferEngine.this.log;

		private final CmfObjectCounter<R> counter;
		private final Collection<LISTENER> listeners;
		protected final LISTENER listenerProxy;

		protected ListenerPropagator(CmfObjectCounter<R> counter, Collection<LISTENER> listeners,
			Class<LISTENER> listenerClass) {
			if (counter == null) { throw new IllegalArgumentException("Must provide a counter"); }
			this.counter = counter;
			this.listeners = listeners;
			this.listenerProxy = listenerClass
				.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] {
					listenerClass
				}, this));
		}

		public final LISTENER getListenerProxy() {
			return this.listenerProxy;
		}

		public final CmfObjectCounter<R> getStoredObjectCounter() {
			return this.counter;
		}

		public final Map<R, Long> getCummulative() {
			return this.counter.getCummulative();
		}

		public final Map<CmfArchetype, Map<R, Long>> getCounters() {
			return this.counter.getCounters();
		}

		public final Map<R, Long> getCounters(CmfArchetype type) {
			return this.counter.getCounters(type);
		}

		@Override
		public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			handleMethod(method.getName(), args);
			propagate(method, args);
			return null;
		}

		protected void handleMethod(String name, Object[] args) throws Throwable {
		}

		protected final void propagate(Method method, Object[] args) {
			for (LISTENER l : this.listeners) {
				try {
					method.invoke(l, args);
				} catch (Throwable t) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", t);
					}
				}
			}
		}
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private static final int MIN_THREAD_COUNT = 1;
	private static final int DEFAULT_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
	private static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 2;

	private final List<LISTENER> listeners = new CopyOnWriteArrayList<>();
	private final Lock runLock = new ReentrantLock();

	protected final boolean supportsDuplicateFileNames;
	protected final CmfCrypt crypto;
	protected final String cfgNamePrefix;

	protected final Class<RESULT> resultClass;
	protected final ENGINE_FACTORY factory;
	protected final Logger output;
	protected final WarningTracker warningTracker;
	protected final Path baseData;
	protected final CmfObjectStore<?, ?> objectStore;
	protected final CmfContentStore<?, ?, ?> contentStore;
	protected final CfgTools settings;

	protected TransferEngine(ENGINE_FACTORY factory, Class<RESULT> resultClass, final Logger output,
		final WarningTracker warningTracker, final File baseData, final CmfObjectStore<?, ?> objectStore,
		final CmfContentStore<?, ?, ?> contentStore, CfgTools settings, String cfgNamePrefix) {
		this.factory = Objects.requireNonNull(factory,
			"Must provide a handle to the factory that created this instance");
		this.resultClass = Objects.requireNonNull(resultClass, "Must provide a valid RESULT class");

		this.crypto = factory.getCrypto();
		this.supportsDuplicateFileNames = factory.isSupportsDuplicateFileNames();

		if (!StringUtils.isEmpty(cfgNamePrefix)) {
			cfgNamePrefix = String.format("%s-", cfgNamePrefix);
		} else {
			cfgNamePrefix = "";
		}
		this.cfgNamePrefix = cfgNamePrefix;
		this.output = output;
		this.warningTracker = warningTracker;
		this.baseData = Tools.canonicalize(baseData).toPath();
		this.objectStore = objectStore;
		this.contentStore = contentStore;
		this.settings = settings;
	}

	protected boolean checkSupported(Set<CmfArchetype> excludes, CmfArchetype type) {
		return !excludes.contains(type);
	}

	public final boolean addListener(LISTENER listener) {
		if (listener != null) { return this.listeners.add(listener); }
		return false;
	}

	public final boolean removeListener(LISTENER listener) {
		if (listener != null) { return this.listeners.remove(listener); }
		return false;
	}

	protected final Collection<LISTENER> getListeners() {
		return new ArrayList<>(this.listeners);
	}

	protected final int getThreadCount(CfgTools settings) {
		Object tc = settings.getObject(TransferSetting.THREAD_COUNT);
		if (!Number.class.isInstance(tc)) {
			if (tc == null) {
				tc = TransferEngine.DEFAULT_THREAD_COUNT;
			} else {
				tc = Integer.valueOf(tc.toString());
			}
		}
		return Tools.ensureBetween(TransferEngine.MIN_THREAD_COUNT, Number.class.cast(tc).intValue(),
			TransferEngine.MAX_THREAD_COUNT);
	}

	protected abstract VALUE getValue(CmfValueType type, Object value);

	protected abstract CmfAttributeTranslator<VALUE> getTranslator();

	protected abstract SessionFactory<SESSION> newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception;

	protected final SessionFactory<SESSION> constructSessionFactory(CfgTools cfg, CmfCrypt crypto)
		throws SessionFactoryException {
		try {
			return newSessionFactory(cfg, crypto);
		} catch (Exception e) {
			throw new SessionFactoryException(String.format(
				"Failed to construct a new SessionFactory instance for engine %s", getClass().getSimpleName()), e);
		}
	}

	protected abstract CONTEXT_FACTORY newContextFactory(SESSION session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception;

	protected abstract DELEGATE_FACTORY newDelegateFactory(SESSION session, CfgTools cfg) throws Exception;

	public final CmfCrypt getCrypto() {
		return this.crypto;
	}

	public final ExportTarget getReferrent(CmfObject<VALUE> marshaled) {
		if (marshaled == null) { throw new IllegalArgumentException("Must provide a marshaled object to analyze"); }
		CmfProperty<VALUE> referrentType = marshaled.getProperty(TransferEngine.REFERRENT_TYPE);
		CmfProperty<VALUE> referrentId = marshaled.getProperty(TransferEngine.REFERRENT_ID);
		CmfProperty<VALUE> referrentKey = marshaled.getProperty(TransferEngine.REFERRENT_KEY);
		if ((referrentType == null) || (referrentId == null) || (referrentKey == null)) { return null; }
		String type = Tools.toString(referrentType.getValue(), true);
		String id = Tools.toString(referrentId.getValue(), true);
		String key = Tools.toString(referrentKey.getValue(), true);
		if ((type == null) || (id == null)) { return null; }
		return new ExportTarget(CmfArchetype.valueOf(type), id, key);
	}

	public final void setReferrent(CmfObject<VALUE> marshaled, ExportTarget referrent) throws ExportException {
		// Now, add the properties to reference the referrent object
		if (referrent != null) {
			final CmfAttributeTranslator<VALUE> translator = getTranslator();
			try {
				CmfProperty<VALUE> referrentType = new CmfProperty<>(TransferEngine.REFERRENT_TYPE, CmfValueType.STRING,
					false);
				referrentType.setValue(translator.getValue(CmfValueType.STRING, referrent.getType().name()));
				marshaled.setProperty(referrentType);

				CmfProperty<VALUE> referrentId = new CmfProperty<>(TransferEngine.REFERRENT_ID, CmfValueType.STRING,
					false);
				referrentId.setValue(translator.getValue(CmfValueType.STRING, referrent.getId()));
				marshaled.setProperty(referrentId);

				CmfProperty<VALUE> referrentKey = new CmfProperty<>(TransferEngine.REFERRENT_KEY, CmfValueType.STRING,
					false);
				referrentKey.setValue(translator.getValue(CmfValueType.STRING, referrent.getSearchKey()));
				marshaled.setProperty(referrentKey);
			} catch (ParseException e) {
				// This should never happen...
				throw new ExportException("Failed to store the referrent information", e);
			}
		}
	}

	protected final String getCfgNamePrefix() {
		return this.cfgNamePrefix;
	}

	public final Class<RESULT> getResultClass() {
		return this.resultClass;
	}

	public final Logger getOutput() {
		return this.output;
	}

	public final WarningTracker getWarningTracker() {
		return this.warningTracker;
	}

	public final Path getBaseData() {
		return this.baseData;
	}

	public final CmfObjectStore<?, ?> getObjectStore() {
		return this.objectStore;
	}

	public final CmfContentStore<?, ?, ?> getContentStore() {
		return this.contentStore;
	}

	public final CfgTools getSettings() {
		return this.settings;
	}

	public final boolean isSupportsDuplicateFileNames() {
		return this.supportsDuplicateFileNames;
	}

	public final Collection<TransferEngineSetting> getSupportedSettings() {
		Map<String, TransferEngineSetting> settings = new TreeMap<>();
		for (TransferSetting s : TransferSetting.values()) {
			settings.put(s.getLabel(), s);
		}
		Collection<TransferEngineSetting> c = new ArrayList<>();
		getSupportedSettings(c);
		for (TransferEngineSetting s : c) {
			if (s == null) {
				continue;
			}
			TransferEngineSetting old = settings.put(s.getLabel(), s);
			if (old != null) {
				this.log.warn(String.format("Duplicate setting name [%s]", s.getLabel()));
			}
		}
		c = new ArrayList<>(settings.size());
		for (String s : settings.keySet()) {
			Matcher m = TransferEngine.SETTING_NAME_PATTERN.matcher(s);
			if (!m.matches()) {
				this.log.warn(String.format("Illegal setting name [%s], skipping", s));
				continue;
			}
			c.add(settings.get(s));
		}
		return Collections.unmodifiableCollection(c);
	}

	protected final void loadPrincipalMappings(CmfValueMapper mapper, CfgTools cfg) throws TransferException {
		for (PrincipalType t : PrincipalType.values()) {
			Properties p = new Properties();
			if (!MappingTools.loadMap(this.log, cfg, t.getSetting(), p)) {
				continue;
			}

			for (String s : p.stringPropertyNames()) {
				String v = StringUtils.strip(p.getProperty(s));
				if (!StringUtils.isEmpty(s)) {
					mapper.setMapping(t.getObjectType(), t.getMappingName(), s, v);
				}
			}
		}
	}

	protected final Transformer getTransformer(CfgTools cfg, AttributeMapper attributeMapper) throws Exception {
		String xformDefault = String.format("%s%s", this.cfgNamePrefix, Transformer.getDefaultLocation());
		String xform = cfg.getString(TransferSetting.TRANSFORMATION.getLabel());

		String metaDefault = String.format("%s%s", this.cfgNamePrefix, ExternalMetadataLoader.getDefaultLocation());
		String meta = cfg.getString(TransferSetting.EXTERNAL_METADATA.getLabel());

		ExternalMetadataLoader emdl = ExternalMetadataLoader
			.getExternalMetadataLoader(Tools.coalesce(meta, metaDefault), (meta != null));

		return Transformer.getTransformer(Tools.coalesce(xform, xformDefault), emdl, attributeMapper, (xform != null));
	}

	protected final ObjectFilter getFilter(CfgTools cfg) throws Exception {
		String filterDefault = String.format("%s%s", this.cfgNamePrefix, ObjectFilter.getDefaultLocation());
		String filter = cfg.getString(TransferSetting.FILTER.getLabel());
		return ObjectFilter.getObjectFilter(Tools.coalesce(filter, filterDefault), (filter != null));
	}

	protected void getSupportedSettings(Collection<TransferEngineSetting> settings) {
	}

	public final CmfObjectCounter<RESULT> run() throws EXCEPTION, CmfStorageException {
		CmfObjectCounter<RESULT> counter = new CmfObjectCounter<>(this.resultClass);
		run(counter);
		return counter;
	}

	@Override
	public CmfObjectCounter<RESULT> call() throws EXCEPTION, CmfStorageException {
		return run();
	}

	public final void run(CmfObjectCounter<RESULT> counter) throws EXCEPTION, CmfStorageException {
		if (!this.runLock.tryLock()) {
			throw newException(String.format("This %s instance is already running a job - can't run a second one",
				getClass().getSimpleName()));
		}
		try {
			if (counter == null) {
				counter = new CmfObjectCounter<>(this.resultClass);
			}
			work(counter);
		} finally {
			this.runLock.unlock();
		}
	}

	protected final EXCEPTION newException(String message) {
		return newException(message, null);
	}

	protected abstract EXCEPTION newException(String message, Throwable cause);

	protected abstract void work(CmfObjectCounter<RESULT> counter) throws EXCEPTION, CmfStorageException;
}