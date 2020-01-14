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
package com.armedia.caliente.store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.activation.MimeType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public final class CmfContentStream implements Comparable<CmfContentStream> {

	public static final String DEFAULT_RENDITION = "$main$";
	public static final String BASENAME = "$basename$";

	private final CmfObjectRef object;
	private final int index;
	private final String renditionIdentifier;
	private final int renditionPage;
	private final String modifier;

	private long length = 0;
	private MimeType mimeType = null;
	private String extension = null;
	private String fileName = null;
	private String locator = null;

	private final Map<String, String> properties = new HashMap<>();
	private final CfgTools cfg = new CfgTools(this.properties);

	public CmfContentStream(CmfObjectRef object, int index) {
		this(object, index, null, 0, null);
	}

	public CmfContentStream(CmfObjectRef object, int index, int renditionPage) {
		this(object, index, null, renditionPage, null);
	}

	public CmfContentStream(CmfObjectRef object, int index, int renditionPage, String modifier) {
		this(object, index, null, renditionPage, modifier);
	}

	public CmfContentStream(CmfObjectRef object, int index, String renditionIdentifier) {
		this(object, index, renditionIdentifier, 0, null);
	}

	public CmfContentStream(CmfObjectRef object, int index, String renditionIdentifier, int renditionPage) {
		this(object, index, renditionIdentifier, renditionPage, null);
	}

	public CmfContentStream(CmfObjectRef object, int index, String renditionIdentifier, int renditionPage,
		String modifier) {
		this.object = new CmfObjectRef(object);
		this.index = (index < 0 ? 0 : index);
		this.renditionIdentifier = Tools.coalesce(renditionIdentifier, CmfContentStream.DEFAULT_RENDITION);
		this.renditionPage = renditionPage;
		this.modifier = Tools.coalesce(modifier, "");
	}

	public CmfObjectRef getObject() {
		return this.object;
	}

	public int getIndex() {
		return this.index;
	}

	public boolean isDefaultRendition() {
		return Objects.equals(CmfContentStream.DEFAULT_RENDITION, this.renditionIdentifier);
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
		return new HashSet<>(this.properties.keySet());
	}

	public void setLocator(String locator) {
		if (this.locator != null) {
			throw new IllegalStateException("A locator has already been assigned to this stream, can't reassign it");
		}
		this.locator = Objects.requireNonNull(locator, "Must provide a non-null locator");
	}

	public String getLocator() {
		return this.locator;
	}

	@Override
	public String toString() {
		return String.format(
			"CmfContentStream [object=%s-%s, renditionIdentifier=%s, renditionPage=%s, modifier=%s, length=%s, mimeType=%s, fileName=%s, locator=%s]",
			this.object.getType().name(), this.object.getId(), this.renditionIdentifier, this.renditionPage,
			this.modifier, this.length, this.mimeType, this.fileName, this.locator);
	}

	@Override
	public int compareTo(CmfContentStream o) {
		if (o == null) { return 1; }
		int r = 0;
		r = Tools.compare(this.object, o.object);
		if (r != 0) { return r; }
		r = Tools.compare(this.renditionIdentifier, o.renditionIdentifier);
		if (r != 0) { return r; }
		r = Tools.compare(this.renditionPage, o.renditionPage);
		if (r != 0) { return r; }
		r = Tools.compare(this.modifier, o.modifier);
		if (r != 0) { return r; }
		return 0;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.object, this.renditionIdentifier, this.renditionPage, this.modifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfContentStream other = CmfContentStream.class.cast(obj);
		if (!Objects.equals(this.object, other.object)) { return false; }
		if (!Objects.equals(this.renditionIdentifier, other.renditionIdentifier)) { return false; }
		if (this.renditionPage != other.renditionPage) { return false; }
		if (!Objects.equals(this.modifier, other.modifier)) { return false; }
		return true;
	}
}