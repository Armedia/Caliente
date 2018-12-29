package com.armedia.caliente.engine.alfresco.bi.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoContentModel.Aspect;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoContentModel.Type;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoSchema;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaAttribute;
import com.armedia.caliente.engine.importer.schema.decl.AttributeDeclaration;
import com.armedia.caliente.engine.importer.schema.decl.SchemaService;
import com.armedia.caliente.engine.importer.schema.decl.TypeDeclaration;
import com.armedia.caliente.store.CmfDataType;
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
			CmfDataType dataType = Tools.coalesce(att.type.cmfDataType, CmfDataType.OTHER);
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
			CmfDataType dataType = Tools.coalesce(att.type.cmfDataType, CmfDataType.OTHER);
			attributes.add(new AttributeDeclaration(a, dataType, (att.mandatory == SchemaAttribute.Mandatory.ENFORCED),
				att.multiple));
		}
		return new TypeDeclaration(aspect.getName(), attributes, aspect.getMandatoryAspects(), parentName);
	}

	@Override
	public void close() {
	}

}