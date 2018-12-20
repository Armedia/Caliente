package com.armedia.caliente.engine.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public final class PathTools {
	public static final String ROOT = "root";

	public static final int MIN_FOLDER_LEVELS = 1;
	public static final int MAX_FOLDER_LEVELS = 7;
	public static final int DEFAULT_FOLDER_LEVELS = 7;

	private PathTools() {

	}

	public static File getRootDirectory(CfgTools cfg) throws IOException {
		String root = cfg.getString(PathTools.ROOT);
		if (root == null) { return null; }
		return new File(root).getCanonicalFile();
	}

	public static String addNumericPaths(List<String> paths, long objectNumber) {
		return PathTools.addNumericPaths(paths, objectNumber, PathTools.DEFAULT_FOLDER_LEVELS);
	}

	// Don't expose this method just yet
	protected static String addNumericPaths(List<String> paths, long objectNumber, int folderLevels) {
		Objects.requireNonNull(paths, "Must provide a list to store the paths in");
		folderLevels = Tools.ensureBetween(PathTools.MIN_FOLDER_LEVELS, folderLevels, PathTools.MAX_FOLDER_LEVELS);
		final String fullObjectNumber = String.format("%016x", objectNumber);

		// The number of levels tells us how large the prefix will be...
		final int firstLevelLength = (16 - (folderLevels * 2));

		// Make sure the contents all land in the bulk-import root location, so it's easy to point
		// the bulk importer at that directory and not import any unwanted crap
		int pos = 0;
		for (int i = 0; i < folderLevels; i++) {
			final int delta = (i == 0 ? firstLevelLength : 2);
			paths.add(fullObjectNumber.substring(pos, pos + delta));
			pos += delta;
		}
		return fullObjectNumber;
	}

	public static List<String> getNumericPaths(long objectNumber) {
		return PathTools.getNumericPaths(objectNumber, PathTools.DEFAULT_FOLDER_LEVELS);
	}

	// Don't expose this method just yet
	protected static List<String> getNumericPaths(long objectNumber, int folderLevels) {
		List<String> paths = new ArrayList<>();
		paths.add(PathTools.addNumericPaths(paths, objectNumber, folderLevels));
		return paths;
	}
}