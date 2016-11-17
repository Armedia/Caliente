package com.armedia.caliente.cli.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.armedia.commons.utilities.Tools;

public class TokenLocalFileSource extends TokenStreamSource {

	private final File sourceFile;

	public TokenLocalFileSource(String sourceFile) {
		if (sourceFile == null) { throw new IllegalArgumentException("Must provide a non-null file object"); }
		this.sourceFile = Tools.canonicalize(new File(sourceFile));
	}

	public TokenLocalFileSource(File sourceFile) {
		if (sourceFile == null) { throw new IllegalArgumentException("Must provide a non-null file object"); }
		this.sourceFile = Tools.canonicalize(sourceFile);
	}

	@Override
	public String getKey() {
		return this.sourceFile.getAbsolutePath();
	}

	@Override
	protected InputStream openStream() throws IOException {
		return new FileInputStream(this.sourceFile);
	}
}