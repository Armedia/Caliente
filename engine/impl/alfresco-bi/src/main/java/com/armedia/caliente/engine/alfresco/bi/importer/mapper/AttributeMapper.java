package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.AttributeMappings;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.IncludeNamed;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.Mapping;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.MappingElement;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.MappingSet;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.NamedMappings;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.NamespaceMapping;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.SetValue;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoType;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaMember;
import com.armedia.caliente.engine.tools.KeyLockableCache;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public class AttributeMapper {

	// Make a cache that doesn't expire items and they don't get GC'd either
	private final KeyLockableCache<String, Map<String, MappedValue>> cache = new KeyLockableCache<String, Map<String, MappedValue>>(
		TimeUnit.SECONDS, -1) {
		@Override
		protected CacheItem newCacheItem(String key, Map<String, MappedValue> value) {
			return new DirectCacheItem(key, value);
		}
	};

	private static AttributeMappings loadMappings() {
		return null;
	}

	public AttributeMapper() throws Exception {
		AttributeMappings xml = AttributeMapper.loadMappings();
		MappingSet commonMappings = xml.getCommonMappings();
		Collection<ValueMapping> common = new ArrayList<>();
		if (commonMappings != null) {
			for (MappingElement m : commonMappings.getMappingElements()) {
				ValueMapping M = null;
				if (Mapping.class.isInstance(m)) {
					Mapping mapping = Mapping.class.cast(m);
					if (SetValue.class.isInstance(mapping)) {
						M = new ValueConstant(mapping);
					} else if (NamespaceMapping.class.isInstance(m)) {
						M = new ValueMappingByNamespace(mapping);
					} else {
						M = new ValueMapping(mapping);
					}
				} else if (IncludeNamed.class.isInstance(m)) {
					// Handle the includes...
				}
				common.add(M);
			}
		}

		Map<String, Collection<ValueMapping>> named = new TreeMap<>();
		List<NamedMappings> namedMappings = xml.getMappings();
		if (namedMappings != null) {
			for (NamedMappings mappingSet : namedMappings) {
				if (named.containsKey(mappingSet.getName())) {
					// ERROR! Duplicate name
					throw new Exception(String.format("Duplicate mapping set name [%s]", mappingSet.getName()));
				}
			}
		}
	}

	public Properties getMappedAttributes(final AlfrescoType type, CmfObject<CmfValue> object) {
		Objects.requireNonNull(object, "Must provide an object whose attribute values to map");
		Properties properties = new Properties();
		// 1) if type == null, end
		if (type == null) { return properties; }

		final String signature = type.getSignature();

		final Map<String, MappedValue> mappings;
		try {
			mappings = this.cache.createIfAbsent(signature, new ConcurrentInitializer<Map<String, MappedValue>>() {
				@Override
				public Map<String, MappedValue> get() throws ConcurrentException {
					Map<String, MappedValue> mappings = new HashMap<>();
					SchemaMember<?> typeDecl = type.getDeclaration();
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
					return mappings;
				}
			});
		} catch (ConcurrentException e) {
			throw new RuntimeException(
				String.format("Failed to generate the mappings for type [%s] (%s)", type.toString(), signature), e);
		}

		// Render the mapped values
		for (String s : mappings.keySet()) {
			mappings.get(s).render(properties, object);
		}

		return properties;
	}

}
