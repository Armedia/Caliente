package com.armedia.caliente.engine.exporter;

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

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.codec.Codec;
import com.armedia.commons.utilities.codec.EnumCodec;
import com.armedia.commons.utilities.codec.StringCodec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class TypeDataCodec<T> implements Codec<List<PropertyDefinition<?>>, List<CmfAttribute<T>>> {

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
		.from(ZonedDateTime.from(TypeDataCodec.FORMATTER.parse(str)));
	private static final Function<Object, String> DATETIME_ENC = (cal) -> TypeDataCodec.FORMATTER
		.format(GregorianCalendar.class.cast(cal).toZonedDateTime());
	private static final Codec<Object, String> DATETIME_CODEC = new StringCodec<>(TypeDataCodec.DATETIME_ENC,
		TypeDataCodec.DATETIME_DEC);

	private static final String LBL_PROPERTY_TYPE = "propertyType";
	private static final String LBL_DEFAULT_VALUE = "defaultValue";
	private static final String LBL_VALUE = "value";
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
			MutablePropertyDefinition::setId, TypeDataCodec.STRING));
		accessors.put("localNamespace", new PropertyDefinitionAccessor<>(PropertyDefinition::getLocalNamespace,
			MutablePropertyDefinition::setLocalNamespace, TypeDataCodec.STRING));
		accessors.put("localName", new PropertyDefinitionAccessor<>(PropertyDefinition::getLocalName,
			MutablePropertyDefinition::setLocalName, TypeDataCodec.STRING));
		accessors.put("queryName", new PropertyDefinitionAccessor<>(PropertyDefinition::getQueryName,
			MutablePropertyDefinition::setQueryName, TypeDataCodec.STRING));
		accessors.put("displayName", new PropertyDefinitionAccessor<>(PropertyDefinition::getDisplayName,
			MutablePropertyDefinition::setDisplayName, TypeDataCodec.STRING));
		accessors.put("description", new PropertyDefinitionAccessor<>(PropertyDefinition::getDescription,
			MutablePropertyDefinition::setDescription, TypeDataCodec.STRING));
		accessors.put("propertyType", new PropertyDefinitionAccessor<>(PropertyDefinition::getPropertyType,
			MutablePropertyDefinition::setPropertyType, new EnumCodec<>(PropertyType.class)));
		accessors.put("cardinality", new PropertyDefinitionAccessor<>(PropertyDefinition::getCardinality,
			MutablePropertyDefinition::setCardinality, new EnumCodec<>(Cardinality.class)));
		accessors.put("updatability", new PropertyDefinitionAccessor<>(PropertyDefinition::getUpdatability,
			MutablePropertyDefinition::setUpdatability, new EnumCodec<>(Updatability.class)));
		accessors.put("inherited", new PropertyDefinitionAccessor<>(PropertyDefinition::isInherited,
			MutablePropertyDefinition::setIsInherited, TypeDataCodec.BOOLEAN));
		accessors.put("required", new PropertyDefinitionAccessor<>(PropertyDefinition::isRequired,
			MutablePropertyDefinition::setIsRequired, TypeDataCodec.BOOLEAN));
		accessors.put("queryable", new PropertyDefinitionAccessor<>(PropertyDefinition::isQueryable,
			MutablePropertyDefinition::setIsQueryable, TypeDataCodec.BOOLEAN));
		accessors.put("orderable", new PropertyDefinitionAccessor<>(PropertyDefinition::isOrderable,
			MutablePropertyDefinition::setIsOrderable, TypeDataCodec.BOOLEAN));
		accessors.put("openChoice", new PropertyDefinitionAccessor<>(PropertyDefinition::isOpenChoice,
			MutablePropertyDefinition::setIsOpenChoice, TypeDataCodec.BOOLEAN));
		COMMON_ACCESSORS = Tools.freezeMap(accessors);

		Map<PropertyType, Map<String, PropertyDefinitionAccessor<?, ?, ?>>> typedAccessors = new EnumMap<>(
			PropertyType.class);

		// Integer
		accessors = new HashMap<>();
		accessors.put("minValue", new PropertyDefinitionAccessor<>(PropertyIntegerDefinition::getMinValue,
			MutablePropertyIntegerDefinition::setMinValue, TypeDataCodec.BIGINTEGER));
		accessors.put("maxValue", new PropertyDefinitionAccessor<>(PropertyIntegerDefinition::getMaxValue,
			MutablePropertyIntegerDefinition::setMaxValue, TypeDataCodec.BIGINTEGER));
		typedAccessors.put(PropertyType.INTEGER, Tools.freezeMap(accessors));

		// Decimal
		accessors = new HashMap<>();
		accessors.put("minValue", new PropertyDefinitionAccessor<>(PropertyDecimalDefinition::getMinValue,
			MutablePropertyDecimalDefinition::setMinValue, TypeDataCodec.BIGDECIMAL));
		accessors.put("maxValue", new PropertyDefinitionAccessor<>(PropertyDecimalDefinition::getMaxValue,
			MutablePropertyDecimalDefinition::setMaxValue, TypeDataCodec.BIGDECIMAL));
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
			MutablePropertyStringDefinition::setMaxLength, TypeDataCodec.BIGINTEGER));
		typedAccessors.put(PropertyType.STRING, Tools.freezeMap(accessors));

		TYPED_ACCESSORS = Tools.freezeMap(typedAccessors);
	}

	private static final Map<PropertyType, Codec<Object, String>> VALUE_CODECS;
	static {
		Map<PropertyType, Codec<Object, String>> valueCodecs = new EnumMap<>(PropertyType.class);
		valueCodecs.put(PropertyType.BOOLEAN, TypeDataCodec.BOOLEAN_CODEC);
		valueCodecs.put(PropertyType.ID, TypeDataCodec.STRING_CODEC);
		valueCodecs.put(PropertyType.INTEGER, TypeDataCodec.BIGINTEGER_CODEC);
		valueCodecs.put(PropertyType.DATETIME, TypeDataCodec.DATETIME_CODEC);
		valueCodecs.put(PropertyType.DECIMAL, TypeDataCodec.BIGDECIMAL_CODEC);
		valueCodecs.put(PropertyType.HTML, TypeDataCodec.STRING_CODEC);
		valueCodecs.put(PropertyType.STRING, TypeDataCodec.STRING_CODEC);
		valueCodecs.put(PropertyType.URI, TypeDataCodec.STRING_CODEC);
		VALUE_CODECS = Tools.freezeMap(valueCodecs);
	}

	protected static MutablePropertyDefinition<?> constructDefinition(PropertyType propertyType) {
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

	protected static String encodeProperty(PropertyDefinition<?> property) throws JsonProcessingException {
		// First: the common properties
		Map<String, Object> values = TypeDataCodec.encodeCommonValues(property);

		// Next, the default values
		Codec<Object, String> codec = TypeDataCodec.VALUE_CODECS.get(property.getPropertyType());

		List<String> defaultValue = new LinkedList<>();
		for (Object o : property.getDefaultValue()) {
			defaultValue.add(codec.encode(o));
		}
		if (!defaultValue.isEmpty()) {
			values.put(TypeDataCodec.LBL_DEFAULT_VALUE, defaultValue);
		}

		// Finally, the choices
		Map<String, Map<String, Object>> choices = new HashMap<>();
		for (Choice<?> c : property.getChoices()) {
			choices.put(c.getDisplayName(), TypeDataCodec.encodeChoice(codec, c));
		}
		if (!choices.isEmpty()) {
			values.put(TypeDataCodec.LBL_CHOICE, choices);
		}
		return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(values);
	}

	protected static Map<String, Object> encodeCommonValues(PropertyDefinition<?> property) {
		Map<String, Object> values = new LinkedHashMap<>();
		for (String name : TypeDataCodec.COMMON_ACCESSORS.keySet()) {
			String value = TypeDataCodec.COMMON_ACCESSORS.get(name).get(property);
			if (value != null) {
				values.put(name, value);
			}
		}

		Map<String, PropertyDefinitionAccessor<?, ?, ?>> typedAccessors = TypeDataCodec.TYPED_ACCESSORS
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

	protected static <T> Map<String, Object> encodeChoice(Codec<Object, String> codec, Choice<T> choice) {
		if (choice == null) { return null; }

		Map<String, Object> encodedChoice = new LinkedHashMap<>();

		// Does this choice have values associated?
		List<String> values = new LinkedList<>();
		for (T v : choice.getValue()) {
			values.add(codec.encode(v));
		}
		encodedChoice.put(TypeDataCodec.LBL_VALUE, values);

		// Does this choice have hierarchical children?
		Map<String, Object> children = new HashMap<>();
		List<Choice<T>> choices = choice.getChoice();
		// There are hierarchical children - encode each one
		for (Choice<T> c : choices) {
			children.put(c.getDisplayName(), TypeDataCodec.encodeChoice(codec, c));
		}

		// Stow the children...
		encodedChoice.put(TypeDataCodec.LBL_CHOICE, children);

		return encodedChoice;
	}

	protected static <V> PropertyDefinition<V> decodeProperty(String json) throws JsonProcessingException {
		// First: the common properties
		Map<?, ?> values = new ObjectMapper().readValue(json, Map.class);

		Pair<Codec<V, String>, MutablePropertyDefinition<V>> newDef = TypeDataCodec.decodeCommonValues(values);

		final Codec<V, String> codec = newDef.getKey();
		final MutablePropertyDefinition<V> property = newDef.getValue();

		// Next, the default values
		Object dv = values.get(TypeDataCodec.LBL_DEFAULT_VALUE);
		if (dv != null) {
			List<V> defaultValue = new LinkedList<>();
			List<?> defaults = List.class.cast(dv);
			if ((defaults != null) && !defaults.isEmpty()) {
				for (Object v : defaults) {
					defaultValue.add(codec.decode(Tools.toString(v)));
				}
			}
			property.setDefaultValue(defaultValue);
		}

		Object c = values.get(TypeDataCodec.LBL_CHOICE);
		if (c != null) {
			@SuppressWarnings("unchecked")
			Map<String, Object> choicesRoot = (Map<String, Object>) c;

			List<Choice<V>> choices = new ArrayList<>(choicesRoot.size());
			for (String displayName : choicesRoot.keySet()) {
				Choice<V> choice = TypeDataCodec.decodeChoice(codec, displayName, choicesRoot.get(displayName));
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

	protected static <V> Pair<Codec<V, String>, MutablePropertyDefinition<V>> decodeCommonValues(Map<?, ?> values) {
		// First things first: get the type
		Object type = values.get(TypeDataCodec.LBL_PROPERTY_TYPE);
		if (type == null) {
			throw new IllegalArgumentException("The given JSON doesn't contain a propertyType attribute");
		}

		final PropertyType propertyType = PropertyType.valueOf(type.toString());

		// This is less than ideal, but it's the simplest way to make things work
		// we just need to be careful to never let this code fall out of sync
		// with what CMIS implements

		@SuppressWarnings("unchecked")
		final Codec<V, String> codec = (Codec<V, String>) TypeDataCodec.VALUE_CODECS.get(propertyType);

		@SuppressWarnings("unchecked")
		final MutablePropertyDefinition<V> property = (MutablePropertyDefinition<V>) TypeDataCodec
			.constructDefinition(propertyType);

		for (String name : TypeDataCodec.COMMON_ACCESSORS.keySet()) {
			Object v = values.get(name);
			if (v != null) {
				TypeDataCodec.COMMON_ACCESSORS.get(name).set(property, v.toString());
			}
		}

		Map<String, PropertyDefinitionAccessor<?, ?, ?>> typedAccessors = TypeDataCodec.TYPED_ACCESSORS
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

	protected static <V> Choice<V> decodeChoice(Codec<V, String> codec, String name, Object choiceObj) {
		if (choiceObj == null) { return null; }

		@SuppressWarnings("unchecked")
		Map<String, Object> choiceMap = (Map<String, Object>) choiceObj;

		ChoiceImpl<V> choice = new ChoiceImpl<>();
		choice.setDisplayName(name);

		Object valueObj = choiceMap.get(TypeDataCodec.LBL_VALUE);
		if (valueObj != null) {
			List<V> value = new LinkedList<>();
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

		Object c = choiceMap.get(TypeDataCodec.LBL_CHOICE);
		if (c != null) {
			@SuppressWarnings("unchecked")
			Map<String, Object> childrenMap = (Map<String, Object>) c;
			List<Choice<V>> children = new ArrayList<>(childrenMap.size());
			for (String displayName : childrenMap.keySet()) {
				Choice<V> child = TypeDataCodec.decodeChoice(codec, displayName, childrenMap.get(displayName));
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

	@Override
	public List<CmfAttribute<T>> encode(List<PropertyDefinition<?>> v) {
		return null;
	}

	@Override
	public List<PropertyDefinition<?>> decode(List<CmfAttribute<T>> e) {
		return null;
	}
}