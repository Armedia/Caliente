package com.armedia.caliente.engine.local.exporter;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.local.common.LocalCommon;

final class LocalVersionInfo {
	private final Path path;
	private final Path radix;
	private final String historyId;
	private final String tag;

	LocalVersionInfo(Path path, Path radix, String tag) {
		this(path, radix, null, tag);
	}

	LocalVersionInfo(Path path, Path radix, String historyId, String tag) {
		this.path = path;
		this.radix = radix;
		if (!StringUtils.isBlank(historyId)) {
			this.historyId = historyId;
		} else {
			this.historyId = LocalCommon.calculateId(LocalCommon.toPortablePath(radix.toString()));
		}
		this.tag = (StringUtils.isBlank(tag) ? StringUtils.EMPTY : tag);
	}

	public Path getPath() {
		return this.path;
	}

	public Path getRadix() {
		return this.radix;
	}

	public String getHistoryId() {
		return this.historyId;
	}

	public String getTag() {
		return this.tag;
	}

	@Override
	public String toString() {
		return String.format("LocalVersionInfo [path=%s, radix=%s, historyId=%s, tag=%s]", this.path, this.radix,
			this.historyId, this.tag);
	}
}