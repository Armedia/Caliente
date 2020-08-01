package com.armedia.caliente.engine.local.common;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public enum LocalCaseFolding {

	//
	NONE(null), //
	LOWER(StringUtils::lowerCase),
	UPPER(StringUtils::upperCase),
	//
	;

	private final Function<String, String> converter;

	private LocalCaseFolding(Function<String, String> converter) {
		this.converter = converter;
	}

	public final String fold(String s) {
		if (ObjectUtils.anyNull(this.converter, s)) { return s; }
		return this.converter.apply(s);
	}

	public final File apply(File file) {
		if (ObjectUtils.anyNull(this.converter, file)) { return file; }
		return apply(file.toPath()).toFile();
	}

	public final Path apply(Path path) {
		if (ObjectUtils.anyNull(this.converter, path)) { return path; }
		Path folded = path.getRoot();
		if (folded != null) {
			folded = Paths.get(this.converter.apply(folded.toString()));
		}
		for (Path p : path) {
			String s = this.converter.apply(p.toString());
			if (folded != null) {
				folded = folded.resolve(s);
			} else {
				folded = Paths.get(s);
			}
		}
		return folded;
	}
}