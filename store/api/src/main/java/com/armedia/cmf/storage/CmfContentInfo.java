package com.armedia.cmf.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public final class CmfContentInfo implements Comparable<CmfContentInfo> {

	public static final String DEFAULT_RENDITION = "$main$";

	private final String renditionIdentifier;
	private final int renditionPage;
	private final String modifier;

	private long length = 0;
	private MimeType mimeType = null;
	private String extension = null;
	private String fileName = null;

	private final Map<String, String> properties = new HashMap<String, String>();
	private final CfgTools cfg = new CfgTools(this.properties);

	public CmfContentInfo() {
		this(null, 0, null);
	}

	public CmfContentInfo(int renditionPage) {
		this(null, renditionPage, null);
	}

	public CmfContentInfo(int renditionPage, String modifier) {
		this(null, renditionPage, modifier);
	}

	public CmfContentInfo(String renditionIdentifier) {
		this(renditionIdentifier, 0, null);
	}

	public CmfContentInfo(String renditionIdentifier, int renditionPage) {
		this(renditionIdentifier, renditionPage, null);
	}

	public CmfContentInfo(String renditionIdentifier, int renditionPage, String modifier) {
		this.renditionIdentifier = Tools.coalesce(renditionIdentifier, CmfContentInfo.DEFAULT_RENDITION);
		this.renditionPage = renditionPage;
		this.modifier = Tools.coalesce(modifier, "");
	}

	public boolean isDefaultRendition() {
		return Tools.equals(CmfContentInfo.DEFAULT_RENDITION, this.renditionIdentifier);
	}

	public String getRenditionIdentifier() {
		return this.renditionIdentifier;
	}

	public int getRenditionPage() {
		return this.renditionPage;
	}

	public String getExtension() {
		return this.extension;
	}

	public String getModifier() {
		return this.modifier;
	}

	public void setExtension(String extension) {
		if (StringUtils.isEmpty(extension)) {
			extension = null;
		}
		this.extension = extension;
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

	@Override
	public String toString() {
		return String.format(
			"CmfContentInfo [renditionIdentifier=%s, renditionPage=%s, length=%s, mimeType=%s, fileName=%s]",
			this.renditionIdentifier, this.renditionPage, this.length, this.mimeType, this.fileName);
	}

	@Override
	public int compareTo(CmfContentInfo o) {
		if (o == null) { return 1; }
		int r = 0;
		r = Tools.compare(this.renditionIdentifier, o.renditionIdentifier);
		if (r != 0) { return r; }
		r = Tools.compare(this.renditionPage, o.renditionPage);
		if (r != 0) { return r; }
		return 0;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.renditionIdentifier, this.renditionPage);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfContentInfo other = CmfContentInfo.class.cast(obj);
		if (!Tools.equals(this.renditionIdentifier, other.renditionIdentifier)) { return false; }
		if (this.renditionPage != other.renditionPage) { return false; }
		return true;
	}
}