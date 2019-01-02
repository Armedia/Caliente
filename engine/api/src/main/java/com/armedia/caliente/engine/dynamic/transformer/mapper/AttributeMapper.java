package com.armedia.caliente.engine.dynamic.transformer.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.ConstructedType;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.ConstructedTypeFactory;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaService;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaServiceException;
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
import com.armedia.caliente.engine.tools.KeyLockableCache;
import com.armedia.commons.utilities.Tools;

public class AttributeMapper {

	private static final Pattern NS_PARSER = Pattern.compile("^([^:]+):(.+)$");

	private static final XmlInstances<AttributeMappings> INSTANCES = new XmlInstances<>(AttributeMappings.class);

	public static AttributeMapper getAttributeMapper(SchemaService schemaService, String location,
		String residualsPrefix, boolean failIfMissing) throws AttributeMappingException {
		try {
			try {
				AttributeMappings attributeMappings = AttributeMapper.INSTANCES.getInstance(location);
				if (attributeMappings == null) { return null; }
				return new AttributeMapper(new ConstructedTypeFactory(schemaService), location, residualsPrefix);
			} catch (final XmlNotFoundException e) {
				if (!failIfMissing) { return null; }
				throw e;
			}
		} catch (Exception e) {
			String pre = "";
			String post = "";
			if (location == null) {
				pre = "default ";
			} else {
				post = String.format(" from [%s]", location);
			}
			throw new AttributeMappingException(
				String.format("Failed to load the %sattribute mapping configuration%s", pre, post), e);
		}
	}

	public static String getDefaultLocation() {
		return AttributeMapper.INSTANCES.getDefaultFileName();
	}

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
	private final ConstructedTypeFactory constructedTypeFactory;
	private final String residualsPrefix;

	private static MappingRenderer buildRenderer(MappingElement e, Character parentSeparator) {
		if (!Mapping.class.isInstance(e)) { return null; }
		Mapping m = Mapping.class.cast(e);
		if (NameMapping.class
			.isInstance(m)) { return new AttributeRenderer(NameMapping.class.cast(m), parentSeparator); }
		if (NamespaceMapping.class
			.isInstance(m)) { return new NamespaceRenderer(NamespaceMapping.class.cast(m), parentSeparator); }
		if (SetValue.class.isInstance(m)) { return new ConstantRenderer(SetValue.class.cast(m), parentSeparator); }
		return null;
	}

	public AttributeMapper(ConstructedTypeFactory constructedTypeFactory, String xmlSource, String residualsPrefix)
		throws XmlInstanceException, XmlNotFoundException {
		this.constructedTypeFactory = Objects.requireNonNull(constructedTypeFactory,
			"Must provide a non-null SchemaService instance");
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
			if (!constructedTypeFactory.hasType(tm.getName())
				&& !constructedTypeFactory.hasSecondaryType(tm.getName())) {
				this.log.warn(
					"No primary or secondary type named [{}] was found in the available schema - ignoring this mapping set",
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

	private MappingRendererSet getMappingRendererSet(final ConstructedType type) {
		if (type == null) { return this.commonRenderers; }
		final String signature = type.getSignature();
		try {
			return this.cache.createIfAbsent(signature, new ConcurrentInitializer<MappingRendererSet>() {
				@Override
				public MappingRendererSet get() throws ConcurrentException {
					Map<String, MappingRenderer> renderers = new LinkedHashMap<>();

					// First, the type itself
					MappingRendererSet set = AttributeMapper.this.typedMappings.get(type.getName());
					if (set != null) {
						renderers.put(type.getName(), set);
					}

					// Next, all of its hierarchical parents
					for (String a : type.getAncestors()) {
						set = AttributeMapper.this.typedMappings.get(a);
						if ((set != null) && !renderers.containsKey(a)) {
							renderers.put(a, set);
						}
					}

					// Finally, all its secondaries
					for (String s : type.getSecondaries()) {
						set = AttributeMapper.this.typedMappings.get(s);
						if ((set != null) && !renderers.containsKey(s)) {
							renderers.put(s, set);
						}
					}

					// Finally, add the common renderers
					if (AttributeMapper.this.commonRenderers != null) {
						renderers.put(null, AttributeMapper.this.commonRenderers);
					}

					return new MappingRendererSet(type.toString(), null, null, new ArrayList<>(renderers.values()));
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

	protected void applyResult(final ConstructedType type, final Map<String, AttributeMapping> mappings,
		final boolean includeResiduals, DynamicObject object) {
		final Map<String, DynamicValue> dynamicValues = object.getAtt();
		mappings.forEach((name, mapping) -> {
			DynamicValue oldValue = null;
			DynamicValue newValue = null;

			final String origName = mapping.getSourceName();
			final String newName = mapping.getTargetName();

			if (origName != null) {
				// This is a rename or a removal...
				oldValue = dynamicValues.remove(origName);

				if ((newName == null) || (oldValue == null)) {
					// This is just a removal, or there's nothing to rename
					return;
				}
			}

			// Ok...so this is either a rename or an assignment, so we
			// check if it's a residual and skip it as required
			if (!includeResiduals && !type.hasAttribute(newName)) { return; }

			newValue = new DynamicValue(newName, mapping.getType(), mapping.isRepeating());
			dynamicValues.put(newName, newValue);

			if (oldValue != null) {
				// It's a rename, so just copy the old values
				newValue.setValues(oldValue.getValues());
			} else {
				// It's a new attribute, so copy the constant values
				newValue.setValues(mapping.getValues());
			}
		});
	}

	public void renderMappedAttributes(final SchemaService schemaService, DynamicObject object)
		throws SchemaServiceException {
		Objects.requireNonNull(object, "Must provide an object whose attribute values to map");
		final ConstructedType type = this.constructedTypeFactory.constructType(schemaService, object.getSubtype(),
			object.getSecondarySubtypes());
		// If there's no type to map against, we simply skip it...
		if (type == null) { return; }
		Map<String, AttributeMapping> finalValues = new TreeMap<>();
		final MappingRendererSet renderer = getMappingRendererSet(type);

		// Render the mapped values
		// The rendering will contain all attributes mapped. Time to filter out residuals from
		// declared attributes...
		final Map<String, AttributeMapping> residuals = new TreeMap<>();
		final ResidualsModeTracker tracker = new ResidualsModeTracker();
		for (AttributeMapping attribute : renderer.render(object, tracker)) {
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
		object.getAtt().forEach((sourceAttribute, att) -> {
			if (finalValues.containsKey(sourceAttribute)) {
				// This is attribute has already been rendered, so skip it! Implicit mappings
				// cannot override explicit mappings
				return;
			}

			final boolean declared = type.hasAttribute(sourceAttribute);
			final AttributeMapping value = new AttributeMapping(att, sourceAttribute, ',', false);

			// If the attribute is declared, then copy it directly...otherwise, it's should be
			// treated as a residual
			(declared ? finalValues : residuals).put(sourceAttribute, value);
		});

		boolean residualsEnabled = false;
		switch (tracker.getActiveResidualsMode()) {
			case MANDATORY:
			case INCLUDE:
				residualsEnabled = true;
				// Process residuals we've already identified
				for (AttributeMapping residual : residuals.values()) {
					finalValues.put(getResidualName(residual.getTargetName()), residual);
				}

				// Fall-through
			default:
				break;
		}

		applyResult(type, finalValues, residualsEnabled, object);
	}
}