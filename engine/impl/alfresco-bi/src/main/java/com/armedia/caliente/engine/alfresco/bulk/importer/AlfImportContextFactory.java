package com.armedia.caliente.engine.alfresco.bulk.importer;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;

import com.armedia.caliente.engine.alfresco.bulk.common.AlfRoot;
import com.armedia.caliente.engine.alfresco.bulk.common.AlfSessionWrapper;
import com.armedia.caliente.engine.importer.ImportContextFactory;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class AlfImportContextFactory
	extends ImportContextFactory<AlfRoot, AlfSessionWrapper, CmfValue, AlfImportContext, AlfImportEngine, File> {

	private volatile Map<CmfType, Map<String, String>> renameMap = null;

	protected AlfImportContextFactory(AlfImportEngine engine, CfgTools settings, AlfRoot root,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CmfTypeMapper typeMapper,
		Logger output) throws Exception {
		super(engine, settings, root, objectStore, contentStore, typeMapper, output);
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
	protected AlfImportContext constructContext(String rootId, CmfType rootType, AlfRoot session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CmfTypeMapper typeMapper,
		int batchPosition) {
		if (this.renameMap == null) {
			synchronized (this) {
				if (this.renameMap == null) {
					Map<CmfType, Map<String, String>> m = null;
					try {
						m = objectStore.getRenameMappings();
					} catch (CmfStorageException e) {
						m = Collections.emptyMap();
						this.log.error("Failed to load the renamer map from the object store", e);
					} finally {
						this.renameMap = m;
					}
				}
			}
		}
		return new AlfImportContext(this, getSettings(), rootId, rootType, session, output, typeMapper,
			getEngine().getTranslator(), objectStore, contentStore, batchPosition);
	}

	@Override
	protected String calculateProductName(AlfRoot session) throws Exception {
		return "AlfrescoBulkImport";
	}

	@Override
	protected String calculateProductVersion(AlfRoot session) throws Exception {
		return "1.0";
	}

	private Map<CmfType, Map<String, String>> getRenameMap() {
		if (this.renameMap == null) {
			synchronized (this) {
				if (this.renameMap == null) {
					Map<CmfType, Map<String, String>> m = null;
					final CmfObjectStore<?, ?> objectStore = getObjectStore();
					try {
						m = objectStore.getRenameMappings();
					} catch (CmfStorageException e) {
						m = Collections.emptyMap();
						this.log.error("Failed to load the renamer map from the object store", e);
					} finally {
						this.renameMap = m;
					}
				}
			}
		}
		return this.renameMap;
	}

	public final String getAlternateName(CmfType type, String id) {
		Map<CmfType, Map<String, String>> renameMap = getRenameMap();
		if ((renameMap == null) || renameMap.isEmpty()) { return null; }
		Map<String, String> m = renameMap.get(type);
		if ((m == null) || m.isEmpty()) { return null; }
		return m.get(id);
	}
}