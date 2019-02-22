package com.armedia.caliente.engine.alfresco.bi.importer;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.alfresco.bi.AlfRoot;
import com.armedia.caliente.engine.alfresco.bi.AlfSessionWrapper;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContextFactory;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.function.LazySupplier;

public class AlfImportContextFactory
	extends ImportContextFactory<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportEngine, File> {

	private final LazySupplier<Map<CmfObject.Archetype, Map<String, String>>> renameMap = new LazySupplier<>(() -> {
		try {
			return getObjectStore().getRenameMappings();
		} catch (final CmfStorageException e) {
			this.log.error("Failed to load the renamer map from the object store", e);
			throw new ImportException(e);
		}
	}, Collections.emptyMap());

	protected AlfImportContextFactory(AlfImportEngine engine, CfgTools settings, AlfRoot root,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, Transformer transformer, Logger output,
		WarningTracker tracker) throws Exception {
		super(engine, settings, root, objectStore, contentStore, transformer, output, tracker);
	}

	@Override
	protected File locateFolder(AlfRoot session, String path) throws Exception {
		File f = new File(session.getFile(), path).getCanonicalFile();
		if (f.exists() && f.isDirectory()) { return f; }
		return null;
	}

	@Override
	protected File createFolder(AlfRoot session, File parent, String name) throws Exception {
		File f = new File(parent, name);
		f.mkdirs();
		if (!f.exists()) { throw new Exception(String.format("Could not create the directory at [%s]", name)); }
		if (!f.isDirectory()) { throw new Exception(String.format("A non-directory already exists at [%s]", name)); }
		return f;
	}

	@Override
	protected AlfImportContext constructContext(String rootId, CmfObject.Archetype rootType, AlfRoot session,
		int batchPosition) {
		final CmfObjectStore<?> store = getObjectStore();
		try {
			this.renameMap.getChecked();
		} catch (Exception e) {
			// Ignore it - already logged
		}
		return new AlfImportContext(this, getSettings(), rootId, rootType, session, getOutput(), getWarningTracker(),
			getTransformer(), getEngine().getTranslator(), store, getContentStore(), batchPosition);
	}

	@Override
	protected String calculateProductName(AlfRoot session) throws Exception {
		return "AlfrescoBulkImport";
	}

	@Override
	protected String calculateProductVersion(AlfRoot session) throws Exception {
		return "1.0";
	}

	private Map<CmfObject.Archetype, Map<String, String>> getRenameMap() throws ImportException {
		try {
			return this.renameMap.getChecked();
		} catch (Exception e) {
			throw new ImportException("Failed to load the renamer map from the object store", e);
		}
	}

	public final String getAlternateName(CmfObject.Archetype type, String id) throws ImportException {
		Map<CmfObject.Archetype, Map<String, String>> renameMap = getRenameMap();
		if ((renameMap == null) || renameMap.isEmpty()) { return null; }
		Map<String, String> m = renameMap.get(type);
		if ((m == null) || m.isEmpty()) { return null; }
		return m.get(id);
	}

	public final Map<CmfObjectRef, String> getObjectNames(Collection<CmfObjectRef> refs, boolean current)
		throws ImportException {
		try {
			return getObjectStore().getObjectNames(refs, current);
		} catch (CmfStorageException e) {
			throw new ImportException(String.format("Failed to resolve the object names for IDs %s", refs), e);
		}
	}
}