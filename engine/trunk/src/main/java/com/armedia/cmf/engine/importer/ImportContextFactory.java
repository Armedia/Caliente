package com.armedia.cmf.engine.importer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.ContextFactory;
import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public abstract class ImportContextFactory<S, W extends SessionWrapper<S>, T, V, C extends ImportContext<S, T, V>, E extends ImportEngine<S, W, T, V, C>>
	extends ContextFactory<S, T, V, C, E> {

	private final List<String> rootPath;
	private final int pathTrunc;

	protected ImportContextFactory(E engine, CfgTools settings) {
		super(engine, settings);
		String rootPath = settings.getString(ImportSetting.TARGET_LOCATION);
		this.rootPath = Tools.freezeList(FileNameTools.tokenize(rootPath, '/'));
		this.pathTrunc = Math.max(0, settings.getInteger(ImportSetting.TRIM_PREFIX));
	}

	public final void ensureTargetPath(S session) throws ImportException {
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
				locateOrCreatePath(session, path);
			} catch (Exception e) {
				throw new ImportException(String.format("Exception raised while locating or creating the path [%s]",
					path), e);
			}
		}
	}

	protected abstract T locateOrCreatePath(S session, String path) throws Exception;

	public final String getTargetPath(String sourcePath) throws ImportException {
		if (sourcePath == null) { throw new IllegalArgumentException("Must provide a path to transform"); }
		if (!sourcePath.startsWith("/")) { throw new IllegalArgumentException(String.format(
			"The path [%s] must be absolute", sourcePath)); }
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