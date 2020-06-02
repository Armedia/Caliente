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
package com.armedia.caliente.engine.cmis.exporter;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDateTimeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDecimalDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyIntegerDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyStringDefinition;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;

import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class CmisObjectTypeDelegate extends CmisExportDelegate<ObjectType> {

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

	private static final Map<String, PropertyReader<?>> COMMON_READERS;
	private static final Map<PropertyType, Map<String, PropertyReader<?>>> TYPED_READERS;
	static {
		Map<String, PropertyReader<?>> readers = new HashMap<>();
		readers.put("id", new PropertyReader<>(PropertyDefinition::getId));
		readers.put("localNamespace", new PropertyReader<>(PropertyDefinition::getLocalNamespace));
		readers.put("localName", new PropertyReader<>(PropertyDefinition::getLocalName));
		readers.put("queryName", new PropertyReader<>(PropertyDefinition::getQueryName));
		readers.put("displayName", new PropertyReader<>(PropertyDefinition::getDisplayName));
		readers.put("description", new PropertyReader<>(PropertyDefinition::getDescription));
		readers.put("propertyType", new PropertyReader<>(PropertyDefinition::getPropertyType));
		readers.put("cardinality", new PropertyReader<>(PropertyDefinition::getCardinality));
		readers.put("updatability", new PropertyReader<>(PropertyDefinition::getUpdatability));
		readers.put("inherited", new PropertyReader<>(PropertyDefinition::isInherited));
		readers.put("required", new PropertyReader<>(PropertyDefinition::isRequired));
		readers.put("queryable", new PropertyReader<>(PropertyDefinition::isQueryable));
		readers.put("orderable", new PropertyReader<>(PropertyDefinition::isOrderable));
		readers.put("openChoice", new PropertyReader<>(PropertyDefinition::isOpenChoice));
		COMMON_READERS = Tools.freezeMap(readers);

		Map<PropertyType, Map<String, PropertyReader<?>>> typedReaders = new EnumMap<>(PropertyType.class);

		// Integer
		readers = new HashMap<>();
		readers.put("minValue",
			new PropertyReader<>(PropertyIntegerDefinition.class, PropertyIntegerDefinition::getMinValue));
		readers.put("maxValue",
			new PropertyReader<>(PropertyIntegerDefinition.class, PropertyIntegerDefinition::getMaxValue));
		typedReaders.put(PropertyType.INTEGER, Tools.freezeMap(readers));

		// Decimal
		readers = new HashMap<>();
		readers.put("minValue",
			new PropertyReader<>(PropertyDecimalDefinition.class, PropertyDecimalDefinition::getMinValue));
		readers.put("maxValue",
			new PropertyReader<>(PropertyDecimalDefinition.class, PropertyDecimalDefinition::getMaxValue));
		readers.put("precision",
			new PropertyReader<>(PropertyDecimalDefinition.class, PropertyDecimalDefinition::getPrecision));
		typedReaders.put(PropertyType.DECIMAL, Tools.freezeMap(readers));

		// Date
		readers = new HashMap<>();
		readers.put("dateTimeResolution",
			new PropertyReader<>(PropertyDateTimeDefinition.class, PropertyDateTimeDefinition::getDateTimeResolution));
		typedReaders.put(PropertyType.DATETIME, Tools.freezeMap(readers));

		// String
		readers = new HashMap<>();
		readers.put("maxLength",
			new PropertyReader<>(PropertyStringDefinition.class, PropertyStringDefinition::getMaxLength));
		typedReaders.put(PropertyType.STRING, Tools.freezeMap(readers));

		TYPED_READERS = Tools.freezeMap(typedReaders);
	}

	protected static Map<String, String> getDefinitionValues(PropertyDefinition<?> property) {
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
		for (String name : CmisObjectTypeDelegate.COMMON_READERS.keySet()) {
			Object o = CmisObjectTypeDelegate.COMMON_READERS.get(name).readValue(property);
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

		Map<String, PropertyReader<?>> typedReaders = CmisObjectTypeDelegate.TYPED_READERS
			.get(property.getPropertyType());
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

	protected CmisObjectTypeDelegate(CmisExportDelegateFactory factory, Session session, ObjectType objectType)
		throws Exception {
		super(factory, session, ObjectType.class, objectType);
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		// Don't marshal the details for base types?
		if (this.object.isBaseType()) { return true; }

		// Save the property definitions...
		for (PropertyDefinition<?> p : this.object.getPropertyDefinitions().values()) {
			// TODO: How to encode this information in an "engine-neutral" fashion?

			Map<String, String> values = CmisObjectTypeDelegate.getDefinitionValues(p);
			values.size();
			// TODO: Where to stow values?

		}
		return true;
	}

	protected int calculateDepth(ObjectType objectType, final Set<String> visited) throws Exception {
		if (objectType == null) {
			throw new IllegalArgumentException("Must provide a folder whose depth to calculate");
		}
		if (!visited.add(objectType.getId())) {
			throw new IllegalStateException(
				String.format("ObjectType [%s] was visited twice - visited set: %s", objectType.getId(), visited));
		}
		try {
			if (objectType.isBaseType()) { return 0; }
			return calculateDepth(objectType.getParentType(), visited) + 1;
		} finally {
			visited.remove(objectType.getId());
		}
	}

	@Override
	protected int calculateDependencyTier(Session session, ObjectType objectType) throws Exception {
		return calculateDepth(objectType, new LinkedHashSet<String>());
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifyRequirements(marshalled, ctx);
		ObjectType objectType = this.object.getParentType();
		if (objectType != null) {
			ret.add(new CmisObjectTypeDelegate(this.factory, ctx.getSession(), objectType));
		}
		return ret;
	}

	@Override
	protected CmfObject.Archetype calculateType(Session session, ObjectType object) throws Exception {
		return CmfObject.Archetype.TYPE;
	}

	@Override
	protected String calculateLabel(Session session, ObjectType object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateObjectId(Session session, ObjectType object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateSearchKey(Session session, ObjectType object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateName(Session session, ObjectType object) throws Exception {
		return object.getId();
	}
}