package com.armedia.caliente.cli.token;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalPathTokenSource extends StreamTokenSource {

	private static Path resolveCanonicalPath(Path p) throws IOException {
		return p.toFile().getCanonicalFile().toPath();
	}

	private final Path sourcePath;

	public LocalPathTokenSource(String sourcePath) throws IOException {
		if (sourcePath == null) { throw new IllegalArgumentException("Must provide a non-null path"); }
		this.sourcePath = LocalPathTokenSource.resolveCanonicalPath(Paths.get(sourcePath));
	}

	public LocalPathTokenSource(Path sourcePath) throws IOException {
		if (sourcePath == null) { throw new IllegalArgumentException("Must provide a non-null path"); }
		this.sourcePath = LocalPathTokenSource.resolveCanonicalPath(sourcePath);
	}

	public Path getSourcePath() {
		return this.sourcePath;
	}

	@Override
	public String getKey() {
		return this.sourcePath.toString();
	}

	@Override
	protected InputStream openStream() throws IOException {
		return new FileInputStream(this.sourcePath.toFile());
	}

	@Override
	public String toString() {
		return String.format("LocalPathTokenSource [path=%s]", this.sourcePath);
	}
}