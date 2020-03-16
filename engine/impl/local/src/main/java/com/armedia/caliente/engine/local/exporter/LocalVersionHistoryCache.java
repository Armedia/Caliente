package com.armedia.caliente.engine.local.exporter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.map.LRUMap;

import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.exporter.LocalVersionLayout.VersionInfo;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.ShareableMap;

public class LocalVersionHistoryCache {

	private class HistoryKey {
		private final String historyId;
		private final Path radix;

		private HistoryKey(Path path) {
			VersionInfo info = LocalVersionHistoryCache.this.versionLayout
				.parseVersionInfo(LocalVersionHistoryCache.this.root, path);
			this.historyId = info.getHistoryId();
			this.radix = info.getRadix();
		}

		private HistoryKey(LocalFile file) {
			this.historyId = file.getHistoryId();
			this.radix = Paths.get(file.getHistoryRadix());
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.historyId, this.radix);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			HistoryKey other = HistoryKey.class.cast(obj);
			if (!Objects.equals(this.historyId, other.historyId)) { return false; }
			if (!Objects.equals(this.radix, other.radix)) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return String.format("HistoryKey [historyId=%s, radix=%s]", this.historyId, this.radix);
		}
	}

	private final LocalVersionLayout versionLayout;
	private final LocalRoot root;

	private final Map<HistoryKey, LocalVersionHistory> histories;

	public LocalVersionHistoryCache(LocalRoot root, LocalVersionLayout plan) {
		this(root, plan, 0);
	}

	public LocalVersionHistoryCache(LocalRoot root, LocalVersionLayout plan, int historySize) {
		this.versionLayout = plan;
		this.root = root;
		if (historySize < 0) {
			this.histories = new ConcurrentHashMap<>();
		} else {
			this.histories = new ShareableMap<>(
				new LRUMap<>(Tools.ensureBetween(100000, historySize, Integer.MAX_VALUE)));
		}
	}

	private LocalVersionHistory getVersionHistory(HistoryKey key, String path) throws IOException {
		final Path truePath = this.root.makeAbsolute(Paths.get(path));
		if (Files.isDirectory(truePath) || (this.versionLayout.getVersionNumberScheme() == null)) {
			// History doesn't need to be cached. so we avoid some synchronization overhead here
			return this.versionLayout.calculateSingleHistory(this.root, truePath);
		}

		try {
			return this.histories.computeIfAbsent(key, (k) -> {
				try {
					return this.versionLayout.calculateHistory(this.root, truePath);
				} catch (IOException e) {
					throw new UncheckedIOException(
						String.format("Failed to calculate the history for path [%s]", k, path), e);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	public LocalFile getLocalFile(Path path) throws IOException {
		return getVersionHistory(path).getByPath(path);
	}

	public LocalVersionHistory getVersionHistory(final Path path) throws IOException {
		return getVersionHistory(new HistoryKey(path), path.toString());
	}

	public LocalVersionHistory getVersionHistory(final LocalFile file) throws IOException {
		return getVersionHistory(new HistoryKey(file), file.getFullPath());
	}
}