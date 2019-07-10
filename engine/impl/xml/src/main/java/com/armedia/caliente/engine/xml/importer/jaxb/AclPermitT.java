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
@XmlType(name = "aclPermit.t")
public class AclPermitT {

	@XmlAttribute(name = "type", required = true)
	protected PermitTypeT type;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "level", required = true)
	protected int level;

	@XmlAttribute(name = "extended", required = false)
	protected String extended;

	public PermitTypeT getType() {
		return this.type;
	}

	public void setType(PermitTypeT value) {
		this.type = value;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public int getLevel() {
		return this.level;
	}

	public void setLevel(int value) {
		this.level = value;
	}

	public String getExtended() {
		return this.extended;
	}

	public void setExtended(String value) {
		this.extended = value;
	}

	@Override
	public String toString() {
		return String.format("AclPermitT [type=%s, name=%s, level=%s, extended=%s]", this.type, this.name, this.level,
			this.extended);
	}
}