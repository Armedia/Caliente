package com.armedia.caliente.engine.exporter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.codec.Codec;
import com.armedia.commons.utilities.codec.EnumCodec;
import com.armedia.commons.utilities.codec.StringCodec;

public class TypeDataCodec<T> implements Codec<List<PropertyDefinition<?>>, List<CmfAttribute<T>>> {

	private static final Function<String, String> STRING = Function.identity();
	private static final Function<String, Boolean> BOOLEAN = Boolean::valueOf;
	private static final Function<String, BigInteger> BIGINTEGER = BigInteger::new;
	private static final Function<String, BigDecimal> BIGDECIMAL = BigDecimal::new;

	private static class PropertyAccessor<V, R extends PropertyDefinition<?>, W extends MutablePropertyDefinition<?>> {
		private final Function<R, V> reader;
		private final BiConsumer<W, V> writer;
		private final Codec<V, String> codec;

		private PropertyAccessor(Function<R, V> reader, BiConsumer<W, V> writer, Function<String, V> decoder) {
			this.reader = reader;
			this.writer = writer;
			this.codec = new StringCodec<>(decoder);
		}

		private PropertyAccessor(Function<R, V> reader, BiConsumer<W, V> writer, Codec<V, String> codec) {
			this.reader = reader;
			this.writer = writer;
			this.codec = codec;
		}

		public String readValue(PropertyDefinition<?> def) {
			if (this.reader == null) { return null; }

			// I don't like this, but this is the easiest way to get it done
			// Adding the classes for stricter type validation adds unnecessary overhead

			@SuppressWarnings("unchecked")
			R r = (R) def;

			return this.codec.encode(this.reader.apply(r));
		}

		public void writeValue(Object def, String value) {
			if (def == null) { return; }
			if (!MutablePropertyDecimalDefinition.class.isInstance(def)) { return; }

			// I don't like this, but this is the easiest way to get it done
			// Adding the classes for stricter type validation adds unnecessary overhead

			@SuppressWarnings("unchecked")
			W w = (W) def;

			this.writer.accept(w, this.codec.decode(value));
		}
	}

	private static final Map<String, PropertyAccessor<?, ?, ?>> COMMON_ACCESSORS;
	private static final Map<PropertyType, Map<String, PropertyAccessor<?, ?, ?>>> TYPED_ACCESSORS;
	static {
		Map<String, PropertyAccessor<?, ?, ?>> accessors = new HashMap<>();

		accessors.put("id",
			new PropertyAccessor<>(PropertyDefinition::getId, MutablePropertyDefinition::setId, TypeDataCodec.STRING));
		accessors.put("localNamespace", new PropertyAccessor<>(PropertyDefinition::getLocalNamespace,
			MutablePropertyDefinition::setLocalNamespace, TypeDataCodec.STRING));
		accessors.put("localName", new PropertyAccessor<>(PropertyDefinition::getLocalName,
			MutablePropertyDefinition::setLocalName, TypeDataCodec.STRING));
		accessors.put("queryName", new PropertyAccessor<>(PropertyDefinition::getQueryName,
			MutablePropertyDefinition::setQueryName, TypeDataCodec.STRING));
		accessors.put("displayName", new PropertyAccessor<>(PropertyDefinition::getDisplayName,
			MutablePropertyDefinition::setDisplayName, TypeDataCodec.STRING));
		accessors.put("description", new PropertyAccessor<>(PropertyDefinition::getDescription,
			MutablePropertyDefinition::setDescription, TypeDataCodec.STRING));
		accessors.put("propertyType", new PropertyAccessor<>(PropertyDefinition::getPropertyType,
			MutablePropertyDefinition::setPropertyType, new EnumCodec<>(PropertyType.class)));
		accessors.put("cardinality", new PropertyAccessor<>(PropertyDefinition::getCardinality,
			MutablePropertyDefinition::setCardinality, new EnumCodec<>(Cardinality.class)));
		accessors.put("updatability", new PropertyAccessor<>(PropertyDefinition::getUpdatability,
			MutablePropertyDefinition::setUpdatability, new EnumCodec<>(Updatability.class)));
		accessors.put("inherited", new PropertyAccessor<>(PropertyDefinition::isInherited,
			MutablePropertyDefinition::setIsInherited, TypeDataCodec.BOOLEAN));
		accessors.put("required", new PropertyAccessor<>(PropertyDefinition::isRequired,
			MutablePropertyDefinition::setIsRequired, TypeDataCodec.BOOLEAN));
		accessors.put("queryable", new PropertyAccessor<>(PropertyDefinition::isQueryable,
			MutablePropertyDefinition::setIsQueryable, TypeDataCodec.BOOLEAN));
		accessors.put("orderable", new PropertyAccessor<>(PropertyDefinition::isOrderable,
			MutablePropertyDefinition::setIsOrderable, TypeDataCodec.BOOLEAN));
		accessors.put("openChoice", new PropertyAccessor<>(PropertyDefinition::isOpenChoice,
			MutablePropertyDefinition::setIsOpenChoice, TypeDataCodec.BOOLEAN));
		COMMON_ACCESSORS = Tools.freezeMap(accessors);

		Map<PropertyType, Map<String, PropertyAccessor<?, ?, ?>>> typedAccessors = new EnumMap<>(PropertyType.class);

		// Integer
		accessors = new HashMap<>();
		accessors.put("minValue", new PropertyAccessor<>(PropertyIntegerDefinition::getMinValue,
			MutablePropertyIntegerDefinition::setMinValue, TypeDataCodec.BIGINTEGER));
		accessors.put("minValue", new PropertyAccessor<>(PropertyIntegerDefinition::getMaxValue,
			MutablePropertyIntegerDefinition::setMaxValue, TypeDataCodec.BIGINTEGER));
		typedAccessors.put(PropertyType.INTEGER, Tools.freezeMap(accessors));

		// Decimal
		accessors = new HashMap<>();
		accessors.put("minValue", new PropertyAccessor<>(PropertyDecimalDefinition::getMinValue,
			MutablePropertyDecimalDefinition::setMinValue, TypeDataCodec.BIGDECIMAL));
		accessors.put("maxValue", new PropertyAccessor<>(PropertyDecimalDefinition::getMaxValue,
			MutablePropertyDecimalDefinition::setMaxValue, TypeDataCodec.BIGDECIMAL));
		accessors.put("precision", new PropertyAccessor<>(PropertyDecimalDefinition::getPrecision,
			MutablePropertyDecimalDefinition::setPrecision, DecimalPrecision::valueOf));
		typedAccessors.put(PropertyType.DECIMAL, Tools.freezeMap(accessors));

		// DateTime
		accessors = new HashMap<>();
		accessors.put("dateTimeResolution", new PropertyAccessor<>(PropertyDateTimeDefinition::getDateTimeResolution,
			MutablePropertyDateTimeDefinition::setDateTimeResolution, new EnumCodec<>(DateTimeResolution.class)));
		typedAccessors.put(PropertyType.DATETIME, Tools.freezeMap(accessors));

		// String
		accessors = new HashMap<>();
		accessors.put("maxLength", new PropertyAccessor<>(PropertyStringDefinition::getMaxLength,
			MutablePropertyStringDefinition::setMaxLength, TypeDataCodec.BIGINTEGER));
		typedAccessors.put(PropertyType.STRING, Tools.freezeMap(accessors));

		TYPED_ACCESSORS = Tools.freezeMap(typedAccessors);
	}

	protected static String encodeProperty(PropertyDefinition<?> property) {
		Map<String, Object> values = new LinkedHashMap<>();
		for (String name : TypeDataCodec.COMMON_ACCESSORS.keySet()) {
			Object o = TypeDataCodec.COMMON_ACCESSORS.get(name).readValue(property);
			if (o == null) {
				continue;
			}

			if (o.getClass().isEnum()) {
				o = Enum.class.cast(o).name();
			} else {
				o = Tools.toString(o);
			}

			values.put(name, o.toString());
		}

		Map<String, PropertyAccessor<?, ?, ?>> typedAccessors = TypeDataCodec.TYPED_ACCESSORS
			.get(property.getPropertyType());
		if (typedAccessors != null) {
			for (String name : typedAccessors.keySet()) {
				Object o = typedAccessors.get(name).readValue(property);
				if (o == null) {
					continue;
				}

				if (o.getClass().isEnum()) {
					o = Enum.class.cast(o).name();
				} else {
					o = Tools.toString(o);
				}

				values.put(name, o.toString());
			}
		}

		// Now handle the default values and choices

		return null;
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