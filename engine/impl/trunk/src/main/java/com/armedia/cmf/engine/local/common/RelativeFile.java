package com.armedia.cmf.engine.local.common;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public class RelativeFile {

	private static final String ENCODING = "UTF-8";

	private static String makeSafe(String s) throws IOException {
		return URLEncoder.encode(s, RelativeFile.ENCODING);
	}

	private static String makeUnsafe(String s) throws IOException {
		return URLDecoder.decode(s, RelativeFile.ENCODING);
	}

	public static String decodeSafePath(String safePath) throws IOException {
		List<String> r = new ArrayList<String>();
		for (String s : FileNameTools.tokenize(safePath, '/')) {
			r.add(RelativeFile.makeUnsafe(s));
		}
		return RootPath.normalize(FileNameTools.reconstitute(r, false, false));
	}

	public static RelativeFile newFromSafePath(RootPath root, String safePath) throws IOException {
		return new RelativeFile(root, RelativeFile.decodeSafePath(safePath));
	}

	private final RootPath root;
	private final String path;
	private final File absoluteFile;
	private final File relativeFile;
	private final String safePath;

	public RelativeFile(RootPath root, String path) throws IOException {
		this.root = root;
		File f = root.relativize(new File(path));
		this.relativeFile = f;
		this.path = f.getPath();
		this.absoluteFile = root.makeAbsolute(this.path);
		List<String> r = new ArrayList<String>();
		for (String s : FileNameTools.tokenize(this.path)) {
			r.add(RelativeFile.makeSafe(s));
		}
		this.safePath = FileNameTools.reconstitute(r, false, false, '/');
	}

	public String getPathHash() {
		return String.format("%08x", this.relativeFile.hashCode());
	}

	public String getPath() {
		return this.path;
	}

	/**
	 * <p>
	 * Returns a "universally-safe" path for the absoluteFile which escapes all
	 * potentially-dangerous characters with their URL-safe alternatives (i.e. ' ' -> '+', %XX
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

	public RootPath getRootPath() {
		return this.root;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.root, this.absoluteFile);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		RelativeFile other = RelativeFile.class.cast(obj);
		if (!Tools.equals(this.root, other.root)) { return false; }
		if (!Tools.equals(this.absoluteFile, other.absoluteFile)) { return false; }
		return true;
	}
}
