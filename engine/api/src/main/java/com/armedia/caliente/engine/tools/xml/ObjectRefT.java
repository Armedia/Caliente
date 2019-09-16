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

package com.armedia.caliente.engine.tools.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "objectRef.t", propOrder = {
	"id"
})
public class ObjectRefT {

	@XmlAttribute(name = "type", required = true)
	protected CmfObject.Archetype archetype;

	@XmlValue
	protected String id;

	public ObjectRefT() {
	}

	public ObjectRefT(CmfObjectRef ref) {
		this.archetype = ref.getType();
		this.id = ref.getId();
	}

	public CmfObjectRef getObjectRef() {
		return new CmfObjectRef(this.archetype, this.id);
	}

	public CmfObject.Archetype getArchetype() {
		return this.archetype;
	}

	public void setArchetype(CmfObject.Archetype archetype) {
		this.archetype = archetype;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}
}