package com.armedia.caliente.engine.local.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;

import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.commons.utilities.Tools;

public class LocalJdbcVersionFinder implements LocalVersionFinder {

	private final LocalQueryService service;

	public LocalJdbcVersionFinder(LocalQueryService service) {
		this.service = Objects.requireNonNull(service, "Must provide a non-null LocalQueryService instance");
	}

	@Override
	public String getObjectId(LocalRoot root, Path path) throws Exception {
		// TODO: Actually fetch the objectId from the mapping table?
		return LocalCommon.calculateId(LocalCommon.toPortablePath(root.relativize(path).toString()));
	}

	@Override
	public String getHistoryId(LocalRoot root, Path path) throws Exception {
		String objectId = getObjectId(root, path);
		if (Files.isDirectory(root.makeAbsolute(path))) { return objectId; }
		return this.service.getHistoryId(objectId);
	}

	@Override
	public LocalVersionHistory getFullHistory(LocalRoot root, Path path) throws Exception {
		final String historyId = getHistoryId(root, path);
		final List<Pair<String, Path>> versions = this.service.getVersionList(historyId);
		Map<String, Integer> byPath = new HashMap<>();
		Map<String, Integer> byHistoryId = new HashMap<>();
		List<LocalFile> fullHistory = new ArrayList<>(versions.size());
		Path radix = versions.get(versions.size() - 1).getValue();
		radix = root.makeAbsolute(radix);

		int i = 0;
		for (Pair<String, Path> version : versions) {
			final String tag = version.getKey();
			final Path versionPath = root.makeAbsolute(version.getValue());
			byHistoryId.put(tag, i);
			final boolean latest = (i == (versions.size() - 1));
			LocalFile lf = new LocalFile(root, versionPath.toString(),
				new LocalVersionInfo(versionPath, radix, historyId, tag), latest);
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