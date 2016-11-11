package com.armedia.caliente.engine.xml.common;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public class XmlFile {

	private static final String ENCODING = "UTF-8";

	private static String makeSafe(String s) throws IOException {
		return URLEncoder.encode(s, XmlFile.ENCODING);
	}

	private static String makeUnsafe(String s) throws IOException {
		return URLDecoder.decode(s, XmlFile.ENCODING);
	}

	public static String decodeSafePath(String safePath) throws IOException {
		List<String> r = new ArrayList<>();
		for (String s : FileNameTools.tokenize(safePath, '/')) {
			r.add(XmlFile.makeUnsafe(s));
		}
		return XmlRoot.normalize(FileNameTools.reconstitute(r, false, false));
	}

	public static XmlFile newFromSafePath(XmlRoot root, String safePath) throws IOException {
		return new XmlFile(root, XmlFile.decodeSafePath(safePath));
	}

	private final XmlRoot root;
	private final File absoluteFile;
	private final File relativeFile;
	private final String safePath;
	private final String path;
	private final String name;
	private final int pathCount;

	public XmlFile(XmlRoot root, String path) throws IOException {
		this.root = root;
		File f = root.relativize(new File(path));
		this.relativeFile = f;
		this.absoluteFile = root.makeAbsolute(f);

		List<String> r = new ArrayList<>();
		for (String s : FileNameTools.tokenize(this.relativeFile.getPath())) {
			r.add(XmlFile.makeSafe(s));
		}
		this.safePath = FileNameTools.reconstitute(r, false, false, '/');
		this.pathCount = r.size();
		this.path = f.getParent();
		this.name = f.getName();

	}

	public String getPathHash() {
		return String.format("%08x", this.relativeFile.hashCode());
	}

	public String getPath() {
		return this.path;
	}

	public String getName() {
		return this.name;
	}

	public int getPathCount() {
		return this.pathCount;
	}

	public String getPortablePath() {
		String path = getPath();
		if (path == null) { return null; }
		return FileNameTools.reconstitute(FileNameTools.tokenize(path), true, false, '/');
	}

	/**
	 * <p>
	 * Returns a "universally-safe" path for the absoluteFile which escapes all
	 * potentially-dangerous characters with their URL-safe alternatives (i.e. ' ' -&gt; '+', %XX
	 * encoding, etc.). All path components are separated by forward slashes ('/'). Thus, the
	 * algorithm to obtain the original filename is to tokenize on forward slashes, URL-decode each
	 * component, and re-concatenate using the {@code File#separator} or {@code File#separatorChar}.
	 * </p>
	 *
	 * @return str
	 */
	public String getSafePath() {
		return this.safePath;
	}

	public File getRelative() {
		return this.relativeFile;
	}

	public File getAbsolute() {
		return this.absoluteFile;
	}

	public XmlRoot getRootPath() {
		return this.root;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.root, this.absoluteFile);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		XmlFile other = XmlFile.class.cast(obj);
		if (!Tools.equals(this.root, other.root)) { return false; }
		if (!Tools.equals(this.absoluteFile, other.absoluteFile)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format(
			"XmlFile [root=%s, absoluteFile=%s, relativeFile=%s, safePath=%s, path=%s, name=%s, pathCount=%s]",
			this.root, this.absoluteFile, this.relativeFile, this.safePath, this.path, this.name, this.pathCount);
	}
}
