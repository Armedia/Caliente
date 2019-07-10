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
package com.armedia.caliente.engine.alfresco.bi.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoContentModel.Aspect;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoContentModel.Type;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoSchema;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaAttribute;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.AttributeDeclaration;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaService;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.TypeDeclaration;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class AlfSchemaService implements SchemaService {

	private final AlfrescoSchema schema;

	public AlfSchemaService(AlfrescoSchema schema) {
		this.schema = schema;
	}

	@Override
	public Collection<String> getObjectTypeNames() {
		if (this.schema == null) { return Collections.emptyList(); }
		return this.schema.getTypeNames();
	}

	@Override
	public TypeDeclaration getObjectTypeDeclaration(String typeName) {
		if (this.schema == null) { return null; }
		Type type = this.schema.getType(typeName);
		if (type == null) { return null; }
		Type parent = type.getParent();
		String parentName = (parent != null ? parent.getName() : null);
		List<AttributeDeclaration> attributes = new ArrayList<>(type.getAttributeCount());
		for (String a : type.getAttributeNames()) {
			SchemaAttribute att = type.getAttribute(a);
			if (att == null) {
				continue;
			}
			CmfValue.Type dataType = Tools.coalesce(att.type.cmfValueType, CmfValue.Type.OTHER);
			attributes.add(new AttributeDeclaration(a, dataType, (att.mandatory == SchemaAttribute.Mandatory.ENFORCED),
				att.multiple));
		}
		return new TypeDeclaration(type.getName(), attributes, type.getMandatoryAspects(), parentName);
	}

	@Override
	public Collection<String> getSecondaryTypeNames() {
		if (this.schema == null) { return Collections.emptyList(); }
		return this.schema.getAspectNames();
	}

	@Override
	public TypeDeclaration getSecondaryTypeDeclaration(String secondaryTypeName) {
		if (this.schema == null) { return null; }
		if (this.schema == null) { return null; }
		Aspect aspect = this.schema.getAspect(secondaryTypeName);
		if (aspect == null) { return null; }
		Aspect parent = aspect.getParent();
		String parentName = (parent != null ? parent.getName() : null);
		List<AttributeDeclaration> attributes = new ArrayList<>(aspect.getAttributeCount());
		for (String a : aspect.getAttributeNames()) {
			SchemaAttribute att = aspect.getAttribute(a);
			if (att == null) {
				continue;
			}
			CmfValue.Type dataType = Tools.coalesce(att.type.cmfValueType, CmfValue.Type.OTHER);
			attributes.add(new AttributeDeclaration(a, dataType, (att.mandatory == SchemaAttribute.Mandatory.ENFORCED),
				att.multiple));
		}
		return new TypeDeclaration(aspect.getName(), attributes, aspect.getMandatoryAspects(), parentName);
	}

	@Override
	public void close() {
	}

}