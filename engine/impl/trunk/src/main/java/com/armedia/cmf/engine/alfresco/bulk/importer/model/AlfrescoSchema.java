package com.armedia.cmf.engine.alfresco.bulk.importer.model;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import com.armedia.commons.utilities.Tools;

public class AlfrescoSchema {

	private final Map<String, SchemaMember<?>> typeIndex;
	private final Map<String, SchemaMember<?>> aspectIndex;

	public AlfrescoSchema(Collection<URI> modelFiles) throws IOException, JAXBException {
		List<AlfrescoContentModel> models = new ArrayList<AlfrescoContentModel>();
		Map<String, SchemaMember<?>> typeIndex = new TreeMap<String, SchemaMember<?>>();
		Map<String, SchemaMember<?>> aspectIndex = new TreeMap<String, SchemaMember<?>>();
		for (URI f : modelFiles) {
			AlfrescoContentModel model = AlfrescoContentModel.newModel(f, models);
			for (String typeName : model.getTypeNames()) {
				typeIndex.put(typeName, model.getType(typeName));
			}
			for (String aspectName : model.getAspectNames()) {
				aspectIndex.put(aspectName, model.getAspect(aspectName));
			}
			models.add(model);
		}
		this.typeIndex = Tools.freezeCopy(new LinkedHashMap<String, SchemaMember<?>>(typeIndex));
		this.aspectIndex = Tools.freezeCopy(new LinkedHashMap<String, SchemaMember<?>>(aspectIndex));
	}

	public AlfrescoType buildType(String typeName) {
		return buildType(typeName, null);
	}

	public AlfrescoType buildType(String typeName, Collection<String> aspectNames) {
		if (typeName == null) { throw new IllegalArgumentException("Must provide a non-null type name"); }
		if (aspectNames == null) {
			aspectNames = Collections.emptyList();
		}
		SchemaMember<?> baseType = this.typeIndex.get(typeName);
		if (baseType == null) { return null; }
		List<SchemaMember<?>> aspects = Collections.emptyList();
		if (aspectNames.size() > 0) {
			aspects = new ArrayList<SchemaMember<?>>(aspectNames.size());
			for (String aspectName : aspectNames) {
				SchemaMember<?> aspect = this.aspectIndex.get(aspectName);
				if (aspect == null) { throw new IllegalArgumentException(
					String.format("Could not find aspect [%s] to apply", aspectName)); }
				aspects.add(aspect);
			}
		}

		return new AlfrescoType(baseType, aspects);
	}

	public Set<String> getTypeNames() {
		return this.typeIndex.keySet();
	}

	public SchemaMember<?> getType(String name) {
		return this.typeIndex.get(name);
	}

	public Set<String> getAspectNames() {
		return this.aspectIndex.keySet();
	}

	public SchemaMember<?> getAspect(String name) {
		return this.aspectIndex.get(name);
	}
}