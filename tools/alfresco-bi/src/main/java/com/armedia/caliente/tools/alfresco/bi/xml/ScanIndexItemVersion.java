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
package com.armedia.caliente.tools.alfresco.bi.xml;

import java.math.BigDecimal;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "version.t", propOrder = {
	"number", "content", "metadata"
})
@XmlRootElement(name = "version")
public class ScanIndexItemVersion implements Cloneable {
	@XmlElement(required = true)
	protected BigDecimal number;

	@XmlElement(required = true)
	protected String content;

	@XmlElement(required = true)
	protected String metadata;

	protected ScanIndexItemVersion(ScanIndexItemVersion copy) {
		if (copy != null) {
			this.number = copy.number;
			this.content = copy.content;
			this.metadata = copy.metadata;
		}
	}

	public ScanIndexItemVersion() {

	}

	public BigDecimal getNumber() {
		return this.number;
	}

	public void setNumber(BigDecimal number) {
		this.number = number;
	}

	public String getContent() {
		return this.content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getMetadata() {
		return this.metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.number, this.content, this.metadata);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ScanIndexItemVersion other = ScanIndexItemVersion.class.cast(obj);
		if (!Objects.equals(this.number, other.number)) { return false; }
		if (!Objects.equals(this.content, other.content)) { return false; }
		if (!Objects.equals(this.metadata, other.metadata)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("ScanIndexItemVersion [number=%s, content=%s, metadata=%s]", this.number, this.content,
			this.metadata);
	}

	@Override
	public ScanIndexItemVersion clone() {
		return new ScanIndexItemVersion(this);
	}
}