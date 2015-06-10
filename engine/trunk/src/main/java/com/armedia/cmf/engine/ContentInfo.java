package com.armedia.cmf.engine;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.cmf.storage.CmfEncodeableName;
import com.armedia.commons.utilities.CfgTools;

public final class ContentInfo {
	private static final String ENCODING = "UTF-8";
	private static final char ENTRY_SEP = '|';
	private static final char VALUE_SEP = '=';
	private static final Pattern CHECKER = Pattern.compile("^(?:[\\w-.~+%]*\\Q" + ContentInfo.VALUE_SEP
		+ "\\E[\\w-.~+%]*(?:\\Q" + ContentInfo.ENTRY_SEP + "\\E[\\w-.~+%]*\\Q" + ContentInfo.VALUE_SEP
		+ "\\E[\\w-.~+%]*)*)?$");

	private final String qualifier;
	private final Map<String, String> properties = new HashMap<String, String>();
	private final CfgTools cfg = new CfgTools(this.properties);

	public ContentInfo(String qualifier) {
		this.qualifier = qualifier;
	}

	public ContentInfo(String qualifier, String properties) {
		this(qualifier);
		if (properties != null) {
			if (!ContentInfo.CHECKER.matcher(properties).matches()) { throw new IllegalArgumentException(String.format(
				"The given encoded properties [%s] don't match the expected pattern /%s/", properties,
				ContentInfo.CHECKER.pattern())); }
			StrTokenizer tok = new StrTokenizer(properties, ContentInfo.ENTRY_SEP);
			while (tok.hasNext()) {
				String e = tok.next();
				int split = e.indexOf(ContentInfo.VALUE_SEP);
				String k = e.substring(0, split);
				String v = e.substring(split + 1);
				try {
					this.properties.put(URLDecoder.decode(k, ContentInfo.ENCODING),
						URLDecoder.decode(v, ContentInfo.ENCODING));
				} catch (UnsupportedEncodingException ex) {
					// This should be impossible...but we still check for it
					throw new RuntimeException(String.format("%s encoding is not supported", ContentInfo.ENCODING), ex);
				}
			}
		}
	}

	public String getQualifier() {
		return this.qualifier;
	}

	public CfgTools getCfgTools() {
		return this.cfg;
	}

	public String setProperty(CmfEncodeableName name, String value) {
		return setProperty(name.encode(), value);
	}

	public String setProperty(String name, String value) {
		return this.properties.put(name, value);
	}

	public boolean hasProperty(CmfEncodeableName name) {
		return hasProperty(name.encode());
	}

	public boolean hasProperty(String name) {
		return this.properties.containsKey(name);
	}

	public String getProperty(CmfEncodeableName name) {
		return getProperty(name.encode());
	}

	public String getProperty(String name) {
		return this.properties.get(name);
	}

	public String clearProperty(CmfEncodeableName name) {
		return clearProperty(name.encode());
	}

	public String clearProperty(String name) {
		return this.properties.remove(name);
	}

	public void clearAllProperties() {
		this.properties.clear();
	}

	public int getPropertyCount() {
		return this.properties.size();
	}

	public Set<String> getPropertyNames() {
		return new HashSet<String>(this.properties.keySet());
	}

	public String encodeProperties() {
		StringBuilder b = new StringBuilder();
		for (Map.Entry<String, String> e : this.properties.entrySet()) {
			if (b.length() > 0) {
				b.append(ContentInfo.ENTRY_SEP);
			}
			try {
				b.append(URLEncoder.encode(e.getKey(), ContentInfo.ENCODING)).append(ContentInfo.VALUE_SEP)
					.append(URLEncoder.encode(e.getValue(), ContentInfo.ENCODING));
			} catch (UnsupportedEncodingException ex) {
				// This should be impossible...but we still check for it
				throw new RuntimeException(String.format("%s encoding is not supported", ContentInfo.ENCODING), ex);
			}
		}
		return b.toString();
	}
}