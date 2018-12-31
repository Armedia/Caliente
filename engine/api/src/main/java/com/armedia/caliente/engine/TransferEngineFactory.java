package com.armedia.caliente.engine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.PluggableServiceLocator;

public abstract class TransferEngineFactory< //
	LISTENER extends TransferListener, //
	RESULT extends Enum<RESULT>, //
	EXCEPTION extends TransferException, //
	SESSION, //
	VALUE, //
	CONTEXT extends TransferContext<SESSION, VALUE, CONTEXT_FACTORY>, //
	CONTEXT_FACTORY extends TransferContextFactory<SESSION, VALUE, CONTEXT, ?>, //
	DELEGATE_FACTORY extends TransferDelegateFactory<SESSION, VALUE, CONTEXT, ?>, //
	ENGINE extends TransferEngine<LISTENER, RESULT, EXCEPTION, SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ?> //
> {
	private static final Map<String, Map<String, Object>> REGISTRY = new HashMap<>();
	private static final Map<String, PluggableServiceLocator<?>> LOCATORS = new HashMap<>();

	private static synchronized <FACTORY extends TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?>> void registerSubclass(
		Class<FACTORY> subclass) {

		final String key = subclass.getCanonicalName();
		Map<String, Object> m = TransferEngineFactory.REGISTRY.get(key);
		if (m != null) { return; }

		m = new HashMap<>();
		PluggableServiceLocator<FACTORY> locator = new PluggableServiceLocator<>(subclass);
		locator.setHideErrors(true);
		for (FACTORY e : locator) {
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
		TransferEngineFactory.REGISTRY.put(key, m);
		TransferEngineFactory.LOCATORS.put(key, locator);
	}

	public static synchronized <ENGINE_FACTORY extends TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?>> ENGINE_FACTORY getEngineFactory(
		Class<ENGINE_FACTORY> subclass, String targetName) {
		if (subclass == null) { throw new IllegalArgumentException("Must provide a valid engine subclass"); }
		if (StringUtils.isEmpty(
			targetName)) { throw new IllegalArgumentException("Must provide a non-empty, non-null target name"); }
		TransferEngineFactory.registerSubclass(subclass);
		Map<String, Object> m = TransferEngineFactory.REGISTRY.get(subclass.getCanonicalName());
		if (m == null) { return null; }
		return subclass.cast(m.get(targetName));
	}

	public static synchronized <ENGINE_FACTORY extends TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?>> Set<String> getAvailableEngineFactories(
		Class<ENGINE_FACTORY> subclass) {
		if (subclass == null) { throw new IllegalArgumentException("Must provide a valid engine subclass"); }
		TransferEngineFactory.registerSubclass(subclass);
		return new TreeSet<>(TransferEngineFactory.REGISTRY.keySet());
	}

	protected final boolean supportsDuplicateFileNames;
	protected final CmfCrypt crypto;

	public TransferEngineFactory(boolean supportsDuplicateFileNames, CmfCrypt crypto) {
		this.supportsDuplicateFileNames = supportsDuplicateFileNames;
		this.crypto = crypto;
	}

	public abstract ENGINE newInstance(final Logger output, final WarningTracker warningTracker, final File baseData,
		final CmfObjectStore<?, ?> objectStore, final CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings)
		throws EXCEPTION;

	protected abstract Set<String> getTargetNames();

	public final boolean isSupportsDuplicateFileNames() {
		return this.supportsDuplicateFileNames;
	}

	public final CmfCrypt getCrypto() {
		return this.crypto;
	}

}