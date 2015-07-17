package com.armedia.cmf.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;

import com.armedia.commons.utilities.CfgTools;

public final class CmfContentInfo {

	private final String qualifier;
	private long length = 0;
	private MimeType mimeType = null;
	private String fileName = null;

	private final Map<String, String> properties = new HashMap<String, String>();
	private final CfgTools cfg = new CfgTools(this.properties);

	public CmfContentInfo(String qualifier) {
		this.qualifier = qualifier;
	}

	public String getQualifier() {
		return this.qualifier;
	}

	public long getLength() {
		return this.length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public MimeType getMimeType() {
		return this.mimeType;
	}

	public void setMimeType(MimeType mimeType) {
		this.mimeType = mimeType;
	}

	public String getFileName() {
		return this.fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
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
}