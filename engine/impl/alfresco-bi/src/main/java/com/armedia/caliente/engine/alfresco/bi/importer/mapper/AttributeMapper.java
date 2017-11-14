package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.AttributeMappings;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.Mapping;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.MappingElement;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.MappingSet;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.NameMapping;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.NamedMappings;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.NamespaceMapping;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.ResidualsMode;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.SetValue;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.TypeMappings;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoSchema;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoType;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaMember;
import com.armedia.caliente.engine.tools.KeyLockableCache;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class AttributeMapper {

	// Make a cache that doesn't expire items and they don't get GC'd either
	private final KeyLockableCache<String, MappingRendererSet> cache = new KeyLockableCache<String, MappingRendererSet>(
		TimeUnit.SECONDS, -1) {
		@Override
		protected CacheItem newCacheItem(String key, MappingRendererSet value) {
			return new DirectCacheItem(key, value);
		}
	};

	private static AttributeMappings loadMappings() {
		return null;
	}

	private final ResidualsMode commonResidualsMode;
	private final List<MappingRenderer> commonRenderers;

	private static MappingRenderer buildRenderer(MappingElement e) {
		if (!Mapping.class.isInstance(e)) { return null; }
		Mapping m = Mapping.class.cast(e);
		if (NameMapping.class.isInstance(m)) { return new MappingRenderer(m); }
		if (NamespaceMapping.class.isInstance(m)) { return new NamespaceRenderer(m); }
		if (SetValue.class.isInstance(m)) { return new ConstantRenderer(m); }
		return null;
	}

	public AttributeMapper(AlfrescoSchema schema) throws Exception {
		AttributeMappings xml = AttributeMapper.loadMappings();
		MappingSet commonMappings = xml.getCommonMappings();

		this.commonResidualsMode = commonMappings.getResidualsMode();
		List<MappingRenderer> commonRenderers = new ArrayList<>();
		for (MappingElement e : commonMappings.getMappingElements()) {
			MappingRenderer r = AttributeMapper.buildRenderer(e);
			if (r == null) {
				// TODO: Log a warning before we simply ignore it?
				continue;
			}
			commonRenderers.add(r);
		}
		this.commonRenderers = Tools.freezeList(commonRenderers);

		Map<String, NamedMappings> namedMappings = new HashMap<>();
		Map<String, TypeMappings> typeMappings = new HashMap<>();
		for (NamedMappings nm : xml.getMappings()) {
			if (TypeMappings.class.isInstance(nm)) {
				typeMappings.put(nm.getName(), TypeMappings.class.cast(nm));
			} else {
				namedMappings.put(nm.getName(), nm);
			}
		}
	}

	public Map<String, String> renderMappedAttributes(final AlfrescoType type, CmfObject<CmfValue> object) {
		Objects.requireNonNull(object, "Must provide an object whose attribute values to map");
		// 1) if type == null, end
		if (type == null) { return Collections.emptyMap(); }

		final String signature = type.getSignature();

		final MappingRendererSet renderers;
		try {
			renderers = this.cache.createIfAbsent(signature, new ConcurrentInitializer<MappingRendererSet>() {
				@Override
				public MappingRendererSet get() throws ConcurrentException {
					List<MappingRenderer> renderers = new ArrayList<>();
					SchemaMember<?> typeDecl = type.getDeclaration();
					boolean residuals = false;
					while (typeDecl != null) {
						// 1) find the mappings for the specific type

						// 2) find the mappings for the declared, uninherited aspects

						if (typeDecl == type.getDeclaration()) {
							// This only happens the first time...
							// 3) find the mappings for the undeclared, uninherited aspects
						}

						// Process the parent type's mappings
						typeDecl = typeDecl.getParent();
					}
					// Finally, add the common renderers
					renderers.addAll(AttributeMapper.this.commonRenderers);
					return new MappingRendererSet(type, residuals, renderers);
				}
			});
		} catch (ConcurrentException e) {
			throw new RuntimeException(String.format("Failed to generate the mapping renderers for type [%s] (%s)",
				type.toString(), signature), e);
		}

		// Render the mapped values
		return renderers.render(object);
	}

}