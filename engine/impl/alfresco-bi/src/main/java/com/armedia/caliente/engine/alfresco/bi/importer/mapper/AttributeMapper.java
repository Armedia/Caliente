package com.armedia.caliente.engine.alfresco.bi.importer.mapper;

import java.text.ParseException;
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
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaAttribute;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaMember;
import com.armedia.caliente.engine.dynamic.xml.XmlInstanceException;
import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.caliente.engine.dynamic.xml.XmlNotFoundException;
import com.armedia.caliente.engine.tools.KeyLockableCache;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class AttributeMapper {

	private static final String DEFAULT_SCHEMA = "alfresco-bi.xsd";
	private static final String DEFAULT_FILENAME = "alfresco-attribute-map.xml";

	private static final XmlInstances<AttributeMappings> INSTANCES = new XmlInstances<>(AttributeMappings.class,
		AttributeMapper.DEFAULT_SCHEMA, AttributeMapper.DEFAULT_FILENAME);

	// Make a cache that doesn't expire items and they don't get GC'd either
	private final KeyLockableCache<String, MappingRendererSet> cache = new KeyLockableCache<String, MappingRendererSet>(
		TimeUnit.SECONDS, -1) {
		@Override
		protected CacheItem newCacheItem(String key, MappingRendererSet value) {
			return new DirectCacheItem(key, value);
		}
	};

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

	public AttributeMapper(AlfrescoSchema schema, String xmlSource) throws XmlInstanceException, XmlNotFoundException {
		AttributeMappings xml = AttributeMapper.INSTANCES.getInstance(xmlSource);
		if (xml == null) {
			xml = new AttributeMappings();
		}
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
				commonRenderers = new MappingRendererSet("<common>", commonMappings.getSeparator(),
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
						throw new XmlInstanceException(
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
			namedMappings.put(nm.getName(), new MappingRendererSet(nm.getName(), separator, residualsMode, renderers));
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
						throw new XmlInstanceException(
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
				new MappingRendererSet(tm.getName(), tm.getSeparator(), tm.getResidualsMode(), renderers));
		}
		this.typedMappings = Tools.freezeMap(new LinkedHashMap<>(typedMappings));
	}

	private String renderValue(AttributeValue attribute) {
		return renderValue(attribute.getSeparator(), attribute);
	}

	private String renderValue(char separator, Iterable<CmfValue> srcValues) {
		List<String> values = new ArrayList<>();
		for (CmfValue v : srcValues) {
			try {
				values.add(v.serialize());
			} catch (ParseException e) {
				throw new RuntimeException(
					String.format("Failed to render %s value [%s]", v.getDataType().name(), v.asString()), e);
			}
		}
		return Tools.joinEscaped(separator, values);
	}

	private MappingRendererSet getMappingRendererSet(final AlfrescoType type) {
		if (type == null) { return this.commonRenderers; }
		final String signature = type.getSignature();
		try {
			return this.cache.createIfAbsent(signature, new ConcurrentInitializer<MappingRendererSet>() {
				@Override
				public MappingRendererSet get() throws ConcurrentException {
					List<MappingRenderer> renderers = new ArrayList<>();
					SchemaMember<?> typeDecl = type.getDeclaration();
					Set<String> aspectsAdded = new HashSet<>();
					while (typeDecl != null) {
						// 1) find the mappings for the specific type
						MappingRendererSet rendererSet = AttributeMapper.this.typedMappings.get(typeDecl.getName());
						if (rendererSet != null) {
							// There's a specific renderer for this type, so add it!
							renderers.add(rendererSet);
						}

						// 2) find the mappings for the type's mandatory aspects
						for (String aspect : typeDecl.getMandatoryAspects()) {
							if (!aspectsAdded.add(aspect)) {
								// If this aspect is already being processed, we skip it
								continue;
							}
							rendererSet = AttributeMapper.this.typedMappings.get(aspect);
							if (rendererSet != null) {
								renderers.add(rendererSet);
							}
						}

						// 3) find the mappings for the type's undeclared (extra-attached) aspects.
						// We only do this for the first iteration (i.e. the leaf type)
						if (typeDecl == type.getDeclaration()) {
							for (String aspect : type.getExtraAspects()) {
								if (!aspectsAdded.add(aspect)) {
									// If this aspect is already being processed, we skip it
									continue;
								}
								rendererSet = AttributeMapper.this.typedMappings.get(aspect);
								if (rendererSet != null) {
									renderers.add(rendererSet);
								}
							}
						}

						// 4) Recurse upward through the parent type...
						typeDecl = typeDecl.getParent();
					}

					// Finally, add the common renderers
					if (AttributeMapper.this.commonRenderers != null) {
						renderers.add(AttributeMapper.this.commonRenderers);
					}
					return new MappingRendererSet(type.toString(), null, null, renderers);
				}
			});
		} catch (ConcurrentException e) {
			throw new RuntimeException(String.format("Failed to generate the mapping renderers for type [%s] (%s)",
				type.toString(), signature), e);
		}
	}

	public Map<String, String> renderMappedAttributes(final AlfrescoType type, CmfObject<CmfValue> object) {
		// 1) if type == null, end
		if (type == null) { return Collections.emptyMap(); }
		Objects.requireNonNull(object, "Must provide an object whose attribute values to map");
		Map<String, String> finalValues = new TreeMap<>();
		final MappingRendererSet renderer = getMappingRendererSet(type);
		if (renderer == null) { return finalValues; }

		// Render the mapped values
		Set<String> sourcesProcessed = new HashSet<>();
		// The rendering will contain all attributes mapped. Time to filter out residuals from
		// declared attributes...
		Map<String, AttributeValue> residuals = new TreeMap<>();
		ResidualsModeTracker tracker = new ResidualsModeTracker();
		for (AttributeValue attribute : renderer.render(object, tracker)) {
			final String targetName = attribute.getTargetName();
			// First things first: is this attribute residual?
			final SchemaAttribute schemaAttribute = type.getAttribute(targetName);
			if (schemaAttribute == null) {
				residuals.put(targetName, attribute);
				continue;
			}

			// This attribute is a declared attribute, so we render it!
			// But make sure to take into account the overrides
			if (attribute.isOverride() || !finalValues.containsKey(targetName)) {
				sourcesProcessed.add(attribute.getSourceName());
				finalValues.put(targetName, renderValue(attribute));
			}
		}

		switch (tracker.getActiveResidualsMode()) {
			case MANDATORY:
			case INCLUDE:
				// Process residuals we've already identified
				for (AttributeValue residual : residuals.values()) {
					finalValues.put(residual.getTargetName(), renderValue(residual));
					sourcesProcessed.add(residual.getSourceName());
				}

				// Now, scan through the source object's attributes for any values that have not yet
				// been processed and should be included as residuals
				for (String sourceAttribute : object.getAttributeNames()) {

					if (finalValues.containsKey(sourceAttribute)) {
						// This is a direct-map attribute that has already been rendered, so skip
						// it!
						continue;
					}

					final CmfAttribute<CmfValue> att = object.getAttribute(sourceAttribute);
					final SchemaAttribute schemaAttribute = type.getAttribute(sourceAttribute);
					if (schemaAttribute != null) {
						// This is a direct-map attribute that has not already been rendered, so
						// render it!
						finalValues.put(sourceAttribute, renderValue(',', att));
						continue;
					}

					if (sourcesProcessed.contains(sourceAttribute)) {
						// This attribute is not a direct-map...but has already been processed, so
						// skip it!
						continue;
					}

					// This attribute is not a direct-map, and has not yet been processed, so
					// process it!
					finalValues.put(sourceAttribute, renderValue(',', att));
				}

				// Fall-through
			default:
				break;
		}

		return finalValues;
	}
}