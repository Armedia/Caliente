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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "property.t", propOrder = {
	"value"
})
public class PropertyT extends AttributeBaseT {

	@XmlElement(name = "value", required = false)
	protected List<String> value;

	@XmlAttribute(name = "repeating", required = true)
	protected boolean repeating;

	public boolean isRepeating() {
		return this.repeating;
	}

	public void setRepeating(boolean repeating) {
		this.repeating = repeating;
	}

	public List<String> getValue() {
		if (this.value == null) {
			this.value = new ArrayList<>();
		}
		return this.value;
	}

	@Override
	public String toString() {
		return String.format("AttributeT [name=%s, dataType=%s, repeating=%s value=%s]", this.name, this.dataType,
			this.repeating, this.value);
	}

}