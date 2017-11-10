package com.armedia.caliente.engine.alfresco.bi.importer.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model.ClassElement;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model.Property;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoContentModel.Aspect;
import com.armedia.commons.utilities.Tools;

public abstract class SchemaMember<T extends SchemaMember<T>> {

	protected final T parent;
	protected final String name;

	protected final Map<String, Aspect> mandatoryAspects;
	protected final Map<String, SchemaAttribute> localAttributes;
	protected final Set<String> allAttributeNames;

	SchemaMember(T parent, ClassElement e, Collection<Aspect> mandatoryAspects) {
		this.parent = parent;
		this.name = e.getName();

		Map<String, SchemaAttribute> localAttributes = new TreeMap<>();
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
			SchemaAttribute schemaAttribute = new SchemaAttribute(this, name,
				AlfrescoDataType.decode(property.getType()), property.getMandatory(), mult.booleanValue());
			localAttributes.put(schemaAttribute.name, schemaAttribute);
		}

		// Next, apply the attributes from the mandatory aspects as our own
		Map<String, Aspect> ma = new LinkedHashMap<>();
		for (Aspect aspect : mandatoryAspects) {
			ma.put(aspect.name, aspect);

			// Add the aspect's attributes
			for (String attributeName : aspect.getAllAttributeNames()) {
				SchemaAttribute attribute = aspect.getAttribute(attributeName);
				// If this attribute isn't declared on this type, or it's not declared in a
				// parent type, or it's not declared in another mandatory aspect, we add it...
				if (!localAttributes.containsKey(attributeName)) {
					localAttributes.put(attributeName, attribute);
				}
			}
		}

		this.mandatoryAspects = Tools.freezeMap(ma);
		this.localAttributes = Tools.freezeMap(new LinkedHashMap<>(localAttributes));

		// Finally, create the list of all the attributes this object supports
		Set<String> allAttributeNames = new TreeSet<>();
		allAttributeNames.addAll(this.localAttributes.keySet());
		if (parent != null) {
			allAttributeNames.addAll(parent.getAllAttributeNames());
		}
		this.allAttributeNames = Tools.freezeSet(new LinkedHashSet<>(allAttributeNames));
	}

	public T getParent() {
		return this.parent;
	}

	public Set<String> getMandatoryAspects() {
		return this.mandatoryAspects.keySet();
	}

	public boolean isDescendedOf(String name) {
		if (this.name.equals(name)) { return true; }
		if (this.mandatoryAspects.containsKey(name)) { return true; }
		for (String aspectName : this.mandatoryAspects.keySet()) {
			if (this.mandatoryAspects.get(aspectName).isDescendedOf(name)) { return true; }
		}
		if (this.parent != null) { return this.parent.isDescendedOf(name); }
		return false;
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

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.name, this.parent);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		@SuppressWarnings("unchecked")
		SchemaMember<T> other = (SchemaMember<T>) obj;
		if (!Tools.equals(this.name, other.name)) { return false; }
		if (!Tools.equals(this.parent, other.parent)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		final String type = getClass().getSimpleName();
		if (this.parent != null) { return String.format("%s [%s extends %s]", type, this.name, this.parent.name); }
		return String.format("%s [%s]", type, this.name);
	}
}