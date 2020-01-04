package com.armedia.caliente.engine.local.exporter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.map.LRUMap;

import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.exporter.LocalVersionPlan.VersionInfo;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.ShareableMap;

public class LocalVersionHistoryManager {

	private class HistoryKey {
		private final String historyId;
		private final Path radix;

		private HistoryKey(Path path) {
			VersionInfo info = LocalVersionHistoryManager.this.plan
				.parseVersionInfo(LocalVersionHistoryManager.this.root, path);
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

	private final LocalVersionPlan plan;
	private final LocalRoot root;

	private final Map<HistoryKey, LocalVersionHistory> histories = new ShareableMap<>(new LRUMap<>(1000));

	public LocalVersionHistoryManager(LocalRoot root, LocalVersionPlan plan) {
		this.plan = plan;
		this.root = root;
	}

	public LocalVersionHistory getVersionHistory(final Path path) throws IOException {
		try {
			return this.histories.computeIfAbsent(new HistoryKey(path), (k) -> {
				try {
					return this.plan.calculateHistory(this.root,
						LocalFile.getInstance(this.root, path.toString(), this.plan));
				} catch (IOException e) {
					throw new UncheckedIOException(
						String.format("Failed to calculate the history for %s (path = %s)", k, path), e);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	public LocalVersionHistory getVersionHistory(final LocalFile file) throws IOException {
		try {
			return this.histories.computeIfAbsent(new HistoryKey(file), (k) -> {
				try {
					return this.plan.calculateHistory(this.root,
						LocalFile.getInstance(this.root, file.toString(), this.plan));
				} catch (IOException e) {
					throw new UncheckedIOException(
						String.format("Failed to calculate the history for %s (path = %s)", k, file), e);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}
}