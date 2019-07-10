/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.cli.ticketdecoder.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "page.t", propOrder = {
	"path"
})
public class Page {

	@XmlAttribute(name = "number", required = true)
	protected long number;

	@XmlAttribute(name = "length", required = true)
	protected long length;

	@XmlAttribute(name = "hash", required = false)
	protected String hash;

	@XmlValue
	protected String path;

	public long getNumber() {
		return this.number;
	}

	public Page setNumber(long number) {
		this.number = number;
		return this;
	}

	public long getLength() {
		return this.length;
	}

	public Page setLength(long length) {
		this.length = length;
		return this;
	}

	public String getHash() {
		return this.hash;
	}

	public Page setHash(String hash) {
		if (StringUtils.isBlank(hash)) {
			hash = null;
		}
		this.hash = hash;
		return this;
	}

	public String getPath() {
		return this.path;
	}

	public Page setPath(String path) {
		this.path = path;
		return this;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.number, this.length, this.hash, this.path);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		Page other = Page.class.cast(obj);
		if (this.number != other.number) { return false; }
		if (this.length != other.length) { return false; }
		if (!Tools.equals(this.hash, other.hash)) { return false; }
		if (!Tools.equals(this.path, other.path)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Rendition [number=%d, length=%d, hash=%s, path=[%s]]", this.number, this.length,
			this.hash, this.path);
	}
}