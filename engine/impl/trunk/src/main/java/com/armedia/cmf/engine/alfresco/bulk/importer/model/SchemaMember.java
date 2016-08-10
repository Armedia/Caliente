package com.armedia.cmf.engine.alfresco.bulk.importer.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoContentModel.Aspect;
import com.armedia.cmf.engine.alfresco.bulk.importer.model.jaxb.ClassElement;
import com.armedia.cmf.engine.alfresco.bulk.importer.model.jaxb.Property;
import com.armedia.commons.utilities.Tools;

public abstract class SchemaMember<T extends SchemaMember<T>> {

	protected final T parent;
	protected final String name;

	protected final Set<String> mandatoryAspects;
	protected final Map<String, SchemaAttribute> localAttributes;
	protected final Set<String> allAttributeNames;

	SchemaMember(T parent, ClassElement e, Collection<Aspect> mandatoryAspects) {
		this.parent = parent;
		this.name = e.getName();

		Map<String, SchemaAttribute> schemaAttributes = new TreeMap<String, SchemaAttribute>();
		for (Property property : e.getProperties()) {
			String name = property.getName();
			// Is this a duplicate at this level?
			if (schemaAttributes.containsKey(name)) { throw new IllegalStateException(
				String.format("Duplicate attribute name [%s] on type [%s]", name, this.name)); }

			// Is this a duplicate at my parent's level?
			if ((parent != null)
				&& (parent.getAttribute(name) != null)) { throw new IllegalStateException(String.format(
					"Duplicate attribute name [%s] on type [%s] - already defined by a supertype", name, this.name)); }

			// No dupes, add the attribute
			Boolean mult = Tools.coalesce(property.getMultiple(), Boolean.FALSE);
			SchemaAttribute schemaAttribute = new SchemaAttribute(name, AlfrescoDataType.decode(property.getType()),
				mult.booleanValue());
			schemaAttributes.put(schemaAttribute.name, schemaAttribute);
		}

		Set<String> ma = new LinkedHashSet<String>();
		for (Aspect aspect : mandatoryAspects) {
			ma.add(aspect.name);

			// Add the aspect's attributes
			for (String attributeName : aspect.allAttributeNames) {
				SchemaAttribute attribute = aspect.getAttribute(attributeName);
				// If this attribute isn't declared on this type, or it's not decalred in a
				// parent type, or it's not declared in another mandatory aspect, we add it...
				if (!schemaAttributes.containsKey(attributeName)
					&& ((parent == null) || (parent.getAttribute(attributeName) == null))) {
					schemaAttributes.put(attributeName, attribute);
				}
			}
		}
		this.mandatoryAspects = Tools.freezeSet(ma);
		this.localAttributes = Tools.freezeMap(new LinkedHashMap<String, SchemaAttribute>(schemaAttributes));
		Set<String> attributeNames = new TreeSet<String>();
		attributeNames.addAll(this.localAttributes.keySet());
		if (parent != null) {
			attributeNames.addAll(parent.getAttributeNames());
		}
		this.allAttributeNames = Tools.freezeSet(attributeNames);
	}

	public T getParent() {
		return this.parent;
	}

	public Set<String> getMandatoryAspects() {
		return this.mandatoryAspects;
	}

	public SchemaAttribute getAttribute(String name) {
		if (!this.allAttributeNames.contains(name)) { return null; }
		SchemaAttribute schemaAttribute = this.localAttributes.get(name);
		if (schemaAttribute == null) {
			schemaAttribute = this.parent.getAttribute(name);
		}
		return schemaAttribute;
	}

	public Set<String> getAttributeNames() {
		return this.localAttributes.keySet();
	}

	public int getAttributeCount() {
		return this.localAttributes.size();
	}

	public Set<String> getAllAttributeNames() {
		return this.allAttributeNames;
	}

	public int getAllAttributeCount() {
		return this.allAttributeNames.size();
	}
}