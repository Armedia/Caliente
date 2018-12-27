package com.armedia.caliente.engine.dynamic.mapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.xml.XmlInstanceException;
import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.caliente.engine.dynamic.xml.XmlNotFoundException;
import com.armedia.caliente.engine.dynamic.xml.mapper.AttributeMappings;
import com.armedia.caliente.engine.dynamic.xml.mapper.IncludeNamed;
import com.armedia.caliente.engine.dynamic.xml.mapper.Mapping;
import com.armedia.caliente.engine.dynamic.xml.mapper.MappingElement;
import com.armedia.caliente.engine.dynamic.xml.mapper.MappingSet;
import com.armedia.caliente.engine.dynamic.xml.mapper.NameMapping;
import com.armedia.caliente.engine.dynamic.xml.mapper.NamedMappings;
import com.armedia.caliente.engine.dynamic.xml.mapper.NamespaceMapping;
import com.armedia.caliente.engine.dynamic.xml.mapper.ResidualsMode;
import com.armedia.caliente.engine.dynamic.xml.mapper.SetValue;
import com.armedia.caliente.engine.dynamic.xml.mapper.TypeMappings;
import com.armedia.caliente.engine.importer.schema.ObjectType;
import com.armedia.caliente.engine.importer.schema.SchemaMember;
import com.armedia.caliente.engine.importer.schema.TypeSchema;
import com.armedia.caliente.engine.tools.KeyLockableCache;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class AttributeMapper {

	private static final Pattern NS_PARSER = Pattern.compile("^([^:]+):(.+)$");

	private static final XmlInstances<AttributeMappings> INSTANCES = new XmlInstances<>(AttributeMappings.class);

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
	private final String residualsPrefix;

	private static MappingRenderer buildRenderer(MappingElement e, Character parentSeparator) {
		if (!Mapping.class.isInstance(e)) { return null; }
		Mapping m = Mapping.class.cast(e);
		if (NameMapping.class.isInstance(m)) { return new AttributeRenderer(m, parentSeparator); }
		if (NamespaceMapping.class.isInstance(m)) { return new NamespaceRenderer(m, parentSeparator); }
		if (SetValue.class.isInstance(m)) { return new ConstantRenderer(m, parentSeparator); }
		return null;
	}

	public AttributeMapper(TypeSchema schema, String xmlSource, String residualsPrefix)
		throws XmlInstanceException, XmlNotFoundException {
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
					"No type or aspect named [{}] was found in the declared content model - ignoring this mapping set",
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
		this.residualsPrefix = residualsPrefix;
	}

	public String getResidualsPrefix() {
		return this.residualsPrefix;
	}

	protected String getSignature(final ObjectType type) {
		return type.getSignature();
	}

	protected Set<String> getImplicitSecondaries(final ObjectType type) {
		return type.getExtraAspects();
	}

	private MappingRendererSet getMappingRendererSet(final ObjectType type) {
		if (type == null) { return this.commonRenderers; }
		final String signature = getSignature(type);
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
							for (String aspect : getImplicitSecondaries(type)) {
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

	private String getResidualName(String attributeName) {
		// If if lacks a prefix, just pre-pend it...
		Matcher m = AttributeMapper.NS_PARSER.matcher(attributeName);
		return String.format("%s:%s", this.residualsPrefix, (m.matches() ? m.group(2) : attributeName));
	}

	public AttributeMappingResult renderMappedAttributes(final ObjectType type, CmfObject<CmfValue> object) {
		Objects.requireNonNull(object, "Must provide an object whose attribute values to map");
		Map<String, AttributeValue> finalValues = new TreeMap<>();
		final MappingRendererSet renderer = getMappingRendererSet(type);

		// Render the mapped values
		// The rendering will contain all attributes mapped. Time to filter out residuals from
		// declared attributes...
		final Map<String, AttributeValue> residuals = new TreeMap<>();
		final ResidualsModeTracker tracker = new ResidualsModeTracker();
		for (AttributeValue attribute : renderer.render(object, tracker)) {
			final String targetName = attribute.getTargetName();
			// First things first: is this attribute residual?
			if (!type.hasAttribute(targetName)) {
				residuals.put(targetName, attribute);
				continue;
			}

			// This attribute is a declared attribute, so we render it!
			// But make sure to take into account the overrides
			if (attribute.isOverride() || !finalValues.containsKey(targetName)) {
				finalValues.put(targetName, attribute);
			}
		}

		// Now, scan through the source object's attributes for any values that have not yet
		// been processed and should be included as direct mappings
		for (String sourceAttribute : object.getAttributeNames()) {
			if (finalValues.containsKey(sourceAttribute)) {
				// This is attribute has already been rendered, so skip it! Direct mappings
				// cannot override explicit mappings
				continue;
			}

			final boolean declared = type.hasAttribute(sourceAttribute);
			final CmfAttribute<CmfValue> att = object.getAttribute(sourceAttribute);
			final AttributeValue value = new AttributeValue(att, sourceAttribute, ',', false);

			// If the attribute is declared, then copy it directly...otherwise, it's should be
			// treated as a residual
			(declared ? finalValues : residuals).put(sourceAttribute, value);
		}

		boolean residualsEnabled = false;
		switch (tracker.getActiveResidualsMode()) {
			case MANDATORY:
			case INCLUDE:
				residualsEnabled = true;
				// Process residuals we've already identified
				for (AttributeValue residual : residuals.values()) {
					finalValues.put(getResidualName(residual.getTargetName()), residual);
				}

				// Fall-through
			default:
				break;
		}

		return new AttributeMappingResult(finalValues, residualsEnabled);
	}
}