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
package com.armedia.caliente.engine.exporter.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.tools.xml.XmlSchema;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"sources", "metadata", "content"
})
@XmlRootElement(name = "extraction")
@XmlSchema("engine.xsd")
public class Extraction extends SettingsContainer {

	@XmlElementWrapper(name = "sources", required = true)
	@XmlElement(name = "source", required = true)
	protected List<ExtractSourceT> sources;

	@XmlElement(name = "metadata", required = false)
	protected ExtractComponentT metadata;

	@XmlElement(name = "content", required = false)
	protected ExtractComponentT content;

	public List<ExtractSourceT> getSources() {
		if (this.sources == null) {
			this.sources = new ArrayList<>();
		}
		return this.sources;
	}

	public ExtractComponentT getMetadata() {
		return this.metadata;
	}

	public void setMetadata(ExtractComponentT value) {
		this.metadata = value;
	}

	public ExtractComponentT getContent() {
		return this.content;
	}

	public void setContent(ExtractComponentT value) {
		this.content = value;
	}

}