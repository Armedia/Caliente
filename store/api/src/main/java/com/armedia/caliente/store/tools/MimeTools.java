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
package com.armedia.caliente.store.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.armedia.commons.utilities.concurrent.ConcurrentTools;

import eu.medsea.mimeutil.MimeUtil;

/**
 * This class serves as a utility class to encapsulate the MIME functionality from the identifier
 * library.
 *
 *
 *
 */
public class MimeTools {
	private static final String DEFAULT_MIME_STRING = "application/octet-stream";
	private static final ConcurrentMap<String, Pair<Boolean, MimeType>> MIME_CACHE = new ConcurrentHashMap<>();

	public static final MimeType DEFAULT_MIME_TYPE;
	public static final MimeType UNKNOWN;

	static {
		DEFAULT_MIME_TYPE = MimeTools.resolveMimeType(MimeTools.DEFAULT_MIME_STRING);
		if (MimeTools.DEFAULT_MIME_TYPE == null) {
			throw new RuntimeException(
				String.format("Failed to parse the default mime string [%s]", MimeTools.DEFAULT_MIME_STRING));
		}
		UNKNOWN = MimeTools.DEFAULT_MIME_TYPE;
		MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
		MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector");
	}

	private static MimeType canonicalize(String type) {
		try {
			return new MimeType(type);
		} catch (MimeTypeParseException e) {
			return null;
		}
	}

	public static MimeType resolveMimeType(String type) {
		if (type == null) { return null; }
		final MimeType key = MimeTools.canonicalize(type);
		if (key != null) {
			type = key.toString();
		}
		Pair<Boolean, MimeType> data = ConcurrentTools.createIfAbsent(MimeTools.MIME_CACHE, type,
			(t) -> Pair.of(key != null, key));
		return ((data != null) && data.getKey() ? data.getValue() : null);
	}

	/**
	 * First, register the MIME detectors globally, and initialize the cache
	 */

	private static eu.medsea.mimeutil.MimeType cast(Object o) {
		return eu.medsea.mimeutil.MimeType.class.cast(o);
	}

	/**
	 * Convert to {@link MimeType} from {@link eu.medsea.mimeutil.MimeType}, by preserving the
	 * primary and sub types.
	 *
	 * @param type
	 * @return the equivalent {@link MimeType} instance to the given
	 *         {@link eu.medsea.mimeutil.MimeType}
	 */
	private static MimeType convert(eu.medsea.mimeutil.MimeType type) {
		if (type == null) { return null; }
		return MimeTools.resolveMimeType(String.format("%s/%s", type.getMediaType(), type.getSubType()));
	}

	public static MimeType determineMimeType(File data) throws IOException {
		try (FileInputStream in = new FileInputStream(data)) {
			return MimeTools.determineMimeType(data.getName(), in);
		}
	}

	/**
	 * Identifies the MIME type for the given data using MIME magic.
	 *
	 * @param data
	 * @return the correct {@link MimeType} instance detected from the given data
	 */
	public static MimeType determineMimeType(InputStream data) throws IOException {
		return MimeTools.determineMimeType(IOUtils.toByteArray(data));
	}

	/**
	 * Identifies the MIME type for the given data using MIME magic.
	 *
	 * @param data
	 * @return the correct {@link MimeType} instance detected from the given data
	 */
	public static MimeType determineMimeType(byte[] data) {
		return MimeTools.convert(MimeTools.cast(MimeUtil.getMimeTypes(data).iterator().next()));
	}

	/**
	 * Identifies the MIME type for the given fileName using Extension mapping
	 *
	 * @param fileName
	 * @return the correct {@link MimeType} instance detected from the given filename's extension
	 */
	public static MimeType determineMimeType(String fileName) {
		return MimeTools.convert(MimeTools.cast(MimeUtil.getMimeTypes(fileName).iterator().next()));
	}

	/**
	 * Identifies the MIME type for the given data and, if that fails, attempts to identify it via
	 * the fileName.
	 *
	 * @param data
	 * @param fileName
	 * @return the correct {@link MimeType} instance detected from the given data or filename
	 */
	public static MimeType determineMimeType(byte[] data, String fileName) {
		MimeType type = MimeTools.determineMimeType(data);
		if (type != MimeTools.UNKNOWN) { return type; }
		return MimeTools.determineMimeType(fileName);
	}

	/**
	 * Identifies the MIME type for the given data and, if that fails, attempts to identify it via
	 * the fileName.
	 *
	 * @param data
	 * @param fileName
	 * @return the correct {@link MimeType} instance detected from the given data or filename
	 */
	public static MimeType determineMimeType(InputStream data, String fileName) {
		MimeType type = null;
		try {
			type = MimeTools.determineMimeType(data);
		} catch (IOException e) {
			// Ignore it...try to do it by name anyway
		}
		if ((type != null) && (type != MimeTools.UNKNOWN)) { return type; }
		return MimeTools.determineMimeType(fileName);
	}

	/**
	 * Identifies the MIME type for the given fileName and, if that fails, attempts to identify it
	 * via the data sample.
	 *
	 * @param fileName
	 * @param data
	 * @return the correct {@link MimeType} instance detected from the given filename or data
	 */
	public static MimeType determineMimeType(String fileName, byte[] data) {
		MimeType type = MimeTools.determineMimeType(fileName);
		if (type != MimeTools.UNKNOWN) { return type; }
		return MimeTools.determineMimeType(data);
	}

	/**
	 * Identifies the MIME type for the given fileName and, if that fails, attempts to identify it
	 * via the data sample.
	 *
	 * @param fileName
	 * @param data
	 * @return the correct {@link MimeType} instance detected from the given filename or data
	 */
	public static MimeType determineMimeType(String fileName, InputStream data) {
		MimeType type = MimeTools.determineMimeType(fileName);
		if (type != MimeTools.UNKNOWN) { return type; }
		try {
			return MimeTools.determineMimeType(data);
		} catch (IOException e) {
			return MimeTools.UNKNOWN;
		}
	}
}