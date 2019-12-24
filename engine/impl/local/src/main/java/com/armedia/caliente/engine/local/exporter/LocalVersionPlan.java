package com.armedia.caliente.engine.local.exporter;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

public final class LocalVersionPlan implements Comparator<String> {

	private final LocalFile pattern;
	private final Comparator<String> tagCompare;
	private final BiPredicate<LocalFile, Path> siblingCheck;
	private final Function<Path, Path> converter;

	public LocalVersionPlan(LocalFile pattern, Comparator<String> tagCompare, Function<Path, Path> converter,
		BiPredicate<LocalFile, Path> siblingCheck) {
		this.pattern = Objects.requireNonNull(pattern, "Must provide a file to pattern the plan on");
		this.tagCompare = Objects.requireNonNull(tagCompare, "Must provide a Comparator to order tags with");
		this.siblingCheck = Objects.requireNonNull(siblingCheck, "Must provide a BiPredicate to check for siblings");
		this.converter = (converter != null ? converter : Function.identity());
	}

	public final LocalFile getPatternFile() {
		return this.pattern;
	}

	@Override
	public final int compare(String a, String b) {
		if (a == b) { return 0; }
		if (a == null) { return -1; }
		if (b == null) { return 1; }
		if (Objects.equals(a, b)) { return 0; }
		// TODO: Handle the empty string here?
		return this.tagCompare.compare(a, b);
	}

	public final Path convert(Path p) {
		return this.converter.apply(p);
	}

	public boolean isSibling(Path candidate) {
		if (candidate == null) { return false; }
		return this.siblingCheck.test(this.pattern, candidate);
	}

}