package com.armedia.caliente.engine;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.filter.ObjectFilter;
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
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValueMapper;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public abstract class TransferEngine<S, V, C extends TransferContext<S, V, F>, F extends TransferContextFactory<S, V, C, ?>, D extends TransferDelegateFactory<S, V, C, ?>, L> {
	private static final String REFERRENT_ID = "${REFERRENT_ID}$";
	private static final String REFERRENT_KEY = "${REFERRENT_KEY}$";
	private static final String REFERRENT_TYPE = "${REFERRENT_TYPE}$";

	private static final Pattern SETTING_NAME_PATTERN = Pattern.compile("^[\\w&&[^\\d]][\\w]*$");

	private static final Map<String, Map<String, Object>> REGISTRY = new HashMap<>();
	private static final Map<String, PluggableServiceLocator<?>> LOCATORS = new HashMap<>();

	private static synchronized <E extends TransferEngine<?, ?, ?, ?, ?, ?>> void registerSubclass(Class<E> subclass) {

		final String key = subclass.getCanonicalName();
		Map<String, Object> m = TransferEngine.REGISTRY.get(key);
		if (m != null) { return; }

		m = new HashMap<>();
		PluggableServiceLocator<E> locator = new PluggableServiceLocator<>(subclass);
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
		if (StringUtils.isEmpty(
			targetName)) { throw new IllegalArgumentException("Must provide a non-empty, non-null target name"); }
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

		public final Map<R, Long> getCummulative() {
			return this.counter.getCummulative();
		}

		public final Map<CmfType, Map<R, Long>> getCounters() {
			return this.counter.getCounters();
		}

		public final Map<R, Long> getCounters(CmfType type) {
			return this.counter.getCounters(type);
		}
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private static final int MIN_THREAD_COUNT = 1;
	private static final int DEFAULT_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
	private static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 2;

	private final List<L> listeners = new ArrayList<>();

	protected final CmfCrypt crypto;

	private final boolean supportsDuplicateFileNames;
	private final String cfgNamePrefix;

	public TransferEngine(CmfCrypt crypto, String cfgNamePrefix) {
		this(crypto, cfgNamePrefix, false);
	}

	public TransferEngine(CmfCrypt crypto, String cfgNamePrefix, boolean supportsDuplicateNames) {
		this.crypto = crypto;
		if (!StringUtils.isEmpty(cfgNamePrefix)) {
			cfgNamePrefix = String.format("%s-", cfgNamePrefix);
		} else {
			cfgNamePrefix = "";
		}
		this.cfgNamePrefix = cfgNamePrefix;
		this.supportsDuplicateFileNames = supportsDuplicateNames;
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

	protected abstract V getValue(CmfDataType type, Object value);

	protected abstract CmfAttributeTranslator<V> getTranslator();

	protected abstract SessionFactory<S> newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception;

	protected abstract F newContextFactory(S session, CfgTools cfg, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> streamStore, Transformer transformer, Logger output, WarningTracker warningTracker)
		throws Exception;

	protected abstract D newDelegateFactory(S session, CfgTools cfg) throws Exception;

	protected abstract Set<String> getTargetNames();

	public final CmfCrypt getCrypto() {
		return this.crypto;
	}

	public final ExportTarget getReferrent(CmfObject<V> marshaled) {
		if (marshaled == null) { throw new IllegalArgumentException("Must provide a marshaled object to analyze"); }
		CmfProperty<V> referrentType = marshaled.getProperty(TransferEngine.REFERRENT_TYPE);
		CmfProperty<V> referrentId = marshaled.getProperty(TransferEngine.REFERRENT_ID);
		CmfProperty<V> referrentKey = marshaled.getProperty(TransferEngine.REFERRENT_KEY);
		if ((referrentType == null) || (referrentId == null) || (referrentKey == null)) { return null; }
		String type = Tools.toString(referrentType.getValue(), true);
		String id = Tools.toString(referrentId.getValue(), true);
		String key = Tools.toString(referrentKey.getValue(), true);
		if ((type == null) || (id == null)) { return null; }
		return new ExportTarget(CmfType.valueOf(type), id, key);
	}

	public final void setReferrent(CmfObject<V> marshaled, ExportTarget referrent) throws ExportException {
		// Now, add the properties to reference the referrent object
		if (referrent != null) {
			final CmfAttributeTranslator<V> translator = getTranslator();
			try {
				CmfProperty<V> referrentType = new CmfProperty<>(TransferEngine.REFERRENT_TYPE, CmfDataType.STRING,
					false);
				referrentType.setValue(translator.getValue(CmfDataType.STRING, referrent.getType().name()));
				marshaled.setProperty(referrentType);

				CmfProperty<V> referrentId = new CmfProperty<>(TransferEngine.REFERRENT_ID, CmfDataType.STRING, false);
				referrentId.setValue(translator.getValue(CmfDataType.STRING, referrent.getId()));
				marshaled.setProperty(referrentId);

				CmfProperty<V> referrentKey = new CmfProperty<>(TransferEngine.REFERRENT_KEY, CmfDataType.STRING,
					false);
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

	protected final void loadPrincipalMappings(CmfValueMapper mapper, CfgTools cfg) {
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

	protected final Transformer getTransformer(CfgTools cfg) throws Exception {
		String defaultXform = String.format("%s%s", this.cfgNamePrefix, Transformer.getDefaultLocation());
		String xform = cfg.getString(TransferSetting.TRANSFORMATION.getLabel(), defaultXform);

		String defaultMeta = String.format("%s%s", this.cfgNamePrefix, ExternalMetadataLoader.getDefaultLocation());
		String meta = cfg.getString(TransferSetting.EXTERNAL_METADATA.getLabel(), defaultMeta);

		ExternalMetadataLoader emdl = ExternalMetadataLoader.getExternalMetadataLoader(meta, !defaultMeta.equals(meta));
		return Transformer.getTransformer(xform, emdl, !defaultXform.equals(xform));
	}

	protected final ObjectFilter getFilter(CfgTools cfg) throws Exception {
		String defaultFilter = String.format("%s%s", this.cfgNamePrefix, ObjectFilter.getDefaultLocation());
		String xform = cfg.getString(TransferSetting.FILTER.getLabel(), defaultFilter);
		return ObjectFilter.getObjectFilter(xform, !defaultFilter.equals(xform));
	}

	protected void getSupportedSettings(Collection<TransferEngineSetting> settings) {
	}
}