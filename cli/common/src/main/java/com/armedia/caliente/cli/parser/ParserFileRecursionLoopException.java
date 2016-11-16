package com.armedia.caliente.cli.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.commons.utilities.Tools;

public class ParserFileRecursionLoopException extends ParserFileException {
	private static final long serialVersionUID = 1L;

	private final List<String> files;

	public ParserFileRecursionLoopException(File loopedFile, Collection<String> files) {
		super(loopedFile);
		this.files = Tools.freezeList(new ArrayList<>(files));
	}

	public final List<String> getFiles() {
		return this.files;
	}
}