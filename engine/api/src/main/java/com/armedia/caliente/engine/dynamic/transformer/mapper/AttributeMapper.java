/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.engine.dynamic.transformer.mapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
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
import com.armedia.caliente.engine.tools.KeyLockableCache.ReferenceType;
import com.armedia.caliente.store.CmfAttributeNameMapper;
import com.armedia.commons.utilities.Tools;

public class AttributeMapper {

	private static final AttributeMappings DEFAULT_MAPPINGS;
	static {
		DEFAULT_MAPPINGS = new AttributeMappings();
		MappingSet common = new MappingSet();
		common.setResidualsMode(ResidualsMode.EXCLUDE);
		AttributeMapper.DEFAULT_MAPPINGS.setCommonMappings(common);
	}

	private static final Pattern NS_PARSER = Pattern.compile("^([^:]+):(.+)$");

	private static final XmlInstances<AttributeMappings> INSTANCES = new XmlInstances<>(AttributeMappings.class);

	public static AttributeMapper getAttributeMapper(SchemaService schemaService, String location,
		String residualsPrefix, boolean failIfMissing) throws AttributeMappingException {
		try {
			return new AttributeMapper(new ConstructedTypeFactory(schemaService), location, residualsPrefix);
		} catch (Exception e) {
			if (!failIfMissing) { return null; }
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
	private final KeyLockableCache<String, MappingRendererSet> cache = new KeyLockableCache<>(ReferenceType.FINAL,
		Duration.ofSeconds(-1));

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Map<String, MappingRendererSet> typedMappings;
	private final MappingRendererSet commonRenderers;
	private final ConstructedTypeFactory constructedTypeFactory;
	private final String residualsPrefix;

	private static BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>> buildRenderer(
		MappingElement e, Character parentSeparator) {
		if (!Mapping.class.isInstance(e)) { return null; }
		Mapping m = Mapping.class.cast(e);
		if (NameMapping.class.isInstance(m)) {
			return new AttributeRenderer(NameMapping.class.cast(m), parentSeparator);
		}
		if (NamespaceMapping.class.isInstance(m)) {
			return new NamespaceRenderer(NamespaceMapping.class.cast(m), parentSeparator);
		}
		if (SetValue.class.isInstance(m)) { return new ConstantRenderer(SetValue.class.cast(m), parentSeparator); }
		return null;
	}

	public AttributeMapper(ConstructedTypeFactory constructedTypeFactory, String xmlSource, String residualsPrefix)
		throws XmlInstanceException, XmlNotFoundException {
		this.constructedTypeFactory = Objects.requireNonNull(constructedTypeFactory,
			"Must provide a non-null SchemaService instance");
		AttributeMappings xml = Tools.coalesce(AttributeMapper.INSTANCES.getInstance(xmlSource),
			AttributeMapper.DEFAULT_MAPPINGS);
		MappingSet commonMappings = xml.getCommonMappings();

		List<BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>>> renderers = new ArrayList<>();
		MappingRendererSet commonRenderers = null;
		if (commonMappings != null) {
			for (MappingElement e : commonMappings.getMappingElements()) {
				BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>> r = AttributeMapper
					.buildRenderer(e, commonMappings.getSeparator());
				if (r != null) {
					renderers.add(r);
				}
			}
			commonRenderers = new MappingRendererSet("<common>", commonMappings.getSeparator(),
				commonMappings.getResidualsMode(), renderers);
		}
		this.commonRenderers = commonRenderers;

		List<TypeMappings> typeMappings = new ArrayList<>();
		Map<String, BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>>> namedMappings = new TreeMap<>();
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
					final BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>> mappings = namedMappings
						.get(included);
					if (mappings == null) {
						// KABOOM!! Illegal forward reference
						throw new XmlInstanceException(
							String.format("Illegal forward reference of mappings set [%s] from mapping set [%s]",
								included, nm.getName()));
					}
					renderers.add(mappings);
				} else {
					BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>> renderer = AttributeMapper
						.buildRenderer(e, nm.getSeparator());
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
				BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>> renderer = AttributeMapper
					.buildRenderer(e, tm.getSeparator());
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

	private MappingRendererSet buildMappingRendererSet(ConstructedType type) {
		Map<String, BiFunction<DynamicObject, ResidualsModeTracker, Collection<AttributeMapping>>> renderers = new LinkedHashMap<>();

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

	private MappingRendererSet getMappingRendererSet(final ConstructedType type) {
		if (type == null) { return this.commonRenderers; }
		final String signature = type.getSignature();
		try {
			return this.cache.computeIfAbsent(signature, (s) -> buildMappingRendererSet(type));
		} catch (Exception e) {
			throw new RuntimeException(String.format("Failed to generate the mapping renderers for type [%s] (%s)",
				type.toString(), signature), e);
		}
	}

	private String getResidualName(String attributeName) {
		// If if lacks a prefix, just pre-pend it...
		Matcher m = AttributeMapper.NS_PARSER.matcher(attributeName);
		return String.format("%s:%s", this.residualsPrefix, (m.matches() ? m.group(2) : attributeName));
	}

	protected void applyResult(final ConstructedType type, final Collection<AttributeMapping> mappings,
		final boolean includeResiduals, DynamicObject object, CmfAttributeNameMapper nameMapper) {
		final Map<String, DynamicValue> dynamicValues = object.getAtt();
		if (!includeResiduals) {
			// Remove all residuals. That is: remove everything from the dyamicValues
			// map that won't be processed by a mapping
			Set<String> keepers = new LinkedHashSet<>();
			mappings.forEach((m) -> keepers.add(m.getSourceName()));
			dynamicValues.keySet().retainAll(keepers);
		}
		mappings.forEach((mapping) -> {
			DynamicValue oldValue = null;
			DynamicValue newValue = null;

			final String origName = mapping.getSourceName();
			final String newName = mapping.getTargetName();
			final String newMappedName = Tools.coalesce(nameMapper.decodeAttributeName(object.getType(), newName),
				newName);

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
			if (!includeResiduals && !type.hasAttribute(newMappedName)) { return; }

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

	public void renderMappedAttributes(final SchemaService schemaService, DynamicObject object,
		CmfAttributeNameMapper nameMapper) throws SchemaServiceException {
		Objects.requireNonNull(object, "Must provide an object whose attribute values to map");

		final ConstructedType type = this.constructedTypeFactory.constructType(schemaService, object.getSubtype(),
			object.getSecondarySubtypes());
		// If there's no type to map against, we simply skip it...
		if (type == null) { return; }

		Map<String, AttributeMapping> finalValues = new HashMap<>();
		final MappingRendererSet renderer = getMappingRendererSet(type);

		// Render the mapped values
		// The rendering will contain all attributes mapped. Time to filter out residuals from
		// declared attributes...
		final Map<String, AttributeMapping> residuals = new HashMap<>();
		final ResidualsModeTracker tracker = new ResidualsModeTracker();
		renderer.apply(object, tracker).forEach((attribute) -> {
			final String targetName = attribute.getTargetName();
			final String mappedName = Tools.coalesce(nameMapper.decodeAttributeName(object.getType(), targetName),
				targetName);

			// First things first: is this attribute residual?
			if (!type.hasAttribute(mappedName)) {
				residuals.put(targetName, attribute);
				return;
			}

			// This attribute is a declared attribute, so we render it!
			// But make sure to take into account the overrides
			if (attribute.isOverride() || !finalValues.containsKey(targetName)) {
				finalValues.put(targetName, attribute);
			}
		});

		// Now, scan through the source object's attributes for any values that have not yet
		// been processed and should be included as direct mappings
		object.getAtt().forEach((name, attribute) -> {
			if (finalValues.containsKey(name)) {
				// This is attribute has already been rendered, so skip it! Implicit mappings
				// cannot override explicit mappings
				return;
			}
			final String mappedName = Tools.coalesce(nameMapper.decodeAttributeName(object.getType(), name), name);

			// If the attribute is declared, then copy it directly...otherwise, it's should be
			// treated as a residual
			(type.hasAttribute(mappedName) ? finalValues : residuals).put(name,
				new AttributeMapping(attribute, name, ',', false));
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

		applyResult(type, finalValues.values(), residualsEnabled, object, nameMapper);
	}
}