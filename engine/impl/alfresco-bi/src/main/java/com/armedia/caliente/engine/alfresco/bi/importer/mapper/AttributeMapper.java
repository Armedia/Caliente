package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.AttributeMappings;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.MappingSet;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoType;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaMember;
import com.armedia.caliente.engine.tools.KeyLockableCache;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

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

	public AttributeMapper() throws Exception {
		AttributeMappings xml = AttributeMapper.loadMappings();
		MappingSet commonMappings = xml.getCommonMappings();
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