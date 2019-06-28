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
package com.armedia.caliente.engine.dynamic.xml.mapper;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mappingSet.t", propOrder = {
	"mappingElements", "residuals"
})
@XmlSeeAlso({
	NamedMappings.class
})
public class MappingSet {

	@XmlElements({
		@XmlElement(name = "map", type = NameMapping.class, required = false),
		@XmlElement(name = "set", type = SetValue.class, required = false),
		@XmlElement(name = "include", type = IncludeNamed.class, required = false),
		@XmlElement(name = "nsmap", type = NamespaceMapping.class, required = false)
	})
	protected List<MappingElement> mappingElements;

	@XmlElement(name = "residuals", required = false)
	@XmlJavaTypeAdapter(ResidualsModeAdapter.class)
	protected ResidualsMode residuals;

	@XmlAttribute(name = "separator")
	protected String separator;

	public List<MappingElement> getMappingElements() {
		if (this.mappingElements == null) {
			this.mappingElements = new ArrayList<>();
		}
		return this.mappingElements;
	}

	public void setResidualsMode(ResidualsMode mode) {
		this.residuals = mode;
	}

	public ResidualsMode getResidualsMode() {
		return this.residuals;
	}

	public Character getSeparator() {
		if (this.separator == null) { return null; }
		return this.separator.charAt(0);
	}

	public void setSeparator(Character value) {
		this.separator = (value == null ? null : value.toString());
	}

}