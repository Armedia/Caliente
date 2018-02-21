package com.armedia.caliente.engine.importer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferContext;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.dynamic.transformer.TransformerException;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectHandler;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;
import com.armedia.commons.utilities.CfgTools;

public abstract class ImportContext<S, V, CF extends ImportContextFactory<S, ?, V, ?, ?, ?>>
	extends TransferContext<S, V, CF> {

	private final ImportContextFactory<S, ?, V, ?, ?, ?> factory;
	private final CmfObjectStore<?, ?> cmfObjectStore;
	private final CmfAttributeTranslator<V> translator;
	private final Transformer transformer;
	private final CmfContentStore<?, ?, ?> streamStore;
	private final int historyPosition;

	public <C extends ImportContext<S, V, CF>, W extends SessionWrapper<S>, E extends ImportEngine<S, W, V, C, ?, ?>, F extends ImportContextFactory<S, W, V, C, E, ?>> ImportContext(
		CF factory, CfgTools settings, String rootId, CmfType rootType, S session, Logger output,
		WarningTracker tracker, Transformer transformer, CmfAttributeTranslator<V> translator,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, int historyPosition) {
		super(factory, settings, rootId, rootType, session, output, tracker);
		this.factory = factory;
		this.translator = translator;
		this.cmfObjectStore = objectStore;
		this.streamStore = streamStore;
		this.transformer = transformer;
		this.historyPosition = historyPosition;
	}

	public final int getHistoryPosition() {
		return this.historyPosition;
	}

	public final CmfValueMapper getAttributeMapper() {
		return this.cmfObjectStore.getAttributeMapper();
	}

	public final int loadObjects(CmfType type, Set<String> ids, final CmfObjectHandler<V> handler)
		throws CmfStorageException {
		return this.cmfObjectStore.loadObjects(type, ids, new CmfObjectHandler<CmfValue>() {

			@Override
			public boolean newTier(int tierNumber) throws CmfStorageException {
				return handler.newTier(tierNumber);
			}

			@Override
			public boolean newHistory(String historyId) throws CmfStorageException {
				return handler.newHistory(historyId);
			}

			@Override
			public boolean handleObject(CmfObject<CmfValue> dataObject) throws CmfStorageException {
				if (ImportContext.this.transformer != null) {
					try {
						dataObject = ImportContext.this.transformer.transform(getAttributeMapper(), dataObject);
					} catch (TransformerException e) {
						throw new CmfStorageException(
							String.format("Failed to transform %s", dataObject.getDescription()), e);
					}
				}
				CmfObject<V> encoded = ImportContext.this.translator.decodeObject(dataObject);
				return handler.handleObject(encoded);
			}

			@Override
			public boolean handleException(Exception e) {
				return handler.handleException(e);
			}

			@Override
			public boolean endHistory(String historyId, boolean ok) throws CmfStorageException {
				return handler.endHistory(historyId, ok);
			}

			@Override
			public boolean endTier(int tierNumber, boolean ok) throws CmfStorageException {
				return handler.endTier(tierNumber, ok);
			}
		});
	}

	public final CmfObject<V> getHeadObject(CmfObject<V> sample) throws CmfStorageException {
		if (sample.isHistoryCurrent()) { return sample; }
		CmfObject<CmfValue> rawObject = this.cmfObjectStore.loadHeadObject(sample.getType(), sample.getHistoryId());
		if (this.transformer != null) {
			try {
				rawObject = this.transformer.transform(getAttributeMapper(), rawObject);
			} catch (TransformerException e) {
				throw new CmfStorageException(String.format("Failed to transform %s", sample.getDescription()), e);
			}
		}
		return this.translator.decodeObject(rawObject);
	}

	public final CmfContentStore<?, ?, ?> getContentStore() {
		return this.streamStore;
	}

	protected final CmfObjectStore<?, ?> getObjectStore() {
		return this.cmfObjectStore;
	}

	public final String getTargetPath(String sourcePath) throws ImportException {
		return this.factory.getTargetPath(sourcePath);
	}

	public final boolean isPathAltering() {
		return this.factory.isPathAltering();
	}

	public final List<CmfContentStream> getContentStreams(CmfObject<V> object) throws ImportException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose content to retrieve"); }
		try {
			return this.cmfObjectStore.getContentStreams(object);
		} catch (CmfStorageException e) {
			throw new ImportException(String.format("Failed to load the content info for %s", object.getDescription()),
				e);
		}
	}

	public Collection<CmfObjectRef> getContainers(CmfObject<V> object) throws ImportException {
		try {
			return this.cmfObjectStore.getContainers(object);
		} catch (CmfStorageException e) {
			throw new ImportException(String.format("Failed to load the containers for %s", object.getDescription()),
				e);
		}
	}

	public Collection<CmfObjectRef> getContainedObjects(CmfObject<V> object) throws ImportException {
		try {
			return this.cmfObjectStore.getContainedObjects(object);
		} catch (CmfStorageException e) {
			throw new ImportException(
				String.format("Failed to load the contained objects for %s", object.getDescription()), e);
		}
	}
}