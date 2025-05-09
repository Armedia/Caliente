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
package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "contentStream.t", propOrder = {
	"renditionId", "renditionPage", "modifier", "size", "hash", "location", "fileName", "mimeType", "properties"
})
public class ContentStreamT {

	@XmlElement(name = "renditionId", required = true)
	protected String renditionId;

	@XmlElement(name = "renditionPage", required = true)
	protected int renditionPage;

	@XmlElement(name = "modifier", required = true)
	protected String modifier;

	@XmlElement(name = "size", required = true)
	protected long size;

	@XmlElement(name = "hash", required = false)
	protected byte[] hash;

	@XmlElement(name = "location", required = true)
	protected String location;

	@XmlElement(name = "fileName", required = true)
	protected String fileName;

	@XmlElement(name = "mimeType", required = true)
	protected String mimeType;

	@XmlElementWrapper(name = "properties", required = false)
	@XmlElement(name = "property", required = false)
	protected List<ContentStreamPropertyT> properties;

	@XmlTransient
	protected final Map<String, String> props = new TreeMap<>();

	protected void beforeMarshal(Marshaller m) {
		if (this.properties == null) {
			this.properties = new ArrayList<>(this.props.size());
		}
		this.properties.clear();
		for (String k : this.props.keySet()) {
			this.properties.add(new ContentStreamPropertyT(k, this.props.get(k)));
		}
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		if (this.properties != null) {
			for (ContentStreamPropertyT p : this.properties) {
				if (!StringUtils.isBlank(p.getValue())) {
					this.props.put(p.name, p.value);
				}
			}
		}
	}

	public String getRenditionId() {
		return this.renditionId;
	}

	public void setRenditionId(String renditionId) {
		this.renditionId = renditionId;
	}

	public int getRenditionPage() {
		return this.renditionPage;
	}

	public void setRenditionPage(int renditionPage) {
		this.renditionPage = renditionPage;
	}

	public String getModifier() {
		return this.modifier;
	}

	public void setModifier(String modifier) {
		this.modifier = modifier;
	}

	public long getSize() {
		return this.size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public byte[] getHash() {
		return this.hash;
	}

	public void setHash(byte[] hash) {
		this.hash = hash;
	}

	public String getLocation() {
		return this.location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getFileName() {
		return this.fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getMimeType() {
		return this.mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String setProperty(String name, String value) {
		if (name == null) { throw new IllegalArgumentException("Must provide a non-null name"); }
		return this.props.put(name, value);
	}

	public String getProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a non-null name"); }
		return this.props.get(name);
	}

	public int getPropertyCount() {
		return this.props.size();
	}

	public void clearProperties() {
		this.props.clear();
	}

	public String clearProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a non-null name"); }
		return this.props.remove(name);
	}

	@Override
	public String toString() {
		return String.format(
			"ContentStreamT [renditionId=%s, renditionPage=%,d, modifier=%s, size=%s, hash=%s, location=%s, fileName=%s, mimeType=%s, properties=%s]",
			this.renditionId, this.renditionPage, this.modifier, this.size, Arrays.toString(this.hash), this.location,
			this.fileName, this.mimeType, this.props);
	}
}