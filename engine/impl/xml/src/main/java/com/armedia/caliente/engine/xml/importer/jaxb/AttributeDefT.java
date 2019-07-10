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
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributeDef.t")
public class AttributeDefT extends AttributeBaseT {

	@XmlAttribute(name = "length", required = false)
	protected int length = 0;

	@XmlAttribute(name = "repeating", required = false)
	protected boolean repeating = false;

	@XmlAttribute(name = "inherited", required = true)
	protected boolean inherited = false;

	@XmlAttribute(name = "sourceName", required = true)
	protected String sourceName;

	public int getLength() {
		return this.length;
	}

	public void setLength(int value) {
		this.length = value;
	}

	public boolean isRepeating() {
		return this.repeating;
	}

	public void setRepeating(boolean value) {
		this.repeating = value;
	}

	public boolean isInherited() {
		return this.inherited;
	}

	public void setInherited(boolean inherited) {
		this.inherited = inherited;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	@Override
	public String toString() {
		return String.format(
			"AttributeDefT [name=%s, dataType=%s, length=%s, repeating=%s, inherited=%s, sourceName=%s]", this.name,
			this.dataType, this.length, this.repeating, this.inherited);
	}
}