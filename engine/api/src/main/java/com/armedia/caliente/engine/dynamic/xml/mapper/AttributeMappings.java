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
package com.armedia.caliente.engine.dynamic.xml.mapper;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.xml.XmlSchema;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributeMappings.t", propOrder = {
	"commonMappings", "namedMappings"
})
@XmlRootElement(name = "attribute-mappings")
@XmlSchema("engine.xsd")
public class AttributeMappings {

	@XmlElement(name = "common-mappings")
	protected MappingSet commonMappings;

	@XmlElements({
		@XmlElement(name = "named-mappings", type = NamedMappings.class, required = false),
		@XmlElement(name = "type-mappings", type = TypeMappings.class, required = false),
	})
	protected List<NamedMappings> namedMappings;

	public MappingSet getCommonMappings() {
		return this.commonMappings;
	}

	public void setCommonMappings(MappingSet value) {
		this.commonMappings = value;
	}

	public List<NamedMappings> getMappings() {
		if (this.namedMappings == null) {
			this.namedMappings = new ArrayList<>();
		}
		return this.namedMappings;
	}
}