package com.armedia.cmf.engine.tools;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.commons.utilities.FileNameTools;

public abstract class AclTools {

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
		Set<String> ret = new TreeSet<String>();
		for (String p : names) {
			if (p == null) {
				continue;
			}
			ret.add(AclTools.UrlEncode(p));
		}
		return FileNameTools.reconstitute(ret, false, false, AclTools.SEPARATOR);
	}

	public static Set<String> decode(String encodedNames) {
		Set<String> ret = new TreeSet<String>();
		if (encodedNames != null) {
			for (String p : new StrTokenizer(encodedNames, AclTools.SEPARATOR).getTokenList()) {
				ret.add(AclTools.UrlDecode(p));
			}
		}
		return ret;
	}

	private AclTools() {
	}
}
