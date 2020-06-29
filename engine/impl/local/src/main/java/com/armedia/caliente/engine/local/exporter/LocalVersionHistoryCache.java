package com.armedia.caliente.engine.local.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.map.LRUMap;

import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.ShareableMap;

public class LocalVersionHistoryCache {

	private final LocalVersionFinder versionFinder;
	private final LocalRoot root;

	private final Map<String, LocalVersionHistory> histories;

	public LocalVersionHistoryCache(LocalRoot root, LocalVersionFinder versionFinder) {
		this(root, versionFinder, 0);
	}

	public LocalVersionHistoryCache(LocalRoot root, LocalVersionFinder versionFinder, int historySize) {
		this.versionFinder = versionFinder;
		this.root = root;
		if (historySize <= 0) {
			this.histories = new ConcurrentHashMap<>();
		} else {
			this.histories = new ShareableMap<>(
				new LRUMap<>(Tools.ensureBetween(100000, historySize, Integer.MAX_VALUE)));
		}
	}

	private LocalVersionHistory getVersionHistory(String path) throws Exception {
		final Path truePath = this.root.makeAbsolute(Paths.get(path));
		// Slight optimization...
		if (Files.isDirectory(truePath) || (this.versionFinder == null)) {
			return LocalVersionHistory.getSingleHistory(this.root, truePath);
		}
		final String key = this.versionFinder.getHistoryId(this.root, truePath);
		return this.histories.computeIfAbsent(key, (k) -> {
			try {
				return this.versionFinder.getFullHistory(this.root, truePath);
			} catch (Exception e) {
				throw new RuntimeException(String.format("Failed to calculate the history for path [%s]", k, path), e);
			}
		});
	}

	public LocalFile getLocalFile(Path path) throws Exception {
		return getVersionHistory(path).getByPath(path);
	}

	public LocalVersionHistory getVersionHistory(final Path path) throws Exception {
		return getVersionHistory(path.toString());
	}

	public LocalVersionHistory getVersionHistory(final LocalFile file) throws Exception {
		return getVersionHistory(file.getFullPath());
	}
}