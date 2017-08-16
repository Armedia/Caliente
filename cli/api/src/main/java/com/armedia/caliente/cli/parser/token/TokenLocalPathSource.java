package com.armedia.caliente.cli.parser.token;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TokenLocalPathSource extends TokenStreamSource {

	private static Path resolveCanonicalPath(Path p) throws IOException {
		return p.toFile().getCanonicalFile().toPath();
	}

	private final Path sourcePath;

	public TokenLocalPathSource(String sourcePath) throws IOException {
		if (sourcePath == null) { throw new IllegalArgumentException("Must provide a non-null path"); }
		this.sourcePath = TokenLocalPathSource.resolveCanonicalPath(Paths.get(sourcePath));
	}

	public TokenLocalPathSource(Path sourcePath) throws IOException {
		if (sourcePath == null) { throw new IllegalArgumentException("Must provide a non-null path"); }
		this.sourcePath = TokenLocalPathSource.resolveCanonicalPath(sourcePath);
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
}