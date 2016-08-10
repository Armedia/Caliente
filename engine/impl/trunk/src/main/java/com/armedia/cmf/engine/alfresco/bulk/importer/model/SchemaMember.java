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

		Map<String, SchemaAttribute> localAttributes = new TreeMap<String, SchemaAttribute>();
		for (Property property : e.getProperties()) {
			String name = property.getName();
			// Is this a duplicate at this level?
			if (localAttributes.containsKey(name)) { throw new IllegalStateException(
				String.format("Duplicate attribute name [%s] on type [%s]", name, this.name)); }

			// Is this a duplicate at my parent's level?
			if ((parent != null)
				&& (parent.getAttribute(name) != null)) { throw new IllegalStateException(String.format(
					"Duplicate attribute name [%s] on type [%s] - already defined by a supertype", name, this.name)); }

			// No dupes, add the attribute
			Boolean mult = Tools.coalesce(property.getMultiple(), Boolean.FALSE);
			SchemaAttribute schemaAttribute = new SchemaAttribute(name, AlfrescoDataType.decode(property.getType()),
				mult.booleanValue());
			localAttributes.put(schemaAttribute.name, schemaAttribute);
		}

		// Next, apply the attributes from the mandatory aspects as our own
		Set<String> ma = new LinkedHashSet<String>();
		for (Aspect aspect : mandatoryAspects) {
			ma.add(aspect.name);

			// Add the aspect's attributes
			for (String attributeName : aspect.allAttributeNames) {
				SchemaAttribute attribute = aspect.getAttribute(attributeName);
				// If this attribute isn't declared on this type, or it's not decalred in a
				// parent type, or it's not declared in another mandatory aspect, we add it...
				if (!localAttributes.containsKey(attributeName)) {
					localAttributes.put(attributeName, attribute);
				}
			}
		}

		this.mandatoryAspects = Tools.freezeSet(ma);
		this.localAttributes = Tools.freezeMap(new LinkedHashMap<String, SchemaAttribute>(localAttributes));

		// Finally, create the list of all the attributes this object supports
		Set<String> allAttributeNames = new TreeSet<String>();
		allAttributeNames.addAll(this.localAttributes.keySet());
		if (parent != null) {
			allAttributeNames.addAll(parent.getAllAttributeNames());
		}
		this.allAttributeNames = Tools.freezeSet(allAttributeNames);
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