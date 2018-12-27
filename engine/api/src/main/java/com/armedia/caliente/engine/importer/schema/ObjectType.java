package com.armedia.caliente.engine.importer.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.codec.digest.DigestUtils;

import com.armedia.caliente.engine.importer.schema.SchemaContentModel.Aspect;
import com.armedia.caliente.engine.importer.schema.decl.SchemaDeclarationService;
import com.armedia.caliente.engine.importer.schema.decl.TypeDeclaration;
import com.armedia.commons.utilities.Tools;

public class ObjectType extends SchemaMember<ObjectType, TypeDeclaration> {

	public static final ObjectType NULL = new ObjectType();

	private final String signature;

	public static String stripNamespace(String attName) {
		if (attName == null) { return null; }
		return attName.replaceAll("^\\w+:", "");
	}

	private ObjectType() {
		super("");
		this.signature = null;
	}

	ObjectType(SchemaDeclarationService schemaService, TypeDeclaration declaration, Collection<String> secondaries) {
		super(schemaService, declaration, secondaries);
		this.declaration = declaration;
		if (secondaries == null) {
			secondaries = Collections.emptyList();
		}
		this.name = declaration.getName();
		Set<String> declaredSecondaries = new HashSet<>();
		if (declaration.getParentName() != null) {
			String parentName = declaration.getParentName();
			declaredSecondaries.addAll(type.parent.getAllAspects());
		}

		this.declaredSecondaries.addAll(declaration.getSecondaries());
		Map<String, Aspect> aspects = new LinkedHashMap<>(type.mandatoryAspects);

		Map<String, ObjectAttribute> attributes = new TreeMap<>();
		// Go through the parent's attributes
		if (type.parent != null) {
			for (String attribute : type.parent.getAllAttributeNames()) {
				attributes.put(attribute, type.parent.getAttribute(attribute));
			}
		}

		Map<String, Aspect> extraAspects = new TreeMap<>();
		for (Aspect aspect : secondaries) {
			aspects.put(aspect.name, aspect);
			if (!declaredSecondaries.contains(aspect.name)) {
				extraAspects.put(aspect.name, aspect);
			}
			for (String attribute : aspect.getAllAttributeNames()) {
				attributes.put(attribute, aspect.getAttribute(attribute));
			}
		}

		// Override any aspect's attributes with our own
		for (String attribute : type.getAllAttributeNames()) {
			attributes.put(attribute, type.getAttribute(attribute));
		}
		this.attributes = Tools.freezeMap(new LinkedHashMap<>(attributes));

		this.declaredSecondaries = Tools.freezeCopy(type.mandatoryAspects.keySet());
		this.extraSecondaries = Tools.freezeMap(extraAspects);
		this.secondaries = Tools.freezeMap(aspects);

		Set<String> s = new TreeSet<>();
		s.addAll(type.getAncestors());
		s.add(type.name);
		s.addAll(this.secondaries.keySet());
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (String str : s) {
			if (sb.length() > 1) {
				sb.append('|');
			}
			sb.append(str);
		}
		sb.append(']');
		this.signature = DigestUtils.sha256Hex(sb.toString());
	}

	public SchemaMember<?> getDeclaration() {
		return this.type;
	}

	@Override
	public String getSignature() {
		return this.signature;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public boolean isDescendedOf(String typeName) {
		if (this.secondaries.containsKey(typeName)) { return true; }
		if (this.type.isDescendedOf(typeName)) { return true; }
		for (String aspectName : this.secondaries.keySet()) {
			if (this.secondaries.get(aspectName).isDescendedOf(typeName)) { return true; }
		}
		return false;
	}

	public Set<String> getAspects() {
		return this.secondaries.keySet();
	}

	public Set<String> getExtraAspects() {
		return this.extraSecondaries.keySet();
	}

	public Set<String> getDeclaredAspects() {
		return this.declaredSecondaries;
	}

	@Override
	public boolean isAttributeInherited(String name) {
		return this.type.isAttributeInherited(name);
	}

	@Override
	public boolean isAspectInherited(String aspect) {
		return this.type.isAspectInherited(this.name);
	}

	@Override
	public ObjectAttribute getAttribute(String name) {
		return this.attributes.get(name);
	}

	public boolean hasAttribute(String name) {
		return this.attributes.containsKey(name);
	}

	@Override
	public Set<String> getAttributeNames() {
		return this.attributes.keySet();
	}

	@Override
	public int getAttributeCount() {
		return this.attributes.size();
	}

	@Override
	public String toString() {
		return String.format("ObjectType [name=%s, aspects=%s]", this.name, this.secondaries);
	}
}