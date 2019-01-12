package com.armedia.caliente.engine.tools;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.text.StringTokenizer;

import com.armedia.commons.utilities.FileNameTools;

public abstract class AclTools {

	public static enum AccessorType {
		//
		USER, GROUP, ROLE,
		//
		;
	}

	private static final String ENCODING = "UTF-8";
	private static final char SEPARATOR = '|';

	private static String UrlEncode(String str) {
		try {
			return URLEncoder.encode(str, AclTools.ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(String.format("[%s] encoding not supported", AclTools.ENCODING), e);
		}
	}

	private static String UrlDecode(String str) {
		try {
			return URLDecoder.decode(str, AclTools.ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(String.format("[%s] encoding not supported", AclTools.ENCODING), e);
		}
	}

	public static String encode(Collection<String> names) {
		if ((names == null) || names.isEmpty()) { return ""; }
		Set<String> ret = new TreeSet<>();
		for (String p : names) {
			if (p == null) {
				continue;
			}
			ret.add(AclTools.UrlEncode(p));
		}
		return FileNameTools.reconstitute(ret, false, false, AclTools.SEPARATOR);
	}

	public static Set<String> decode(String encodedNames) {
		Set<String> ret = new TreeSet<>();
		if (encodedNames != null) {
			for (String p : new StringTokenizer(encodedNames, AclTools.SEPARATOR).getTokenList()) {
				ret.add(AclTools.UrlDecode(p));
			}
		}
		return ret;
	}

	private AclTools() {
	}
}
