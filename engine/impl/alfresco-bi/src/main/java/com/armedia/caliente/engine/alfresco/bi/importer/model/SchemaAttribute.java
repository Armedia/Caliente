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
package com.armedia.caliente.engine.alfresco.bi.importer.model;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model.MandatoryDef;
import com.armedia.commons.utilities.Tools;

public final class SchemaAttribute {

	public static enum Mandatory {
		//
		OPTIONAL, RELAXED, ENFORCED,
		//
		;
	}

	private static Mandatory decodeMandatory(MandatoryDef def) {
		if (def == null) { return Mandatory.OPTIONAL; }
		final boolean mandatory = Tools.coalesce(def.getValue(), Boolean.FALSE).booleanValue();
		if (!mandatory) { return Mandatory.OPTIONAL; }
		final boolean enforced = Tools.coalesce(def.getEnforced(), Boolean.FALSE).booleanValue();
		return (enforced ? Mandatory.ENFORCED : Mandatory.RELAXED);
	}

	public final String name;
	public final AlfrescoDataType type;
	public final boolean multiple;
	public final Mandatory mandatory;
	public final SchemaMember<?> declaration;

	SchemaAttribute(SchemaMember<?> declaration, String name, AlfrescoDataType type, MandatoryDef mandatory,
		boolean multiple) {
		this.declaration = declaration;
		this.name = name;
		this.type = type;
		this.mandatory = SchemaAttribute.decodeMandatory(mandatory);
		this.multiple = multiple;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name, this.type, this.mandatory, this.multiple, this.declaration);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		SchemaAttribute other = SchemaAttribute.class.cast(obj);
		if (!Tools.equals(this.name, other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.mandatory != other.mandatory) { return false; }
		if (this.multiple != other.multiple) { return false; }
		if (!Tools.equals(this.declaration, other.declaration)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("SchemaAttribute [name=%s, type=%s, mandatory=%s, multiple=%s, declaration=%s]", this.name,
			this.type, this.mandatory, this.multiple, this.declaration.name);
	}
}