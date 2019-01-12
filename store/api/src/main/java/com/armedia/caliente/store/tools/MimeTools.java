/**
 * *******************************************************************
 *
 * THIS SOFTWARE IS PROTECTED BY U.S. AND INTERNATIONAL COPYRIGHT LAWS. REPRODUCTION OF ANY PORTION
 * OF THE SOURCE CODE, CONTAINED HEREIN, OR ANY PORTION OF THE PRODUCT, EITHER IN PART OR WHOLE, IS
 * STRICTLY PROHIBITED.
 *
 * Confidential Property of Armedia LLC. (c) Copyright Armedia LLC 2011. All Rights reserved.
 *
 * *******************************************************************
 */
package com.armedia.caliente.store.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.io.IOUtils;

import eu.medsea.mimeutil.MimeUtil;

/**
 * This class serves as a utility class to encapsulate the MIME functionality from the identifier
 * library.
 *
 * @author drivera@armedia.com
 *
 */
public class MimeTools {
	private static final String DEFAULT_MIME_STRING = "application/octet-stream";
	private static final Object MIME_LOCK = new Object();
	private static final Map<String, Boolean> MIME_VALID = new ConcurrentHashMap<>();
	private static final Map<String, Reference<MimeType>> MIME_CACHE = new ConcurrentHashMap<>();

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

	public static MimeType resolveMimeType(String type) {
		if (type == null) { return null; }
		Boolean valid = MimeTools.MIME_VALID.get(type);
		Reference<MimeType> ret = MimeTools.MIME_CACHE.get(type);
		if ((valid == null) || (ret == null) || (ret.get() == null)) {
			synchronized (MimeTools.MIME_LOCK) {
				valid = MimeTools.MIME_VALID.get(type);
				ret = MimeTools.MIME_CACHE.get(type);
				if ((valid == null) || (ret == null) || (ret.get() == null)) {
					try {
						ret = new WeakReference<>(new MimeType(type));
						MimeTools.MIME_CACHE.put(type, ret);
						valid = true;
					} catch (MimeTypeParseException e) {
						ret = null;
						valid = false;
					}
					MimeTools.MIME_VALID.put(type, valid);
					MimeTools.MIME_LOCK.notify();
				}
			}
		}
		return (ret != null ? ret.get() : null);
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