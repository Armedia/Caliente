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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

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
}