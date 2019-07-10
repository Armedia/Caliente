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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model.ClassElement;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model.Model;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.xml.XmlTools;

public class AlfrescoContentModel {

	public static class Aspect extends SchemaMember<Aspect> {
		private Aspect(Aspect parent, ClassElement e, Collection<Aspect> aspects) {
			super(parent, e, aspects);
		}
	}

	public static class Type extends SchemaMember<Type> {
		private Type(Type parent, ClassElement e, Collection<Aspect> aspects) {
			super(parent, e, aspects);
		}
	}

	private final String name;
	private final Map<String, Aspect> aspects;
	private final Map<String, Type> types;

	private AlfrescoContentModel(String name, Map<String, Type> types, Map<String, Aspect> aspects) {
		this.name = name;
		this.aspects = Tools.freezeMap(new LinkedHashMap<>(aspects));
		this.types = Tools.freezeMap(new LinkedHashMap<>(types));
	}

	public String getName() {
		return this.name;
	}

	public Aspect getAspect(String name) {
		return this.aspects.get(name);
	}

	public Set<String> getAspectNames() {
		return this.aspects.keySet();
	}

	public Type getType(String name) {
		return this.types.get(name);
	}

	public Set<String> getTypeNames() {
		return this.types.keySet();
	}

	public static AlfrescoContentModel newModel(URI model, Collection<AlfrescoContentModel> references)
		throws IOException, JAXBException {
		if (references == null) {
			references = Collections.emptyList();
		}

		final Model m;
		try (InputStream in = model.toURL().openStream()) {
			m = XmlTools.unmarshal(Model.class, "alfresco-model.xsd", in);
		}

		// First, make an index of all the aspects we have
		Map<String, ClassElement> rawAspects = new TreeMap<>();
		for (ClassElement e : m.getAspects()) {
			rawAspects.put(e.getName(), e);
		}
		Map<String, ClassElement> rawTypes = new TreeMap<>();
		for (ClassElement e : m.getTypes()) {
			rawTypes.put(e.getName(), e);
		}

		Map<String, Aspect> aspects = new TreeMap<>();
		Map<String, Type> types = new TreeMap<>();
		for (ClassElement e : m.getAspects()) {
			Aspect a = AlfrescoContentModel.buildAspect(m.getName(), e, aspects, rawAspects, references, null);
			aspects.put(a.name, a);
		}

		for (ClassElement e : m.getTypes()) {
			Type t = AlfrescoContentModel.buildType(m.getName(), e, types, aspects, rawTypes, references, null);
			types.put(t.name, t);
		}
		return new AlfrescoContentModel(m.getName(), types, aspects);
	}

	private static Aspect locateOrBuildAspect(final String modelName, final String sourceAspectName,
		final String aspectName, final Map<String, Aspect> aspects, final Map<String, ClassElement> rawAspects,
		final Collection<AlfrescoContentModel> references, final Set<String> visited) {
		Aspect ready = aspects.get(aspectName);
		if (ready == null) {
			// Not ready...find it amongst the references
			for (AlfrescoContentModel model : references) {
				ready = model.getAspect(aspectName);
				if (ready != null) {
					break;
				}
			}

			// Still not ready? May need construction
			if (ready == null) {
				ClassElement e = rawAspects.get(aspectName);
				if (e == null) {
					// ERROR!
					throw new IllegalStateException(
						String.format("Illegal forward reference of aspect [%s] from aspect [%s] in model [%s]",
							aspectName, sourceAspectName, modelName));
				}
				// Recurse - try to construct it
				ready = AlfrescoContentModel.buildAspect(modelName, e, aspects, rawAspects, references, visited);
				aspects.put(ready.name, ready);
			}
		}
		return ready;
	}

	private static Aspect buildAspect(final String modelName, final ClassElement src, final Map<String, Aspect> aspects,
		final Map<String, ClassElement> rawAspects, final Collection<AlfrescoContentModel> references,
		Set<String> visited) {

		if (visited == null) {
			visited = new LinkedHashSet<>();
		}

		final String name = src.getName();
		if (!visited.add(name)) {
			throw new IllegalStateException(String.format("Detected a dependency cycle: %s", visited));
		}

		final String parentName = src.getParent();

		final Aspect parent = (parentName != null
			? AlfrescoContentModel.locateOrBuildAspect(modelName, name, parentName, aspects, rawAspects, references,
				visited)
			: null);

		// First things first: check to see if all my mandatory aspects are ready. If not,
		// recurse into each of them to make sure they're ready
		List<Aspect> mandatoryAspects = new ArrayList<>(src.getMandatoryAspects().size());
		for (String mandatoryName : src.getMandatoryAspects()) {
			Aspect ready = AlfrescoContentModel.locateOrBuildAspect(modelName, name, mandatoryName, aspects, rawAspects,
				references, visited);
			mandatoryAspects.add(ready);
		}

		try {
			return new Aspect(parent, src, mandatoryAspects);
		} finally {
			visited.remove(name);
		}
	}

	private static Type locateOrBuildType(final String modelName, final String sourceTypeName, final String typeName,
		final Map<String, Type> types, final Map<String, Aspect> aspects, final Map<String, ClassElement> rawTypes,
		final Collection<AlfrescoContentModel> references, final Set<String> visited) {
		Type ready = types.get(typeName);
		if (ready == null) {
			// Not ready...find it amongst the references
			for (AlfrescoContentModel model : references) {
				ready = model.getType(typeName);
				if (ready != null) {
					break;
				}
			}

			// Still not ready? May need construction
			if (ready == null) {
				ClassElement e = rawTypes.get(typeName);
				if (e == null) {
					// ERROR!
					throw new IllegalStateException(
						String.format("Illegal forward reference of aspect [%s] from aspect [%s] in model [%s]",
							typeName, sourceTypeName, modelName));
				}
				// Recurse - try to construct it
				ready = AlfrescoContentModel.buildType(modelName, e, types, aspects, rawTypes, references, visited);
				types.put(ready.name, ready);
			}
		}
		return ready;
	}

	private static Type buildType(final String modelName, final ClassElement src, final Map<String, Type> types,
		final Map<String, Aspect> aspects, final Map<String, ClassElement> rawTypes,
		final Collection<AlfrescoContentModel> references, Set<String> visited) {

		if (visited == null) {
			visited = new LinkedHashSet<>();
		}

		final String name = src.getName();
		if (!visited.add(name)) {
			throw new IllegalStateException(String.format("Detected a dependency cycle: %s", visited));
		}

		final String parentName = src.getParent();
		final Type parent = (parentName != null
			? AlfrescoContentModel.locateOrBuildType(modelName, name, parentName, types, aspects, rawTypes, references,
				visited)
			: null);

		// First things first: check to see if all my mandatory aspects are ready. If not,
		// recurse into each of them to make sure they're ready
		List<Aspect> mandatoryAspects = new ArrayList<>(src.getMandatoryAspects().size());
		for (String mandatoryName : src.getMandatoryAspects()) {
			Aspect aspect = aspects.get(mandatoryName);
			if (aspect == null) {
				for (AlfrescoContentModel m : references) {
					aspect = m.getAspect(mandatoryName);
					if (aspect != null) {
						break;
					}
				}
				if (aspect == null) {
					throw new IllegalStateException(
						String.format("Illegal forward reference of aspect [%s] from type [%s] in model [%s]",
							mandatoryName, name, modelName));
				}
			}
			mandatoryAspects.add(aspect);
		}

		try {
			return new Type(parent, src, mandatoryAspects);
		} finally {
			visited.remove(name);
		}
	}
}