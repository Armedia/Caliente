package com.armedia.caliente.store.local;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfStorageException;
import com.armedia.commons.utilities.Tools;

public class LocalContentLocator implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String ENCODING = StandardCharsets.UTF_8.name();

	public static final String SCHEME_RAW = "raw";
	public static final String SCHEME_FIXED = "fixed";
	public static final String SCHEME_SAFE = "safe";

	public static final Set<String> SCHEMES = Tools.freezeSet(new HashSet<String>() {
		private static final long serialVersionUID = 1L;

		{
			add(LocalContentLocator.SCHEME_RAW);
			add(LocalContentLocator.SCHEME_FIXED);
			add(LocalContentLocator.SCHEME_SAFE);
		}
	});

	private static final Pattern PARSER = Pattern.compile("^([^:]+):(.*)$");

	public static boolean isSupported(String scheme) {
		return StringUtils.isNotBlank(scheme) && LocalContentLocator.SCHEMES.contains(scheme);
	}

	public static LocalContentLocator decode(String locator) throws CmfStorageException {
		if (StringUtils.isBlank(locator)) { return null; }
		return new LocalContentLocator(locator);
	}

	private static String encodePath(String path) {
		try {
			return URLEncoder.encode(path, LocalContentLocator.ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(String.format("A required encoding (%s) is not installed - cannot continue",
				LocalContentLocator.ENCODING), e);
		}
	}

	private static String decodePath(String path) {
		try {
			return URLDecoder.decode(path, LocalContentLocator.ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(String.format("A required encoding (%s) is not installed - cannot continue",
				LocalContentLocator.ENCODING), e);
		}
	}

	private final String scheme;
	private final String path;
	private final String encoded;

	private LocalContentLocator(String locator) throws CmfStorageException {
		Matcher m = LocalContentLocator.PARSER.matcher(locator);
		if (!m.matches()) {
			throw new CmfStorageException(
				String.format("Content locator string did not match the required regex %s: [%s]",
					LocalContentLocator.PARSER.pattern(), locator));
		}

		try {
			this.scheme = m.group(1);
			if (!LocalContentLocator.isSupported(this.scheme)) {
				throw new IllegalArgumentException(String.format("The scheme [%s] is not valid - must be one of %s",
					this.scheme, LocalContentLocator.SCHEMES));
			}

			this.path = LocalContentLocator.decodePath(m.group(2));
			if (StringUtils.isEmpty(this.path)) {
				throw new IllegalArgumentException("The path must be non-null, and non-empty");
			}

			this.encoded = locator;
		} catch (Exception e) {
			throw new CmfStorageException(
				String.format("Could not decode the string [%s] into a local content locator", locator), e);
		}

	}

	public LocalContentLocator(String scheme, String path) {
		if (!LocalContentLocator.isSupported(scheme)) {
			throw new IllegalArgumentException(
				String.format("The scheme [%s] is not valid, must be one of %s", scheme, LocalContentLocator.SCHEMES));
		}
		this.scheme = scheme;

		if (StringUtils.isEmpty(path)) {
			throw new IllegalArgumentException("The path must be non-null, and non-empty");
		}
		this.path = path;
		this.encoded = String.format("%s:%s", scheme, LocalContentLocator.encodePath(this.path));
	}

	public String getScheme() {
		return this.scheme;
	}

	public String getPath() {
		return this.path;
	}

	public String encode() {
		return this.encoded;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.scheme, this.path);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		LocalContentLocator other = LocalContentLocator.class.cast(obj);
		if (!Objects.equals(this.scheme, other.scheme)) { return false; }
		if (!Objects.equals(this.path, other.path)) { return false; }
		return true;
	}
}