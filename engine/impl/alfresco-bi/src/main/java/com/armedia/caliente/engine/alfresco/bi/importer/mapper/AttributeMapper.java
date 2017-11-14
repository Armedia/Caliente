package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.AttributeMappings;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.IncludeNamed;
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
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaMember;
import com.armedia.caliente.engine.tools.KeyLockableCache;
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

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Map<String, MappingRendererSet> typedMappings;

	private static MappingRenderer buildRenderer(MappingElement e, Character parentSeparator) {
		if (!Mapping.class.isInstance(e)) { return null; }
		Mapping m = Mapping.class.cast(e);
		if (NameMapping.class.isInstance(m)) { return new AttributeRendererImpl(m, parentSeparator); }
		if (NamespaceMapping.class.isInstance(m)) { return new NamespaceRenderer(m, parentSeparator); }
		if (SetValue.class.isInstance(m)) { return new ConstantRenderer(m, parentSeparator); }
		return null;
	}

	public AttributeMapper(AlfrescoSchema schema) throws Exception {
		AttributeMappings xml = AttributeMapper.loadMappings();
		MappingSet commonMappings = xml.getCommonMappings();

		List<MappingRenderer> renderers = new ArrayList<>();
		final MappingRendererSet commonRenderers;
		if (commonMappings != null) {
			for (MappingElement e : commonMappings.getMappingElements()) {
				MappingRenderer r = AttributeMapper.buildRenderer(e, commonMappings.getSeparator());
				if (r != null) {
					renderers.add(r);
				}
			}
			if (!renderers.isEmpty()) {
				commonRenderers = new MappingRendererSet(null, commonMappings.getSeparator(),
					commonMappings.getResidualsMode(), renderers);
			} else {
				commonRenderers = null;
			}
		} else {
			commonRenderers = null;
		}

		List<TypeMappings> typeMappings = new ArrayList<>();
		Map<String, MappingRendererSet> namedMappings = new TreeMap<>();
		for (NamedMappings nm : xml.getMappings()) {
			if (TypeMappings.class.isInstance(nm)) {
				typeMappings.add(TypeMappings.class.cast(nm));
				continue;
			}

			// Construct the mapping set for this:
			final ResidualsMode residualsMode = nm.getResidualsMode();
			final Character separator = nm.getSeparator();
			renderers = new ArrayList<>();
			for (MappingElement e : nm.getMappingElements()) {
				if (IncludeNamed.class.isInstance(e)) {
					String included = IncludeNamed.class.cast(e).getValue();
					included = StringUtils.strip(included);
					final MappingRendererSet mappings = namedMappings.get(included);
					if (mappings == null) {
						// KABOOM!! Illegal forward reference
						throw new Exception(
							String.format("Illegal forward reference of mappings set [%s] from mapping set [%s]",
								included, nm.getName()));
					}
					renderers.add(mappings);
				} else {
					MappingRenderer renderer = AttributeMapper.buildRenderer(e, nm.getSeparator());
					if (renderer != null) {
						renderers.add(renderer);
					}
				}
			}
			if (commonRenderers != null) {
				renderers.add(commonRenderers);
			}
			namedMappings.put(nm.getName(), new MappingRendererSet(null, separator, residualsMode, renderers));
		}

		Map<String, MappingRendererSet> typedMappings = new TreeMap<>();
		for (TypeMappings tm : typeMappings) {
			SchemaMember<?> type = schema.getType(tm.getName());
			if (type == null) {
				type = schema.getAspect(tm.getName());
			}
			if (type == null) {
				this.log.warn(
					"No type or aspect named [{}] was found in the declared Alfresco content model - ignoring this mapping set",
					tm.getName());
				continue;
			}

			// Construct the mapping set for this:
			final ResidualsMode residualsMode = tm.getResidualsMode();
			final Character separator = tm.getSeparator();
			renderers = new ArrayList<>();
			for (MappingElement e : tm.getMappingElements()) {
				MappingRenderer renderer = AttributeMapper.buildRenderer(e, tm.getSeparator());
				if ((renderer == null) && IncludeNamed.class.isInstance(e)) {
					// If this is an <include>, then get the element and add it to the rendering
					// pipeline!
					String included = IncludeNamed.class.cast(e).getValue();
					included = StringUtils.strip(included);
					renderer = namedMappings.get(included);
					if (renderer == null) {
						// KABOOM!! Illegal forward reference ... this shouldn't happen, though,
						// since the JAXB parser should trigger this error due to how the XSD is
						// built
						throw new Exception(
							String.format("No named mapping [%s] found, referenced from type mapping set [%s]",
								included, tm.getName()));
					}
				}
				// If there's something to add, add it!
				if (renderer != null) {
					renderers.add(renderer);
				}
			}

			if (commonRenderers != null) {
				renderers.add(commonRenderers);
			}
			typedMappings.put(tm.getName(), new MappingRendererSet(type, separator, residualsMode, renderers));
		}
		this.typedMappings = Tools.freezeMap(new LinkedHashMap<>(typedMappings));
	}

	/*
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
	
	*/
}