/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.tools;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public final class PathTools {

	private static final String ENCODING = StandardCharsets.UTF_8.name();

	public static final String ROOT = "root";

	public static final int MIN_FOLDER_LEVELS = 1;
	public static final int MAX_FOLDER_LEVELS = 7;
	public static final int DEFAULT_FOLDER_LEVELS = 7;

	private PathTools() {

	}

	public static String makeSafe(String s) {
		try {
			return URLEncoder.encode(s, PathTools.ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new UncheckedIOException(
				String.format("Default encoding of %s is not supported...how?!?!", PathTools.ENCODING), e);
		}
	}

	public static String makeUnsafe(String s) {
		try {
			return URLDecoder.decode(s, PathTools.ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new UncheckedIOException(
				String.format("Default encoding of %s is not supported...how?!?!", PathTools.ENCODING), e);
		}
	}

	public static String decodeSafePath(String safePath) {
		List<String> r = new ArrayList<>();
		for (String s : FileNameTools.tokenize(safePath, '/')) {
			r.add(PathTools.makeUnsafe(s));
		}
		return FileNameTools.reconstitute(r, false, false, File.separatorChar);
	}

	public static String encodeSafePath(String path) {
		List<String> r = new ArrayList<>();
		for (String s : FileNameTools.tokenize(path.toString(), File.separatorChar)) {
			r.add(PathTools.makeSafe(s));
		}
		return FileNameTools.reconstitute(r, false, false, '/');
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