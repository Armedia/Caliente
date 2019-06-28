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
package com.armedia.caliente.engine.xml.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributeBase.t")
@XmlSeeAlso({
	AttributeT.class, AttributeDefT.class
})
public class AttributeBaseT implements Comparable<AttributeBaseT> {

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "dataType", required = true)
	protected DataTypeT dataType;

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public DataTypeT getDataType() {
		return this.dataType;
	}

	public void setDataType(DataTypeT value) {
		this.dataType = value;
	}

	@Override
	public int compareTo(AttributeBaseT o) {
		if (this == o) { return 0; }
		if (o == null) { return 1; }
		return Tools.compare(this.name, o.name);
	}

	@Override
	public String toString() {
		return String.format("AttributeBaseT [name=%s, dataType=%s]", this.name, this.dataType);
	}
}