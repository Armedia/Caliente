package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
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

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Map<String, MappingRendererSet> typedMappings;
	private final MappingRendererSet commonRenderers;

	private static MappingRenderer buildRenderer(MappingElement e, Character parentSeparator) {
		if (!Mapping.class.isInstance(e)) { return null; }
		Mapping m = Mapping.class.cast(e);
		if (NameMapping.class.isInstance(m)) { return new AttributeRenderer(m, parentSeparator); }
		if (NamespaceMapping.class.isInstance(m)) { return new NamespaceRenderer(m, parentSeparator); }
		if (SetValue.class.isInstance(m)) { return new ConstantRenderer(m, parentSeparator); }
		return null;
	}

	public AttributeMapper(AlfrescoSchema schema, String xmlSource) throws Exception {
		AttributeMappings xml = AttributeMapper.loadMappings();
		MappingSet commonMappings = xml.getCommonMappings();

		List<MappingRenderer> renderers = new ArrayList<>();
		MappingRendererSet commonRenderers = null;
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
			}
		}
		this.commonRenderers = commonRenderers;

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
			typedMappings.put(tm.getName(),
				new MappingRendererSet(type, tm.getSeparator(), tm.getResidualsMode(), renderers));
		}
		this.typedMappings = Tools.freezeMap(new LinkedHashMap<>(typedMappings));
	}

	private String renderValue(AttributeValue attribute) {
		List<String> values = new ArrayList<>();
		for (CmfValue v : attribute) {
			// TODO: Do we want to use special renderers here? I.e. data type serializers?
			values.add(v.asString());
		}
		return Tools.joinEscaped(attribute.getSeparator(), values);
	}

	public Map<String, String> renderMappedAttributes(final AlfrescoType type, CmfObject<CmfValue> object) {
		// 1) if type == null, end
		if (type == null) { return Collections.emptyMap(); }

		Objects.requireNonNull(object, "Must provide an object whose attribute values to map");

		final String signature = type.getSignature();

		final MappingRendererSet renderer;
		try {
			renderer = this.cache.createIfAbsent(signature, new ConcurrentInitializer<MappingRendererSet>() {
				@Override
				public MappingRendererSet get() throws ConcurrentException {
					ResidualsMode residualsMode = null;
					List<MappingRenderer> renderers = new ArrayList<>();
					SchemaMember<?> typeDecl = type.getDeclaration();
					while (typeDecl != null) {
						// 1) find the mappings for the specific type
						MappingRenderer renderer = AttributeMapper.this.typedMappings.get(typeDecl.getName());
						if (renderer == null) {
							// WTF?!?!?
						}
						renderers.add(renderer);

						// Now process all declared (mandatory) aspects
						for (String aspect : type.getDeclaredAspects()) {
							if (type.isAspectInherited(aspect)) {
								// Skip inherited aspects
								continue;
							}
							renderer = AttributeMapper.this.typedMappings.get(aspect);
							if (renderer != null) {
								renderers.add(renderer);
							}
						}

						// Now process all undeclared (extra-attached) aspects...this should only
						// produce results on the first class since the inherited types won't have
						// "extra" aspects - only the mandatory declared ones.
						for (String aspect : type.getExtraAspects()) {
							if (type.isAspectInherited(aspect)) {
								// Skip inherited aspects
								continue;
							}
							renderer = AttributeMapper.this.typedMappings.get(aspect);
							if (renderer != null) {
								renderers.add(renderer);
							}
						}

						// Process the parent type's mappings
						typeDecl = typeDecl.getParent();
					}

					// Finally, add the common renderers
					renderers.add(AttributeMapper.this.commonRenderers);
					return new MappingRendererSet(null, null, null, renderers);
				}
			});
		} catch (ConcurrentException e) {
			throw new RuntimeException(String.format("Failed to generate the mapping renderers for type [%s] (%s)",
				type.toString(), signature), e);
		}

		// Render the mapped values
		Set<String> sourcesProcessed = new HashSet<>();
		Set<String> targetsProcessed = new HashSet<>();
		Map<String, String> values = new TreeMap<>();
		for (AttributeValue attributeRendition : renderer.render(object)) {
			final String targetName = attributeRendition.getTargetName();
			if (!targetsProcessed.add(targetName)) {
				// If this isn't an override, we skip it
				if (!attributeRendition.isOverride()) {
					continue;
				}
			}

			final String sourceName = attributeRendition.getSourceName();

			final String value = renderValue(attributeRendition);
			final boolean override = attributeRendition.isOverride();

		}
		// TODO: Process residuals
		return values;
	}
}