package com.armedia.caliente.engine.local.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.exporter.LocalPathVersionFinder.VersionInfo;
import com.armedia.commons.utilities.Tools;

public class LocalJdbcVersionFinder implements LocalVersionFinder {

	private final LocalQueryService service;

	public LocalJdbcVersionFinder(LocalQueryService service) {
		this.service = Objects.requireNonNull(service, "Must provide a non-null LocalQueryService instance");
	}

	@Override
	public String getHistoryId(LocalRoot root, Path path, Function<Path, Path> pathConverter) throws Exception {
		path = Tools.coalesce(pathConverter, LocalVersionFinder.PATH_IDENTITY).apply(path);
		String objectId = LocalCommon.calculateId(LocalCommon.toPortablePath(root.relativize(path).toString()));
		if (Files.isDirectory(root.makeAbsolute(path))) { return objectId; }
		return this.service.getHistoryId(objectId);
	}

	@Override
	public LocalVersionHistory getFullHistory(LocalRoot root, Path path, Function<Path, Path> pathConverter)
		throws Exception {
		final String historyId = getHistoryId(root, path, pathConverter);
		final Path truePath = root.makeAbsolute(path);

		if (Files.isDirectory(truePath)) { return LocalVersionHistory.getSingleHistory(root, path); }

		final List<Pair<String, Path>> versions = this.service.getVersionList(historyId);
		Map<String, Integer> byPath = new HashMap<>();
		Map<String, Integer> byHistoryId = new HashMap<>();
		List<LocalFile> fullHistory = new ArrayList<>(versions.size());
		int i = 0;
		for (Pair<String, Path> v : versions) {
			final String tag = v.getKey();
			Path versionPath = v.getValue();
			byHistoryId.put(tag, i);
			final boolean latest = (i == (versions.size() - 1));
			VersionInfo thisInfo = new VersionInfo(truePath, Paths.get(historyId), tag);
			LocalFile lf = new LocalFile(root, versionPath.toString(), thisInfo, latest);
			byPath.put(lf.getFullPath(), i);
			fullHistory.add(lf);
			i++;
		}
		byPath = Tools.freezeMap(byPath);
		fullHistory = Tools.freezeList(fullHistory);
		byHistoryId = Tools.freezeMap(byHistoryId);
		LocalFile rootVersion = fullHistory.get(0);
		LocalFile currentVersion = fullHistory.get(fullHistory.size() - 1);
		return new LocalVersionHistory(historyId, rootVersion, currentVersion, byHistoryId, byPath, fullHistory);
	}

}