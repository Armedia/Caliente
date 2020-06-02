package com.armedia.caliente.engine.common;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.commons.definitions.Choice;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDateTimeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDecimalDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyIntegerDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyStringDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDateTimeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDecimalDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyIntegerDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyStringDefinition;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.DateTimeResolution;
import org.apache.chemistry.opencmis.commons.enums.DecimalPrecision;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ChoiceImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriDefinitionImpl;
import org.apache.commons.lang3.tuple.Pair;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.codec.Codec;
import com.armedia.commons.utilities.codec.EnumCodec;
import com.armedia.commons.utilities.codec.StringCodec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class PropertyDefinitionEncoder {

	private static final Function<String, String> STRING = Function.identity();
	private static final Codec<Object, String> STRING_CODEC = new StringCodec<>(Tools::toString);

	private static final Function<String, Boolean> BOOLEAN = Boolean::valueOf;
	private static final Codec<Object, String> BOOLEAN_CODEC = new StringCodec<>(Boolean::valueOf);

	private static final Function<String, BigInteger> BIGINTEGER = BigInteger::new;
	private static final Codec<Object, String> BIGINTEGER_CODEC = new StringCodec<>(BigInteger::new);

	private static final Function<String, BigDecimal> BIGDECIMAL = BigDecimal::new;
	private static final Codec<Object, String> BIGDECIMAL_CODEC = new StringCodec<>(BigDecimal::new);

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
	private static final Function<String, Object> DATETIME_DEC = (str) -> GregorianCalendar
		.from(ZonedDateTime.from(PropertyDefinitionEncoder.FORMATTER.parse(str)));
	private static final Function<Object, String> DATETIME_ENC = (cal) -> PropertyDefinitionEncoder.FORMATTER
		.format(GregorianCalendar.class.cast(cal).toZonedDateTime());
	private static final Codec<Object, String> DATETIME_CODEC = new StringCodec<>(
		PropertyDefinitionEncoder.DATETIME_ENC, PropertyDefinitionEncoder.DATETIME_DEC);

	private static final String LBL_PROPERTY_TYPE = "propertyType";
	private static final String LBL_DEFAULT_VALUE = "defaultValue";
	private static final String LBL_CHOICE_NAME = "displayName";
	private static final String LBL_CHOICE_VALUE = "value";
	private static final String LBL_CHOICE = "choice";

	private static class PropertyDefinitionAccessor<V, R extends PropertyDefinition<?>, W extends MutablePropertyDefinition<?>> {
		private final Function<R, V> reader;
		private final BiConsumer<W, V> writer;
		private final Codec<V, String> codec;

		private PropertyDefinitionAccessor(Function<R, V> reader, BiConsumer<W, V> writer,
			Function<String, V> decoder) {
			this.reader = reader;
			this.writer = writer;
			this.codec = new StringCodec<>(decoder);
		}

		private PropertyDefinitionAccessor(Function<R, V> reader, BiConsumer<W, V> writer, Codec<V, String> codec) {
			this.reader = reader;
			this.writer = writer;
			this.codec = codec;
		}

		public String get(PropertyDefinition<?> def) {
			if (this.reader == null) { return null; }

			// I don't like this, but this is the easiest way to get it done
			// Adding the classes for stricter type validation adds unnecessary overhead

			@SuppressWarnings("unchecked")
			R r = (R) def;

			return this.codec.encode(this.reader.apply(r));
		}

		public void set(Object def, String value) {
			if (def == null) { return; }
			if (!MutablePropertyDefinition.class.isInstance(def)) { return; }

			// I don't like this, but this is the easiest way to get it done
			// Adding the classes for stricter type validation adds unnecessary overhead

			@SuppressWarnings("unchecked")
			W w = (W) def;

			this.writer.accept(w, this.codec.decode(value));
		}
	}

	private static final Map<String, PropertyDefinitionAccessor<?, ?, ?>> COMMON_ACCESSORS;
	private static final Map<PropertyType, Map<String, PropertyDefinitionAccessor<?, ?, ?>>> TYPED_ACCESSORS;
	static {
		Map<String, PropertyDefinitionAccessor<?, ?, ?>> accessors = new HashMap<>();

		accessors.put("id", new PropertyDefinitionAccessor<>(PropertyDefinition::getId,
			MutablePropertyDefinition::setId, PropertyDefinitionEncoder.STRING));
		accessors.put("localNamespace", new PropertyDefinitionAccessor<>(PropertyDefinition::getLocalNamespace,
			MutablePropertyDefinition::setLocalNamespace, PropertyDefinitionEncoder.STRING));
		accessors.put("localName", new PropertyDefinitionAccessor<>(PropertyDefinition::getLocalName,
			MutablePropertyDefinition::setLocalName, PropertyDefinitionEncoder.STRING));
		accessors.put("queryName", new PropertyDefinitionAccessor<>(PropertyDefinition::getQueryName,
			MutablePropertyDefinition::setQueryName, PropertyDefinitionEncoder.STRING));
		accessors.put("displayName", new PropertyDefinitionAccessor<>(PropertyDefinition::getDisplayName,
			MutablePropertyDefinition::setDisplayName, PropertyDefinitionEncoder.STRING));
		accessors.put("description", new PropertyDefinitionAccessor<>(PropertyDefinition::getDescription,
			MutablePropertyDefinition::setDescription, PropertyDefinitionEncoder.STRING));
		accessors.put("propertyType", new PropertyDefinitionAccessor<>(PropertyDefinition::getPropertyType,
			MutablePropertyDefinition::setPropertyType, new EnumCodec<>(PropertyType.class)));
		accessors.put("cardinality", new PropertyDefinitionAccessor<>(PropertyDefinition::getCardinality,
			MutablePropertyDefinition::setCardinality, new EnumCodec<>(Cardinality.class)));
		accessors.put("updatability", new PropertyDefinitionAccessor<>(PropertyDefinition::getUpdatability,
			MutablePropertyDefinition::setUpdatability, new EnumCodec<>(Updatability.class)));
		accessors.put("inherited", new PropertyDefinitionAccessor<>(PropertyDefinition::isInherited,
			MutablePropertyDefinition::setIsInherited, PropertyDefinitionEncoder.BOOLEAN));
		accessors.put("required", new PropertyDefinitionAccessor<>(PropertyDefinition::isRequired,
			MutablePropertyDefinition::setIsRequired, PropertyDefinitionEncoder.BOOLEAN));
		accessors.put("queryable", new PropertyDefinitionAccessor<>(PropertyDefinition::isQueryable,
			MutablePropertyDefinition::setIsQueryable, PropertyDefinitionEncoder.BOOLEAN));
		accessors.put("orderable", new PropertyDefinitionAccessor<>(PropertyDefinition::isOrderable,
			MutablePropertyDefinition::setIsOrderable, PropertyDefinitionEncoder.BOOLEAN));
		accessors.put("openChoice", new PropertyDefinitionAccessor<>(PropertyDefinition::isOpenChoice,
			MutablePropertyDefinition::setIsOpenChoice, PropertyDefinitionEncoder.BOOLEAN));
		COMMON_ACCESSORS = Tools.freezeMap(accessors);

		Map<PropertyType, Map<String, PropertyDefinitionAccessor<?, ?, ?>>> typedAccessors = new EnumMap<>(
			PropertyType.class);

		// Integer
		accessors = new HashMap<>();
		accessors.put("minValue", new PropertyDefinitionAccessor<>(PropertyIntegerDefinition::getMinValue,
			MutablePropertyIntegerDefinition::setMinValue, PropertyDefinitionEncoder.BIGINTEGER));
		accessors.put("maxValue", new PropertyDefinitionAccessor<>(PropertyIntegerDefinition::getMaxValue,
			MutablePropertyIntegerDefinition::setMaxValue, PropertyDefinitionEncoder.BIGINTEGER));
		typedAccessors.put(PropertyType.INTEGER, Tools.freezeMap(accessors));

		// Decimal
		accessors = new HashMap<>();
		accessors.put("minValue", new PropertyDefinitionAccessor<>(PropertyDecimalDefinition::getMinValue,
			MutablePropertyDecimalDefinition::setMinValue, PropertyDefinitionEncoder.BIGDECIMAL));
		accessors.put("maxValue", new PropertyDefinitionAccessor<>(PropertyDecimalDefinition::getMaxValue,
			MutablePropertyDecimalDefinition::setMaxValue, PropertyDefinitionEncoder.BIGDECIMAL));
		accessors.put("precision", new PropertyDefinitionAccessor<>(PropertyDecimalDefinition::getPrecision,
			MutablePropertyDecimalDefinition::setPrecision, DecimalPrecision::valueOf));
		typedAccessors.put(PropertyType.DECIMAL, Tools.freezeMap(accessors));

		// DateTime
		accessors = new HashMap<>();
		accessors.put("dateTimeResolution",
			new PropertyDefinitionAccessor<>(PropertyDateTimeDefinition::getDateTimeResolution,
				MutablePropertyDateTimeDefinition::setDateTimeResolution, new EnumCodec<>(DateTimeResolution.class)));
		typedAccessors.put(PropertyType.DATETIME, Tools.freezeMap(accessors));

		// String
		accessors = new HashMap<>();
		accessors.put("maxLength", new PropertyDefinitionAccessor<>(PropertyStringDefinition::getMaxLength,
			MutablePropertyStringDefinition::setMaxLength, PropertyDefinitionEncoder.BIGINTEGER));
		typedAccessors.put(PropertyType.STRING, Tools.freezeMap(accessors));

		TYPED_ACCESSORS = Tools.freezeMap(typedAccessors);
	}

	private static final Map<PropertyType, Codec<Object, String>> VALUE_CODECS;
	static {
		Map<PropertyType, Codec<Object, String>> valueCodecs = new EnumMap<>(PropertyType.class);
		valueCodecs.put(PropertyType.BOOLEAN, PropertyDefinitionEncoder.BOOLEAN_CODEC);
		valueCodecs.put(PropertyType.ID, PropertyDefinitionEncoder.STRING_CODEC);
		valueCodecs.put(PropertyType.INTEGER, PropertyDefinitionEncoder.BIGINTEGER_CODEC);
		valueCodecs.put(PropertyType.DATETIME, PropertyDefinitionEncoder.DATETIME_CODEC);
		valueCodecs.put(PropertyType.DECIMAL, PropertyDefinitionEncoder.BIGDECIMAL_CODEC);
		valueCodecs.put(PropertyType.HTML, PropertyDefinitionEncoder.STRING_CODEC);
		valueCodecs.put(PropertyType.STRING, PropertyDefinitionEncoder.STRING_CODEC);
		valueCodecs.put(PropertyType.URI, PropertyDefinitionEncoder.STRING_CODEC);
		VALUE_CODECS = Tools.freezeMap(valueCodecs);
	}

	public static MutablePropertyDefinition<?> constructDefinition(PropertyType propertyType) {
		MutablePropertyDefinition<?> ret = null;
		switch (Objects.requireNonNull(propertyType, "Must provide the type of property to instantiate")) {
			case BOOLEAN:
				ret = new PropertyBooleanDefinitionImpl();
				break;

			case ID:
				ret = new PropertyIdDefinitionImpl();
				break;

			case INTEGER:
				ret = new PropertyIntegerDefinitionImpl();
				break;

			case DATETIME:
				ret = new PropertyDateTimeDefinitionImpl();
				break;

			case DECIMAL:
				ret = new PropertyDecimalDefinitionImpl();
				break;

			case HTML:
				ret = new PropertyHtmlDefinitionImpl();
				break;

			case STRING:
				ret = new PropertyStringDefinitionImpl();
				break;

			case URI:
				ret = new PropertyUriDefinitionImpl();
				break;

			default:
				break;
		}
		if (ret == null) {
			throw new IllegalArgumentException("The property type " + propertyType.name() + " is not yet supported");
		}
		ret.setPropertyType(propertyType);
		return ret;
	}

	static String encodeProperty(PropertyDefinition<?> property) throws JsonProcessingException {
		// First: the common properties
		Map<String, Object> values = PropertyDefinitionEncoder.encodeCommonValues(property);

		// Next, the default values
		Codec<Object, String> codec = PropertyDefinitionEncoder.VALUE_CODECS.get(property.getPropertyType());

		List<String> defaultValue = new LinkedList<>();
		for (Object o : property.getDefaultValue()) {
			defaultValue.add(codec.encode(o));
		}
		if (!defaultValue.isEmpty()) {
			values.put(PropertyDefinitionEncoder.LBL_DEFAULT_VALUE, defaultValue);
		}

		// Finally, the choices
		List<Object> choices = new LinkedList<>();
		for (Choice<?> c : property.getChoices()) {
			choices.add(PropertyDefinitionEncoder.encodeChoice(codec, c));
		}
		if (!choices.isEmpty()) {
			values.put(PropertyDefinitionEncoder.LBL_CHOICE, choices);
		}
		return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(values);
	}

	static Map<String, Object> encodeCommonValues(PropertyDefinition<?> property) {
		Map<String, Object> values = new LinkedHashMap<>();
		for (String name : PropertyDefinitionEncoder.COMMON_ACCESSORS.keySet()) {
			String value = PropertyDefinitionEncoder.COMMON_ACCESSORS.get(name).get(property);
			if (value != null) {
				values.put(name, value);
			}
		}

		Map<String, PropertyDefinitionAccessor<?, ?, ?>> typedAccessors = PropertyDefinitionEncoder.TYPED_ACCESSORS
			.get(property.getPropertyType());
		// Then, the ones specifically for each type
		if (typedAccessors != null) {
			for (String name : typedAccessors.keySet()) {
				String value = typedAccessors.get(name).get(property);
				if (value != null) {
					values.put(name, value);
				}
			}
		}
		return values;
	}

	static Object encodeChoice(Codec<Object, String> codec, Choice<?> choice) {
		if (choice == null) { return null; }

		Map<String, Object> encodedChoice = new LinkedHashMap<>();

		encodedChoice.put(PropertyDefinitionEncoder.LBL_CHOICE_NAME, choice.getDisplayName());

		// Does this choice have values associated?
		List<String> values = new LinkedList<>();
		for (Object v : choice.getValue()) {
			values.add(codec.encode(v));
		}
		encodedChoice.put(PropertyDefinitionEncoder.LBL_CHOICE_VALUE, values);

		// Does this choice have hierarchical children?
		List<Object> children = new LinkedList<>();
		// There are hierarchical children - encode each one
		for (Choice<?> c : choice.getChoice()) {
			children.add(PropertyDefinitionEncoder.encodeChoice(codec, c));
		}

		// Stow the children...
		encodedChoice.put(PropertyDefinitionEncoder.LBL_CHOICE, children);

		return encodedChoice;
	}

	static <V> PropertyDefinition<V> decodeProperty(String json) throws JsonProcessingException {
		// First: the common properties
		Map<?, ?> values = new ObjectMapper().readValue(json, Map.class);

		Pair<Codec<V, String>, MutablePropertyDefinition<V>> newDef = PropertyDefinitionEncoder
			.decodeCommonValues(values);

		final Codec<V, String> codec = newDef.getKey();
		final MutablePropertyDefinition<V> property = newDef.getValue();

		// Next, the default values
		Object dv = values.get(PropertyDefinitionEncoder.LBL_DEFAULT_VALUE);
		if (dv != null) {
			List<V> defaultValue = new ArrayList<>();
			List<?> defaults = List.class.cast(dv);
			if ((defaults != null) && !defaults.isEmpty()) {
				for (Object v : defaults) {
					defaultValue.add(codec.decode(Tools.toString(v)));
				}
			}
			property.setDefaultValue(defaultValue);
		}

		Object c = values.get(PropertyDefinitionEncoder.LBL_CHOICE);
		if (c != null) {
			@SuppressWarnings("unchecked")
			List<Object> choicesRoot = (List<Object>) c;
			List<Choice<V>> choices = new ArrayList<>(choicesRoot.size());
			for (Object choiceObj : choicesRoot) {
				Choice<V> choice = PropertyDefinitionEncoder.decodeChoice(codec, choiceObj);
				if (choice != null) {
					choices.add(choice);
				}
			}
			if (!choices.isEmpty()) {
				property.setChoices(choices);
			}
		}

		return property;
	}

	static <V> Pair<Codec<V, String>, MutablePropertyDefinition<V>> decodeCommonValues(Map<?, ?> values) {
		// First things first: get the type
		Object type = values.get(PropertyDefinitionEncoder.LBL_PROPERTY_TYPE);
		if (type == null) {
			throw new IllegalArgumentException("The given JSON doesn't contain a propertyType attribute");
		}

		final PropertyType propertyType = PropertyType.valueOf(type.toString());

		// This is less than ideal, but it's the simplest way to make things work
		// we just need to be careful to never let this code fall out of sync
		// with what CMIS implements

		@SuppressWarnings("unchecked")
		final Codec<V, String> codec = (Codec<V, String>) PropertyDefinitionEncoder.VALUE_CODECS.get(propertyType);

		@SuppressWarnings("unchecked")
		final MutablePropertyDefinition<V> property = (MutablePropertyDefinition<V>) PropertyDefinitionEncoder
			.constructDefinition(propertyType);

		for (String name : PropertyDefinitionEncoder.COMMON_ACCESSORS.keySet()) {
			Object v = values.get(name);
			if (v != null) {
				PropertyDefinitionEncoder.COMMON_ACCESSORS.get(name).set(property, v.toString());
			}
		}

		Map<String, PropertyDefinitionAccessor<?, ?, ?>> typedAccessors = PropertyDefinitionEncoder.TYPED_ACCESSORS
			.get(property.getPropertyType());
		// Then, the ones specifically for each type
		if (typedAccessors != null) {
			for (String name : typedAccessors.keySet()) {
				Object v = values.get(name);
				if (v != null) {
					typedAccessors.get(name).set(property, v.toString());
				}
			}
		}

		return Pair.of(codec, property);
	}

	static <V> Choice<V> decodeChoice(Codec<V, String> codec, Object choiceObj) {
		if (choiceObj == null) { return null; }

		@SuppressWarnings("unchecked")
		Map<String, Object> choiceMap = (Map<String, Object>) choiceObj;

		ChoiceImpl<V> choice = new ChoiceImpl<>();
		choice.setDisplayName(Tools.toString(choiceMap.get(PropertyDefinitionEncoder.LBL_CHOICE_NAME)));

		Object valueObj = choiceMap.get(PropertyDefinitionEncoder.LBL_CHOICE_VALUE);
		if (valueObj != null) {
			List<V> value = new ArrayList<>();
			for (Object o : List.class.cast(valueObj)) {
				V v = codec.decode(Tools.toString(o));
				if (v != null) {
					value.add(v);
				}
			}
			if (!value.isEmpty()) {
				choice.setValue(value);
			}
		}

		Object c = choiceMap.get(PropertyDefinitionEncoder.LBL_CHOICE);
		if (c != null) {
			@SuppressWarnings("unchecked")
			List<Object> childrenList = (List<Object>) c;
			List<Choice<V>> children = new ArrayList<>(childrenList.size());
			for (Object childObj : childrenList) {
				Choice<V> child = PropertyDefinitionEncoder.decodeChoice(codec, childObj);
				if (child != null) {
					children.add(child);
				}
			}
			if (!children.isEmpty()) {
				choice.setChoice(children);
			}
		}

		return choice;
	}

	public static <T> void encode(ObjectType type, CmfObject<T> object, Function<String, T> encoder)
		throws ExportException {
		PropertyDefinitionEncoder.encode(type.getPropertyDefinitions(), object, encoder);
	}

	public static <T> void encode(Map<String, PropertyDefinition<?>> m, CmfObject<T> object,
		Function<String, T> encoder) throws ExportException {
		object.setProperty(PropertyDefinitionEncoder.encode(m, encoder));
	}

	public static <T> CmfProperty<T> encode(ObjectType type, Function<String, T> encoder) throws ExportException {
		return PropertyDefinitionEncoder.encode(type.getPropertyDefinitions(), encoder);
	}

	public static <T> CmfProperty<T> encode(Map<String, PropertyDefinition<?>> m, Function<String, T> encoder)
		throws ExportException {
		CmfProperty<T> property = new CmfProperty<>(IntermediateProperty.PROPERTY_DEFINITIONS, CmfValue.Type.STRING,
			true);

		for (PropertyDefinition<?> p : m.values()) {
			try {
				property.addValue(encoder.apply(PropertyDefinitionEncoder.encodeProperty(p)));
			} catch (JsonProcessingException e) {
				throw new ExportException("Failed to encode the property: " + p, e);
			}
		}

		return property;
	}

	public static <T> Map<String, PropertyDefinition<?>> decode(CmfProperty<T> property, Function<T, String> decoder)
		throws ImportException {
		Map<String, PropertyDefinition<?>> map = new LinkedHashMap<>();
		if ((property != null) && property.hasValues()) {
			for (T t : property) {
				try {
					PropertyDefinition<?> p = PropertyDefinitionEncoder.decodeProperty(decoder.apply(t));
					map.put(p.getId(), p);
				} catch (JsonProcessingException e) {
					throw new ImportException("Failed to decode the property: " + t, e);
				}
			}
		}
		return map;
	}

	public static <T> Map<String, PropertyDefinition<?>> decode(CmfObject<T> object, Function<T, String> decoder)
		throws ImportException {
		return PropertyDefinitionEncoder.decode(object.getProperty(IntermediateProperty.PROPERTY_DEFINITIONS), decoder);
	}
}