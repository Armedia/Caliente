package com.armedia.caliente.engine.alfresco.bi;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import com.armedia.commons.utilities.Tools;

public final class AlfRoot implements Comparable<AlfRoot> {

	private final String path;
	private final File file;

	static String normalize(String path) throws IOException {
		String p2 = FilenameUtils.normalize(path);
		if (p2 == null) { throw new IOException(String.format("The path [%s] contains too many '..' elements", path)); }
		return p2;
	}

	public AlfRoot(File path) throws IOException {
		this(path.getCanonicalPath());
	}

	public AlfRoot(String path) throws IOException {
		this.file = new File(AlfRoot.normalize(path)).getCanonicalFile();
		this.path = this.file.getPath();
	}

	public String getPath() {
		return this.path;
	}

	public File getFile() {
		return this.file;
	}

	public String relativize(String path) throws IOException {
		File f = new File(AlfRoot.normalize(path));
		if (!f.isAbsolute()) {
			f = new File(this.file, path);
		}
		String str = f.getPath().substring(this.path.length());
		if (str.startsWith(File.separator)) { return str.substring(1); }
		throw new IOException(String.format("The path [%s] is not a child of [%s]", path, this.path));
	}

	public File relativize(File file) throws IOException {
		return new File(relativize(file.getPath()));
	}

	public File makeAbsolute(File file) throws IOException {
		return makeAbsolute(file.getPath());
	}

	public File makeAbsolute(String path) throws IOException {
		return new File(this.file, AlfRoot.normalize(path)).getAbsoluteFile();
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.file);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		AlfRoot other = AlfRoot.class.cast(obj);
		if (!Tools.equals(this.file, other.file)) { return false; }
		return true;
	}

	@Override
	public int compareTo(AlfRoot o) {
		if (o == null) { return 1; }
		return this.file.compareTo(o.file);
	}

	@Override
	public String toString() {
		return String.format("AlfRoot [%s]", this.file);
	}
}