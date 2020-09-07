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
package com.armedia.caliente.cli.typedumper.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "type.t", propOrder = {
	"attributes"
})
@XmlRootElement(name = "type")
public class Type {

	@XmlElement(name = "attribute", required = false)
	protected List<Attribute> attributes;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "super", required = false)
	protected String superName;

	public Type() {
	}

	public Type(IDfType type) throws DfException {
		Objects.requireNonNull(type, "Must provide an IDfType instance");
		this.name = type.getName();
		IDfType superType = type.getSuperType();
		if (superType != null) {
			this.superName = superType.getName();
		} else {
			this.superName = null;
		}
		final int attrCount = type.getInt("attr_count");
		final int startPos = type.getInt("start_pos");
		List<Attribute> attributes = new ArrayList<>();
		for (int i = startPos; i < attrCount; i++) {
			final String attrName = type.getRepeatingString("attr_name", i);
			final int attrType = type.getRepeatingInt("attr_type", i);
			final boolean attrRepeating = type.getRepeatingBoolean("attr_repeating", i);
			final int attrQualified = type.getRepeatingInt("attr_restriction", i);
			final int attrLength = type.getRepeatingInt("attr_length", i);
			Attribute att = new Attribute();
			att.setName(attrName);
			att.setType(attrType);
			if (attrRepeating) {
				att.setRepeating(attrRepeating);
			}
			if (attrQualified != 0) {
				att.setQualified(false);
			}
			if (attrType == IDfType.DF_STRING) {
				att.setLength(attrLength);
			}
			attributes.add(att);
		}
		this.attributes = attributes;
	}

	public List<Attribute> getAttributes() {
		if (this.attributes == null) {
			this.attributes = new ArrayList<>();
		}
		return this.attributes;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name, this.superName, this.attributes);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		Type other = Type.class.cast(obj);
		if (!Objects.equals(this.name, other.name)) { return false; }
		if (!Objects.equals(this.superName, other.superName)) { return false; }
		if (!Objects.equals(this.attributes, other.attributes)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Type [attributes=%s, name=%s, superName=%s]", this.attributes, this.name, this.superName);
	}
}