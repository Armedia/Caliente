package com.armedia.caliente.cli.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.commons.utilities.Tools;

public class FileRecursionLoopException extends ParserException {
	private static final long serialVersionUID = 1L;

	private final List<String> files;

	public FileRecursionLoopException(Collection<String> files) {
		this.files = Tools.freezeList(new ArrayList<>(files));
	}

	public final List<String> getFiles() {
		return this.files;
	}
}