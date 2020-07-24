package com.armedia.caliente.engine.local.exporter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfContentStream;

public interface LocalPathStore<PARAM> {

	public Stream<ExportTarget> getExportTargets(Collection<PARAM> params) throws Exception;

	public default Stream<ExportTarget> getExportTargets(@SuppressWarnings("unchecked") PARAM... params)
		throws Exception {
		if ((params == null) || (params.length == 0)) { return Stream.empty(); }
		Collection<PARAM> c = Arrays.asList(params);
		Predicate<PARAM> p = LocalPathStore.this::isValid;
		c.removeIf(p.negate());
		return getExportTargets(c);
	}

	public Stream<ExportTarget> queryExportTargets(String query) throws Exception;

	public boolean isValid(PARAM param);

	public String getObjectId(PARAM param) throws Exception;

	public LocalFile getParent(PARAM param) throws Exception;

	public LocalVersionInfo getVersionInfo(PARAM param) throws Exception;

	public NavigableMap<String, LocalVersionInfo> getHistory(PARAM param) throws Exception;

	public NavigableMap<String, LocalVersionInfo> getHistory(LocalVersionInfo version) throws Exception;

	public Collection<Pair<CmfContentStream, Path>> getStreams(PARAM param) throws Exception;

}