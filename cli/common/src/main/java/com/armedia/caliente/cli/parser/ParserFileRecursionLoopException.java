package com.armedia.caliente.cli.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.commons.utilities.Tools;

public class ParserFileRecursionLoopException extends ParserException {
	private static final long serialVersionUID = 1L;

	private final File loopedFile;
	private final List<String> files;

	public ParserFileRecursionLoopException(File loopedFile, Collection<String> files) {
		this.loopedFile = loopedFile;
		this.files = Tools.freezeList(new ArrayList<>(files));
	}

	public final File getLoopedFile() {
		return this.loopedFile;
	}

	public final List<String> getFiles() {
		return this.files;
	}
}