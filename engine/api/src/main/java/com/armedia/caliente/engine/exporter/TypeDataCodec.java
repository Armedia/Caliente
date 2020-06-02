package com.armedia.caliente.engine.exporter;

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
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue.Type;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.codec.Codec;

public class TypeDataCodec<T> implements Codec<List<PropertyDefinition<?>>, List<CmfAttribute<T>>> {

	private static class PropertyReader<T extends PropertyDefinition<?>> {
		private final Class<T> propertyClass;
		private final Function<T, Object> reader;

		private PropertyReader(Function<T, Object> reader) {
			this(null, reader);
		}

		private PropertyReader(Class<T> propertyClass, Function<T, Object> reader) {
			this.propertyClass = propertyClass;
			this.reader = reader;
		}

		public Object readValue(Object def) {
			if (this.propertyClass != null) {
				if (!this.propertyClass.isInstance(def)) { return null; }
				return this.reader.apply(this.propertyClass.cast(def));
			}

			if (!PropertyDefinition.class.isInstance(def)) { return null; }

			@SuppressWarnings("unchecked")
			T t = (T) def;
			return this.reader.apply(t);
		}
	}

	private static class PropertyWriter<V, T extends MutablePropertyDefinition<?>> {
		private final Class<T> propertyClass;
		private final BiConsumer<T, V> writer;

		private PropertyWriter(BiConsumer<T, V> writer) {
			this(null, writer);
		}

		private PropertyWriter(Class<T> propertyClass, BiConsumer<T, V> writer) {
			this.propertyClass = propertyClass;
			this.writer = writer;
		}

		public void writeValue(Object def, V value) {
			if (this.propertyClass != null) {
				if (!this.propertyClass.isInstance(def)) { return; }
				this.writer.accept(this.propertyClass.cast(def), value);
			}

			if (!MutablePropertyDefinition.class.isInstance(def)) { return; }

			@SuppressWarnings("unchecked")
			T t = (T) def;
			this.writer.accept(t, value);
		}
	}

	private static final Map<String, PropertyReader<?>> COMMON_READERS;
	private static final Map<String, PropertyWriter<?, ?>> COMMON_WRITERS;
	private static final Map<PropertyType, Map<String, PropertyReader<?>>> TYPED_READERS;
	private static final Map<PropertyType, Map<String, PropertyWriter<?, ?>>> TYPED_WRITERS;
	static {
		Map<String, PropertyReader<?>> readers = new HashMap<>();
		Map<String, PropertyWriter<?, ?>> writers = new HashMap<>();
		readers.put("id", new PropertyReader<>(PropertyDefinition::getId));
		writers.put("id", new PropertyWriter<String, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setId));
		readers.put("localNamespace", new PropertyReader<>(PropertyDefinition::getLocalNamespace));
		writers.put("localNamespace",
			new PropertyWriter<String, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setLocalNamespace));
		readers.put("localName", new PropertyReader<>(PropertyDefinition::getLocalName));
		writers.put("localName",
			new PropertyWriter<String, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setLocalName));
		readers.put("queryName", new PropertyReader<>(PropertyDefinition::getQueryName));
		writers.put("queryName",
			new PropertyWriter<String, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setQueryName));
		readers.put("displayName", new PropertyReader<>(PropertyDefinition::getDisplayName));
		writers.put("displayName",
			new PropertyWriter<String, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setDisplayName));
		readers.put("description", new PropertyReader<>(PropertyDefinition::getDescription));
		writers.put("description",
			new PropertyWriter<String, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setDescription));
		readers.put("propertyType", new PropertyReader<>(PropertyDefinition::getPropertyType));
		writers.put("propertyType",
			new PropertyWriter<PropertyType, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setPropertyType));
		readers.put("cardinality", new PropertyReader<>(PropertyDefinition::getCardinality));
		writers.put("cardinality",
			new PropertyWriter<Cardinality, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setCardinality));
		readers.put("updatability", new PropertyReader<>(PropertyDefinition::getUpdatability));
		writers.put("updatability",
			new PropertyWriter<Updatability, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setUpdatability));
		readers.put("inherited", new PropertyReader<>(PropertyDefinition::isInherited));
		writers.put("inherited",
			new PropertyWriter<Boolean, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setIsInherited));
		readers.put("required", new PropertyReader<>(PropertyDefinition::isRequired));
		writers.put("required",
			new PropertyWriter<Boolean, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setIsRequired));
		readers.put("queryable", new PropertyReader<>(PropertyDefinition::isQueryable));
		writers.put("queryable",
			new PropertyWriter<Boolean, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setIsQueryable));
		readers.put("orderable", new PropertyReader<>(PropertyDefinition::isOrderable));
		writers.put("orderable",
			new PropertyWriter<Boolean, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setIsOrderable));
		readers.put("openChoice", new PropertyReader<>(PropertyDefinition::isOpenChoice));
		writers.put("openChoice",
			new PropertyWriter<Boolean, MutablePropertyDefinition<?>>(MutablePropertyDefinition::setIsOpenChoice));
		COMMON_READERS = Tools.freezeMap(readers);
		COMMON_WRITERS = Tools.freezeMap(writers);

		Map<PropertyType, Map<String, PropertyReader<?>>> typedReaders = new EnumMap<>(PropertyType.class);
		Map<PropertyType, Map<String, PropertyWriter<?, ?>>> typedWriters = new EnumMap<>(PropertyType.class);

		// Integer
		readers = new HashMap<>();
		writers = new HashMap<>();
		readers.put("minValue",
			new PropertyReader<>(PropertyIntegerDefinition.class, PropertyIntegerDefinition::getMinValue));
		writers.put("minValue", new PropertyWriter<>(MutablePropertyIntegerDefinition.class,
			MutablePropertyIntegerDefinition::setMinValue));
		readers.put("maxValue",
			new PropertyReader<>(PropertyIntegerDefinition.class, PropertyIntegerDefinition::getMaxValue));
		writers.put("maxValue", new PropertyWriter<>(MutablePropertyIntegerDefinition.class,
			MutablePropertyIntegerDefinition::setMinValue));
		typedReaders.put(PropertyType.INTEGER, Tools.freezeMap(readers));
		typedWriters.put(PropertyType.INTEGER, Tools.freezeMap(writers));

		// Decimal
		readers = new HashMap<>();
		writers = new HashMap<>();
		readers.put("minValue",
			new PropertyReader<>(PropertyDecimalDefinition.class, PropertyDecimalDefinition::getMinValue));
		writers.put("minValue", new PropertyWriter<>(MutablePropertyDecimalDefinition.class,
			MutablePropertyDecimalDefinition::setMinValue));
		readers.put("maxValue",
			new PropertyReader<>(PropertyDecimalDefinition.class, PropertyDecimalDefinition::getMaxValue));
		writers.put("maxValue", new PropertyWriter<>(MutablePropertyDecimalDefinition.class,
			MutablePropertyDecimalDefinition::setMinValue));
		readers.put("precision",
			new PropertyReader<>(PropertyDecimalDefinition.class, PropertyDecimalDefinition::getPrecision));
		writers.put("precision", new PropertyWriter<>(MutablePropertyDecimalDefinition.class,
			MutablePropertyDecimalDefinition::setPrecision));
		typedReaders.put(PropertyType.DECIMAL, Tools.freezeMap(readers));
		typedWriters.put(PropertyType.DECIMAL, Tools.freezeMap(writers));

		// Date
		readers = new HashMap<>();
		writers = new HashMap<>();
		readers.put("dateTimeResolution",
			new PropertyReader<>(PropertyDateTimeDefinition.class, PropertyDateTimeDefinition::getDateTimeResolution));
		writers.put("dateTimeResolution", new PropertyWriter<>(MutablePropertyDateTimeDefinition.class,
			MutablePropertyDateTimeDefinition::setDateTimeResolution));
		typedReaders.put(PropertyType.DATETIME, Tools.freezeMap(readers));
		typedWriters.put(PropertyType.DATETIME, Tools.freezeMap(writers));

		// String
		readers = new HashMap<>();
		writers = new HashMap<>();
		readers.put("maxLength",
			new PropertyReader<>(PropertyStringDefinition.class, PropertyStringDefinition::getMaxLength));
		writers.put("maxLength",
			new PropertyWriter<>(MutablePropertyStringDefinition.class, MutablePropertyStringDefinition::setMaxLength));
		typedReaders.put(PropertyType.STRING, Tools.freezeMap(readers));
		typedWriters.put(PropertyType.STRING, Tools.freezeMap(writers));

		TYPED_READERS = Tools.freezeMap(typedReaders);
		TYPED_WRITERS = Tools.freezeMap(typedWriters);
	}

	protected static String encodeProperty(PropertyDefinition<?> property) {
		Map<String, String> values = new LinkedHashMap<>();
		for (String name : TypeDataCodec.COMMON_READERS.keySet()) {
			Object o = TypeDataCodec.COMMON_READERS.get(name).readValue(property);
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

		Map<String, PropertyReader<?>> typedReaders = TypeDataCodec.TYPED_READERS.get(property.getPropertyType());
		if (typedReaders != null) {
			for (String name : typedReaders.keySet()) {
				Object o = typedReaders.get(name).readValue(property);
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
		return values;
	}

	protected static <T> CmfProperty<T> encodeProperties(Function<String, T> converter,
		PropertyDefinition<?> property) {

		final CmfProperty<T> attribute = new CmfProperty<>(IntermediateProperty.PROPERTY_DEFINITIONS, Type.STRING,
			true);

		/*-
		"my:stringProperty":{
		    "id":"my:stringProperty",
		    "localNamespace":"local",
		    "localName":"my:stringProperty",
		    "queryName":"my:stringProperty",
		    "displayName":"My String Property",
		    "description":"This is a String.~,
		    "propertyType":"string",
		    "updatability":"readwrite",
		    "inherited":false,
		    "openChoice":false,
		    "required":false,
		    "cardinality":"single",
		    "queryable":true,
		    "orderable":true,
		}
		*/

		Map<String, String> values = new LinkedHashMap<>();
		for (String name : TypeDataCodec.COMMON_READERS.keySet()) {
			Object o = TypeDataCodec.COMMON_READERS.get(name).readValue(property);
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

		Map<String, PropertyReader<?>> typedReaders = TypeDataCodec.TYPED_READERS.get(property.getPropertyType());
		if (typedReaders != null) {
			for (String name : typedReaders.keySet()) {
				Object o = typedReaders.get(name).readValue(property);
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
		return values;
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