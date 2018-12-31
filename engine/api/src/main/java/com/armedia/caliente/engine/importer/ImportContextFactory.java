package com.armedia.caliente.engine.importer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferContextFactory;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public abstract class ImportContextFactory< //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ImportContext<SESSION, VALUE, ?>, //
	ENGINE extends ImportEngine<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?, ?>, //
	FOLDER //
> extends TransferContextFactory<SESSION, VALUE, CONTEXT, ENGINE> {

	private final List<String> rootPath;
	private final String rootPathStr;
	private final int pathTrunc;

	protected ImportContextFactory(ENGINE engine, CfgTools settings, SESSION session, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> contentStore, Transformer transformer, Logger output, WarningTracker tracker)
		throws Exception {
		super(engine, settings, session, objectStore, contentStore, transformer, output, tracker);
		String rootPath = settings.getString(ImportSetting.TARGET_LOCATION);
		this.rootPath = Tools.freezeList(FileNameTools.tokenize(rootPath, '/'));
		this.pathTrunc = Math.max(0, settings.getInteger(ImportSetting.TRIM_PREFIX));
		if (this.rootPath.isEmpty()) {
			this.rootPathStr = "/";
		} else {
			this.rootPathStr = FileNameTools.reconstitute(this.rootPath, true, false, '/');
		}
	}

	final void ensureTargetPath(SESSION session) throws ImportException {
		try {
			ensurePath(session, this.rootPathStr);
		} catch (Exception e) {
			throw new ImportException(
				String.format("Failed to ensure the existence of the target path [%s]", this.rootPathStr), e);
		}
	}

	private FOLDER ensurePath(SESSION session, String path) throws Exception {
		if (Tools.equals("/", path)) { return null; }
		FOLDER target = locateFolder(session, path);
		if (target == null) {
			FOLDER parent = ensurePath(session, FileNameTools.dirname(path, '/'));
			target = createFolder(session, parent, FileNameTools.basename(path, '/'));
		}
		return target;
	}

	protected abstract FOLDER locateFolder(SESSION session, String path) throws Exception;

	protected abstract FOLDER createFolder(SESSION session, FOLDER parent, String name) throws Exception;

	public final String getTargetPath(String sourcePath) throws ImportException {
		if (sourcePath == null) { throw new IllegalArgumentException("Must provide a path to transform"); }
		if (!sourcePath.startsWith(
			"/")) { throw new IllegalArgumentException(String.format("The path [%s] must be absolute", sourcePath)); }
		List<String> l = FileNameTools.tokenize(sourcePath, '/');
		final int delta = (this.rootPath.size() > 0 ? 1 : 0);
		if (l.size() < (this.pathTrunc - delta)) { throw new ImportException(String.format(
			"The path truncation setting (%d) is higher than the number of path components in [%s] (%d) - can't continue",
			this.pathTrunc, sourcePath, l.size())); }
		for (int i = 0; i < this.pathTrunc; i++) {
			l.remove(0);
		}
		List<String> finalPath = new ArrayList<>(this.rootPath.size() + l.size());
		finalPath.addAll(this.rootPath);
		finalPath.addAll(l);
		return FileNameTools.reconstitute(finalPath, true, false, '/');
	}

	public final boolean isPathAltering() {
		return (this.pathTrunc != 0) || !this.rootPath.isEmpty();
	}

	@Override
	protected void calculateExcludes(CmfObjectStore<?, ?> objectStore, Set<CmfType> excludes)
		throws CmfStorageException {
		Map<CmfType, Long> summary = objectStore.getStoredObjectTypes();
		if ((summary != null) && !summary.isEmpty()) {
			for (CmfType t : CmfType.values()) {
				Long count = summary.get(t);
				// If the object type isn't even included (null or 0-count), then
				// we add the object to the excludes list to avoid problems.
				if ((count == null) || (count.longValue() < 1)) {
					excludes.add(t);
				}
			}
		} else {
			excludes = EnumSet.allOf(CmfType.class);
		}
	}

	@Override
	protected String getContextLabel() {
		return "import";
	}
}