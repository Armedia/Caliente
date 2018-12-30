package com.armedia.caliente.engine;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.filter.ObjectFilter;
import com.armedia.caliente.engine.dynamic.mapper.AttributeMapper;
import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataLoader;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.tools.MappingTools;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValueMapper;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public abstract class TransferEngineWorker< //
	LISTENER extends TransferListener, //
	RESULT extends Enum<RESULT>, //
	EXCEPTION extends TransferEngineException, //
	SESSION, //
	VALUE, //
	CONTEXT extends TransferContext<SESSION, VALUE, CONTEXT_FACTORY>, //
	CONTEXT_FACTORY extends TransferContextFactory<SESSION, VALUE, CONTEXT, ?>, //
	DELEGATE_FACTORY extends TransferDelegateFactory<SESSION, VALUE, CONTEXT, ?> //
> {

	private static final String REFERRENT_ID = "${REFERRENT_ID}$";
	private static final String REFERRENT_KEY = "${REFERRENT_KEY}$";
	private static final String REFERRENT_TYPE = "${REFERRENT_TYPE}$";

	private static final Pattern SETTING_NAME_PATTERN = Pattern.compile("^[\\w&&[^\\d]][\\w]*$");

	protected abstract class ListenerPropagator<R extends Enum<R>, L> implements InvocationHandler {
		protected final Logger log = TransferEngineWorker.this.log;

		private final CmfObjectCounter<R> counter;
		private final Collection<L> listeners;
		protected final L listenerProxy;

		protected ListenerPropagator(CmfObjectCounter<R> counter, Collection<L> listeners, Class<L> listenerClass) {
			if (counter == null) { throw new IllegalArgumentException("Must provide a counter"); }
			this.counter = counter;
			this.listeners = listeners;
			this.listenerProxy = listenerClass
				.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] {
					listenerClass
			}, this));
		}

		public final L getListenerProxy() {
			return this.listenerProxy;
		}

		public final CmfObjectCounter<R> getStoredObjectCounter() {
			return this.counter;
		}

		public final Map<R, Long> getCummulative() {
			return this.counter.getCummulative();
		}

		public final Map<CmfType, Map<R, Long>> getCounters() {
			return this.counter.getCounters();
		}

		public final Map<R, Long> getCounters(CmfType type) {
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
			for (L l : this.listeners) {
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

	private final List<LISTENER> listeners = new ArrayList<>();

	protected final boolean supportsDuplicateFileNames;
	protected final CmfCrypt crypto;
	protected final String cfgNamePrefix;

	protected final Class<RESULT> resultClass;

	protected final Logger output;
	protected final WarningTracker warningTracker;
	protected final File baseData;
	protected final CmfObjectStore<?, ?> objectStore;
	protected final CmfContentStore<?, ?, ?> contentStore;
	protected final CfgTools settings;

	protected TransferEngineWorker(Class<RESULT> resultClass, final Logger output, final WarningTracker warningTracker,
		final File baseData, final CmfObjectStore<?, ?> objectStore, final CmfContentStore<?, ?, ?> contentStore,
		Map<String, ?> settings, CmfCrypt crypto, String cfgNamePrefix, boolean supportsDuplicateNames) {
		this.resultClass = Objects.requireNonNull(resultClass, "Must provide a valid RESULT class");
		this.crypto = crypto;
		if (!StringUtils.isEmpty(cfgNamePrefix)) {
			cfgNamePrefix = String.format("%s-", cfgNamePrefix);
		} else {
			cfgNamePrefix = "";
		}
		this.cfgNamePrefix = cfgNamePrefix;
		this.supportsDuplicateFileNames = supportsDuplicateNames;
		this.output = output;
		this.warningTracker = warningTracker;
		this.baseData = baseData;
		this.objectStore = objectStore;
		this.contentStore = contentStore;
		this.settings = new CfgTools(new TreeMap<>(settings));
	}

	protected boolean checkSupported(Set<CmfType> excludes, CmfType type) {
		return !excludes.contains(type);
	}

	public final synchronized boolean addListener(LISTENER listener) {
		if (listener != null) { return this.listeners.add(listener); }
		return false;
	}

	public final synchronized boolean removeListener(LISTENER listener) {
		if (listener != null) { return this.listeners.remove(listener); }
		return false;
	}

	protected final synchronized Collection<LISTENER> getListeners() {
		return new ArrayList<>(this.listeners);
	}

	protected final int getThreadCount(CfgTools settings) {
		Object tc = settings.getObject(TransferSetting.THREAD_COUNT);
		if (!Number.class.isInstance(tc)) {
			if (tc == null) {
				tc = TransferEngineWorker.DEFAULT_THREAD_COUNT;
			} else {
				tc = Integer.valueOf(tc.toString());
			}
		}
		return Tools.ensureBetween(TransferEngineWorker.MIN_THREAD_COUNT, Number.class.cast(tc).intValue(),
			TransferEngineWorker.MAX_THREAD_COUNT);
	}

	protected abstract VALUE getValue(CmfDataType type, Object value);

	protected abstract CmfAttributeTranslator<VALUE> getTranslator();

	protected abstract SessionFactory<SESSION> newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception;

	protected abstract CONTEXT_FACTORY newContextFactory(SESSION session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception;

	protected abstract DELEGATE_FACTORY newDelegateFactory(SESSION session, CfgTools cfg) throws Exception;

	public final CmfCrypt getCrypto() {
		return this.crypto;
	}

	public final ExportTarget getReferrent(CmfObject<VALUE> marshaled) {
		if (marshaled == null) { throw new IllegalArgumentException("Must provide a marshaled object to analyze"); }
		CmfProperty<VALUE> referrentType = marshaled.getProperty(TransferEngineWorker.REFERRENT_TYPE);
		CmfProperty<VALUE> referrentId = marshaled.getProperty(TransferEngineWorker.REFERRENT_ID);
		CmfProperty<VALUE> referrentKey = marshaled.getProperty(TransferEngineWorker.REFERRENT_KEY);
		if ((referrentType == null) || (referrentId == null) || (referrentKey == null)) { return null; }
		String type = Tools.toString(referrentType.getValue(), true);
		String id = Tools.toString(referrentId.getValue(), true);
		String key = Tools.toString(referrentKey.getValue(), true);
		if ((type == null) || (id == null)) { return null; }
		return new ExportTarget(CmfType.valueOf(type), id, key);
	}

	public final void setReferrent(CmfObject<VALUE> marshaled, ExportTarget referrent) throws ExportException {
		// Now, add the properties to reference the referrent object
		if (referrent != null) {
			final CmfAttributeTranslator<VALUE> translator = getTranslator();
			try {
				CmfProperty<VALUE> referrentType = new CmfProperty<>(TransferEngineWorker.REFERRENT_TYPE,
					CmfDataType.STRING, false);
				referrentType.setValue(translator.getValue(CmfDataType.STRING, referrent.getType().name()));
				marshaled.setProperty(referrentType);

				CmfProperty<VALUE> referrentId = new CmfProperty<>(TransferEngineWorker.REFERRENT_ID,
					CmfDataType.STRING, false);
				referrentId.setValue(translator.getValue(CmfDataType.STRING, referrent.getId()));
				marshaled.setProperty(referrentId);

				CmfProperty<VALUE> referrentKey = new CmfProperty<>(TransferEngineWorker.REFERRENT_KEY,
					CmfDataType.STRING, false);
				referrentKey.setValue(translator.getValue(CmfDataType.STRING, referrent.getSearchKey()));
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

	public final File getBaseData() {
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
			Matcher m = TransferEngineWorker.SETTING_NAME_PATTERN.matcher(s);
			if (!m.matches()) {
				this.log.warn(String.format("Illegal setting name [%s], skipping", s));
				continue;
			}
			c.add(settings.get(s));
		}
		return Collections.unmodifiableCollection(c);
	}

	protected final void loadPrincipalMappings(CmfValueMapper mapper, CfgTools cfg) throws TransferEngineException {
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

	public final CmfObjectCounter<RESULT> run(Collection<LISTENER> listeners) throws EXCEPTION, CmfStorageException {
		CmfObjectCounter<RESULT> counter = new CmfObjectCounter<>(this.resultClass);
		run(listeners, counter);
		return counter;
	}

	public final synchronized void run(Collection<LISTENER> listeners, CmfObjectCounter<RESULT> counter)
		throws EXCEPTION, CmfStorageException {
		if (counter == null) {
			counter = new CmfObjectCounter<>(this.resultClass);
		}
		work(listeners, counter);
	}

	protected final EXCEPTION newException(String message) {
		return newException(message, null);
	}

	protected abstract EXCEPTION newException(String message, Throwable cause);

	protected abstract void work(Collection<LISTENER> listeners, CmfObjectCounter<RESULT> counter)
		throws EXCEPTION, CmfStorageException;
}