package com.armedia.caliente.engine.importer;

import java.util.ArrayList;
import java.util.List;

import com.armedia.caliente.engine.ContextFactory;
import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public abstract class ImportContextFactory<S, W extends SessionWrapper<S>, V, C extends ImportContext<S, V, ?>, E extends ImportEngine<S, W, V, C, ?, ?>, FOLDER>
	extends ContextFactory<S, V, C, E> {

	private final List<String> rootPath;
	private final String rootPathStr;
	private final int pathTrunc;

	protected ImportContextFactory(E engine, CfgTools settings, S session) throws Exception {
		super(engine, settings, session);
		String rootPath = settings.getString(ImportSetting.TARGET_LOCATION);
		this.rootPath = Tools.freezeList(FileNameTools.tokenize(rootPath, '/'));
		this.pathTrunc = Math.max(0, settings.getInteger(ImportSetting.TRIM_PREFIX));
		if (this.rootPath.isEmpty()) {
			this.rootPathStr = "/";
		} else {
			this.rootPathStr = FileNameTools.reconstitute(this.rootPath, true, false, '/');
		}
	}

	final void ensureTargetPath(S session) throws ImportException {
		try {
			ensurePath(session, this.rootPathStr);
		} catch (Exception e) {
			throw new ImportException(
				String.format("Failed to ensure the existence of the target path [%s]", this.rootPathStr), e);
		}
	}

	private FOLDER ensurePath(S session, String path) throws Exception {
		if (Tools.equals("/", path)) { return null; }
		FOLDER target = locateFolder(session, path);
		if (target == null) {
			FOLDER parent = ensurePath(session, FileNameTools.dirname(path, '/'));
			target = createFolder(session, parent, FileNameTools.basename(path, '/'));
		}
		return target;
	}

	protected abstract FOLDER locateFolder(S session, String path) throws Exception;

	protected abstract FOLDER createFolder(S session, FOLDER parent, String name) throws Exception;

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
}