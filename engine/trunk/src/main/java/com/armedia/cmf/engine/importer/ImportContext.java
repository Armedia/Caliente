package com.armedia.cmf.engine.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.cmf.engine.ContextFactory;
import com.armedia.cmf.engine.TransferContext;
import com.armedia.cmf.engine.TransferEngine;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredAttributeMapper;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectHandler;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public abstract class ImportContext<S, T, V> extends TransferContext<S, T, V> {

	private final ObjectStore<?, ?> objectStore;
	private final ObjectStorageTranslator<T, V> translator;
	private final ContentStore streamStore;
	private final List<String> rootPath;
	private final int pathTrunc;

	public <C extends TransferContext<S, T, V>, E extends TransferEngine<S, T, V, C, ?>, F extends ContextFactory<S, T, V, C, E>> ImportContext(
		F factory, CfgTools settings, String rootId, StoredObjectType rootType, S session, Logger output,
		ObjectStorageTranslator<T, V> translator, ObjectStore<?, ?> objectStore, ContentStore streamStore) {
		super(factory, settings, rootId, rootType, session, output);
		this.translator = translator;
		this.objectStore = objectStore;
		this.streamStore = streamStore;
		String rootPath = settings.getString(ImportSetting.TARGET_LOCATION);
		this.rootPath = Tools.freezeList(FileNameTools.tokenize(rootPath, '/'));
		this.pathTrunc = Math.max(0, settings.getInteger(ImportSetting.TRIM_PREFIX));
	}

	public final StoredAttributeMapper getAttributeMapper() {
		return this.objectStore.getAttributeMapper();
	}

	public final int loadObjects(StoredObjectType type, Set<String> ids, StoredObjectHandler<V> handler)
		throws StorageException, StoredValueDecoderException {
		if (isSurrogateType(getRootObjectType(), type)) {
			return this.objectStore.loadObjects(this.translator, type, ids, handler);
		} else {
			return 0;
		}
	}

	public final Handle getContentHandle(StoredObject<V> object) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to inspect for a content URI"); }
		String qualifier = getContentQualifier(object);
		if (qualifier == null) { return null; }
		return this.streamStore.getHandle(object, qualifier);
	}

	protected boolean isSurrogateType(StoredObjectType rootType, StoredObjectType target) {
		return false;
	}

	public final void ensureTargetPath() throws ImportException {
		if (this.rootPath.isEmpty()) { return; }
		List<String> l = new ArrayList<String>(this.rootPath.size());
		for (String s : this.rootPath) {
			if (StringUtils.isBlank(s)) {
				// Should never happen, but be safe...
				continue;
			}
			l.add(s);
			final String path = FileNameTools.reconstitute(l, true, false, '/');
			try {
				locateOrCreatePath(path);
			} catch (Exception e) {
				throw new ImportException(String.format("Exception raised while locating or creating the path [%s]",
					path), e);
			}
		}
	}

	protected abstract T locateOrCreatePath(String path) throws Exception;

	public final String getTargetPath(String sourcePath) throws ImportException {
		List<String> l = FileNameTools.tokenize(sourcePath, '/');
		if (l.size() <= this.pathTrunc) { throw new ImportException(
			String
				.format(
					"The path truncation setting (%d) is higher than the number of path components in [%s] (%d) - can't continue",
					this.pathTrunc, sourcePath, l.size())); }
		for (int i = 0; i < this.pathTrunc; i++) {
			l.remove(0);
		}
		List<String> finalPath = new ArrayList<String>(this.rootPath.size() + l.size());
		finalPath.addAll(this.rootPath);
		finalPath.addAll(l);
		return FileNameTools.reconstitute(finalPath, true, false, '/');
	}
}