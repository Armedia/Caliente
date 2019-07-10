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
package com.armedia.caliente.engine.alfresco.bi.importer.model;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoContentModel.Aspect;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoContentModel.Type;
import com.armedia.commons.utilities.Tools;

public class AlfrescoSchema {
	private static final String[] NO_ASPECTS = {};

	private final Map<String, Type> typeIndex;
	private final Map<String, Aspect> aspectIndex;

	public AlfrescoSchema(Collection<URI> modelFiles) throws IOException, JAXBException {
		List<AlfrescoContentModel> models = new ArrayList<>();
		Map<String, Type> typeIndex = new TreeMap<>();
		Map<String, Aspect> aspectIndex = new TreeMap<>();
		for (URI uri : modelFiles) {
			AlfrescoContentModel model = AlfrescoContentModel.newModel(uri, models);
			for (String typeName : model.getTypeNames()) {
				typeIndex.put(typeName, model.getType(typeName));
			}
			for (String aspectName : model.getAspectNames()) {
				aspectIndex.put(aspectName, model.getAspect(aspectName));
			}
			models.add(model);
		}
		this.typeIndex = Tools.freezeCopy(new LinkedHashMap<>(typeIndex));
		this.aspectIndex = Tools.freezeCopy(new LinkedHashMap<>(aspectIndex));
	}

	public boolean hasType(String typeName) {
		return this.typeIndex.containsKey(typeName);
	}

	public boolean hasAspect(String aspectName) {
		return this.aspectIndex.containsKey(aspectName);
	}

	public AlfrescoType buildType(String typeName, String... aspectNames) {
		if (aspectNames == null) {
			aspectNames = AlfrescoSchema.NO_ASPECTS;
		}
		return buildType(typeName, Arrays.asList(aspectNames));
	}

	public AlfrescoType buildType(String typeName, Collection<String> aspectNames) {
		if (typeName == null) { throw new IllegalArgumentException("Must provide a non-null type name"); }
		if (aspectNames == null) {
			aspectNames = Collections.emptyList();
		}
		SchemaMember<?> baseType = this.typeIndex.get(typeName);
		if (baseType == null) { return null; }
		List<Aspect> aspects = Collections.emptyList();
		if (aspectNames.size() > 0) {
			aspects = new ArrayList<>(aspectNames.size());
			for (String aspectName : aspectNames) {
				Aspect aspect = this.aspectIndex.get(aspectName);
				if (aspect == null) {
					throw new IllegalArgumentException(
						String.format("Could not find aspect [%s] to apply", aspectName));
				}
				aspects.add(aspect);
			}
		}

		return new AlfrescoType(baseType, aspects);
	}

	public Set<String> getTypeNames() {
		return this.typeIndex.keySet();
	}

	public Type getType(String name) {
		return this.typeIndex.get(name);
	}

	public Set<String> getAspectNames() {
		return this.aspectIndex.keySet();
	}

	public Aspect getAspect(String name) {
		return this.aspectIndex.get(name);
	}
}