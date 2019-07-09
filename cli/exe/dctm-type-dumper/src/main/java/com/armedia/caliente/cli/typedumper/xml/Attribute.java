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
package com.armedia.caliente.cli.typedumper.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attribute.t", propOrder = {})
public class Attribute {

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "type", required = true)
	protected int type;

	@XmlAttribute(name = "repeating", required = false)
	protected Boolean repeating;

	@XmlAttribute(name = "qualified", required = false)
	protected Boolean qualified;

	@XmlAttribute(name = "length", required = false)
	protected Integer length;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public boolean isRepeating() {
		return Tools.coalesce(this.repeating, Boolean.FALSE);
	}

	public void setRepeating(Boolean repeating) {
		this.repeating = repeating;
	}

	public boolean isQualified() {
		return Tools.coalesce(this.qualified, Boolean.TRUE);
	}

	public void setQualified(Boolean qualified) {
		this.qualified = qualified;
	}

	public Integer getLength() {
		return this.length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name, this.type, this.repeating, this.qualified, this.length);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		Attribute other = Attribute.class.cast(obj);
		if (!Tools.equals(this.name, other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.repeating != other.repeating) { return false; }
		if (this.qualified != other.qualified) { return false; }
		if (!Tools.equals(this.length, other.length)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Attribute [name=%s, type=%s, repeating=%s, qualified=%s, length=%s]", this.name,
			this.type, this.repeating, this.qualified, this.length);
	}
}