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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "content.t", propOrder = {
	"paths", "renditions"
})
@XmlRootElement(name = "content")
public class Content {

	@XmlAttribute(name = "id", required = true)
	protected String id;

	@XmlElementWrapper(name = "paths", required = true)
	@XmlElement(name = "path", required = true)
	protected List<String> paths;

	@XmlElementWrapper(name = "renditions", required = true)
	@XmlElement(name = "rendition", required = true)
	protected List<Rendition> renditions;

	public String getId() {
		return this.id;
	}

	public Content setId(String id) {
		this.id = id;
		return this;
	}

	public List<String> getPaths() {
		if (this.paths == null) {
			this.paths = new ArrayList<>();
		}
		return this.paths;
	}

	public List<Rendition> getRenditions() {
		if (this.renditions == null) {
			this.renditions = new ArrayList<>();
		}
		return this.renditions;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.id, this.paths, this.renditions);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		Content other = Content.class.cast(obj);
		if (!Tools.equals(this.id, other.id)) { return false; }
		if (!Tools.equals(this.paths, other.paths)) { return false; }
		if (!Tools.equals(this.renditions, other.renditions)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Content [id=%s, paths=%s, renditions=%s]", this.id, this.paths, this.renditions);
	}
}