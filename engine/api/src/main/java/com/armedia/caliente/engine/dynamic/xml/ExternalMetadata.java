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
package com.armedia.caliente.engine.dynamic.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLStreamReader;

import com.armedia.caliente.engine.dynamic.xml.metadata.MetadataSet;
import com.armedia.caliente.engine.dynamic.xml.metadata.MetadataSource;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"metadataSources", "metadataSets"
})
@XmlRootElement(name = "external-metadata")
@XmlSchema("engine.xsd")
public class ExternalMetadata {

	@XmlElement(name = "data-source", required = false)
	protected List<MetadataSource> metadataSources;

	@XmlElement(name = "metadata-set", required = false)
	protected List<MetadataSet> metadataSets;

	public List<MetadataSource> getMetadataSources() {
		if (this.metadataSources == null) {
			this.metadataSources = new ArrayList<>();
		}
		return this.metadataSources;
	}

	public List<MetadataSet> getMetadataSets() {
		if (this.metadataSets == null) {
			this.metadataSets = new ArrayList<>();
		}
		return this.metadataSets;
	}

	public static ExternalMetadata loadFromXML(InputStream in) throws JAXBException {
		return XmlBase.loadFromXML(ExternalMetadata.class, in);
	}

	public static ExternalMetadata loadFromXML(Reader in) throws JAXBException {
		return XmlBase.loadFromXML(ExternalMetadata.class, in);
	}

	public static ExternalMetadata loadFromXML(XMLStreamReader in) throws JAXBException {
		return XmlBase.loadFromXML(ExternalMetadata.class, in);
	}
}