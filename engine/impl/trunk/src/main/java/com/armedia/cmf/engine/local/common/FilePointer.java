package com.armedia.cmf.engine.local.common;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import com.armedia.commons.utilities.Tools;

public class FilePointer implements Comparable<FilePointer> {
	private static Pattern WIN_PATTERN = Pattern.compile(
		"# Match a valid Windows filename (unspecified file system).          \n"
			+ "^                                # Anchor to start of string.        \n"
			+ "(?!                              # Assert filename is not: CON, PRN, \n"
			+ "  (?:                            # AUX, NUL, COM1, COM2, COM3, COM4, \n"
			+ "    CON|PRN|AUX|CLOCK\\$|NUL|    # COM5, COM6, COM7, COM8, COM9,     \n"
			+ "    COM[1-9]|LPT[1-9]            # LPT1, LPT2, LPT3, LPT4, LPT5,     \n"
			+ "  )                              # LPT6, LPT7, LPT8, and LPT9...     \n"
			+ "  (?:\\.[^.]*)?                  # followed by optional extension    \n"
			+ "  $                              # and end of string                 \n"
			+ ")                                # End negative lookahead assertion. \n"
			+ "[^<>:\"/\\\\|?*\\x00-\\x1F]*     # Zero or more valid filename chars.\n"
			+ "[^<>:\"/\\\\|?*\\x00-\\x1F\\ .]  # Last char is not a space or dot.  \n"
			+ "$                                # Anchor to end of string.            ", //
		Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.COMMENTS);

	private final FilePointer parent;
	private final String name;
	private final String path;
	private final File file;

	public FilePointer(File file) throws IOException {
		this(null, file);
	}

	public FilePointer(FilePointer parent, File file) throws IOException {
		if (file == null) { throw new IllegalArgumentException("Must provide a file to point to"); }
		this.parent = parent;
		if (FilePointer.WIN_PATTERN.matcher(file.getName()).matches()) {
			this.name = file.getName();
		} else {
			// URLEncode the name, and keep it as such
			try {
				this.name = URLEncoder.encode(file.getName(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new IOException("UTF-8 encoding not supported in this JVM", e);
			}
		}
		this.file = file.getCanonicalFile();
		if (parent != null) {
			this.path = String.format("%s/%s", parent.path, this.name);
		} else {
			this.path = this.name;
		}
	}

	public FilePointer getParent() {
		return this.parent;
	}

	public String getName() {
		return this.name;
	}

	public String getPath() {
		return this.path;
	}

	public File getFile() {
		return this.file;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.file);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		FilePointer other = FilePointer.class.cast(obj);
		if (!Tools.equals(this.file, other.file)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("FilePointer [path={%s}, file={%s}]", this.name, this.path, this.file.getAbsolutePath());
	}

	@Override
	public int compareTo(FilePointer o) {
		return this.file.compareTo(o.file);
	}
}