package com.armedia.caliente.engine.local.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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
	public String getHistoryId(LocalRoot root, Path path, Function<Path, Path> pathConverter) throws Exception {
		path = Tools.coalesce(pathConverter, LocalVersionFinder.PATH_IDENTITY).apply(path);
		String objectId = LocalCommon.calculateId(LocalCommon.toPortablePath(root.relativize(path).toString()));
		if (Files.isDirectory(root.makeAbsolute(path))) { return objectId; }
		return this.service.getHistoryId(objectId);
	}

	@Override
	public LocalVersionHistory getFullHistory(LocalRoot root, Path path, Function<Path, Path> pathConverter)
		throws Exception {
		String historyId = getHistoryId(root, path, pathConverter);
		List<Pair<String, Path>> versions = this.service.getVersionList(historyId);
		// TODO: Convert that into a LocalVersionHistory
		return null;
	}

}