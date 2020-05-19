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
package com.armedia.caliente.cli.query;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.function.CheckedLazySupplier;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

import javanet.staxutils.IndentingXMLStreamWriter;

public class XmlResultsPersistor extends BaseShareableLockable implements ResultsPersistor {

	private static final String DM_UNKNOWN = "DM_UNKNOWN";
	private static final Map<Integer, String> TYPES;
	static {
		Map<Integer, String> types = new TreeMap<>();
		types.put(IDfAttr.DM_BOOLEAN, "DM_BOOLEAN");
		types.put(IDfAttr.DM_INTEGER, "DM_INTEGER");
		types.put(IDfAttr.DM_STRING, "DM_STRING");
		types.put(IDfAttr.DM_ID, "DM_ID");
		types.put(IDfAttr.DM_TIME, "DM_TIME");
		types.put(IDfAttr.DM_DOUBLE, "DM_DOUBLE");
		types.put(IDfAttr.DM_UNDEFINED, "DM_UNDEFINED");
		TYPES = Tools.freezeMap(types);
	}

	private static final CheckedLazySupplier<XMLOutputFactory, XMLStreamException> OUTPUT_FACTORY = new CheckedLazySupplier<>(
		() -> {
			WstxOutputFactory factory = new WstxOutputFactory();
			try {
				// This is only supported after 5.0
				Field f = WstxOutputProperties.class.getDeclaredField("P_USE_DOUBLE_QUOTES_IN_XML_DECL");
				if (Modifier.isStatic(f.getModifiers()) && String.class.isAssignableFrom(f.getType())) {
					Object v = f.get(null);
					if (v != null) {
						factory.setProperty(v.toString(), true);
					}
				}
			} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
				// It's ok...we're using an older version, so we simply won't have double quotes on
				// the XML declaration
			}
			factory.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES, true);
			factory.setProperty(WstxOutputProperties.P_ADD_SPACE_AFTER_EMPTY_ELEM, true);
			factory.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_ATTR, true);
			return factory;
		});

	private Writer out = null;
	private XMLStreamWriter xml = null;
	private boolean first = false;
	private Map<String, IDfAttr> attributes = null;

	@Override
	public void initialize(final File target, String dql, Collection<IDfAttr> attributes) throws Exception {
		final File finalTarget = Tools.canonicalize(target);
		try (MutexAutoLock lock = autoMutexLock()) {
			this.out = new BufferedWriter(new FileWriter(finalTarget));
			this.attributes = new LinkedHashMap<>();

			XMLOutputFactory factory = XmlResultsPersistor.OUTPUT_FACTORY.get();
			XMLStreamWriter writer = factory.createXMLStreamWriter(this.out);
			this.xml = new IndentingXMLStreamWriter(writer) {
				@Override
				public NamespaceContext getNamespaceContext() {
					return XmlProperties.NO_NAMESPACES;
				}
			};
			this.xml.writeStartDocument(Charset.defaultCharset().name(), "1.1");
			String rootElement = "query";
			this.xml.writeDTD(String.format("<!DOCTYPE %s>", rootElement));
			this.xml.writeStartElement(rootElement);
			this.xml.flush();

			this.xml.writeStartElement("query");
			this.xml.writeCData(dql);
			this.xml.writeEndElement();

			this.xml.writeStartElement("attributes");
			for (IDfAttr attr : attributes) {
				this.attributes.put(attr.getName(), attr);

				this.xml.writeStartElement(attr.getName());

				this.xml.writeStartElement("id");
				this.xml.writeCharacters(attr.getId());
				this.xml.writeEndElement();

				this.xml.writeStartElement("type");
				this.xml.writeAttribute("code", String.valueOf(attr.getDataType()));
				this.xml.writeCharacters(
					XmlResultsPersistor.TYPES.getOrDefault(attr.getDataType(), XmlResultsPersistor.DM_UNKNOWN));
				this.xml.writeEndElement();

				this.xml.writeStartElement("repeating");
				this.xml.writeCharacters(String.valueOf(attr.isRepeating()));
				this.xml.writeEndElement();

				this.xml.writeStartElement("qualifiable");
				this.xml.writeCharacters(String.valueOf(attr.isQualifiable()));
				this.xml.writeEndElement();

				this.xml.writeStartElement("length");
				this.xml.writeCharacters(String.valueOf(attr.getLength()));
				this.xml.writeEndElement();

				this.xml.writeEndElement();
			}
			this.xml.writeEndElement();

			this.attributes = Tools.freezeMap(this.attributes);
			this.first = true;
		}
	}

	private String render(IDfValue value) {
		switch (value.getDataType()) {
			case IDfAttr.DM_BOOLEAN:
				break;

			case IDfAttr.DM_INTEGER:
				break;

			case IDfAttr.DM_STRING:
				break;

			case IDfAttr.DM_ID:
				IDfId id = value.asId();
				return (id.isNull() ? DfId.DF_NULLID_STR : id.toString());

			case IDfAttr.DM_TIME:
				IDfTime time = value.asTime();
				return (time.isNullDate() ? DfTime.DF_NULLDATE_STR
					: DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(time.getDate()));

			case IDfAttr.DM_DOUBLE:
				break;

			default:
				break;
		}
		return value.asString();
	}

	private void render(IDfTypedObject object) throws DfException, XMLStreamException {
		final int atts = object.getAttrCount();

		this.xml.writeStartElement("result");

		for (int i = 0; i < atts; i++) {
			IDfAttr attr = object.getAttr(i);
			final String name = attr.getName();
			this.xml.writeStartElement(name);
			if (!this.attributes.containsKey(name)) {
				this.xml.writeComment("This attribute wasn't included in the main query structure");
			}
			final int values = object.getValueCount(name);
			for (int v = 0; v < values; v++) {
				this.xml.writeStartElement("value");
				this.xml.writeCharacters(render(object.getRepeatingValue(name, v)));
				this.xml.writeEndElement();
			}
		}

		this.xml.writeEndElement();
	}

	@Override
	public void persist(IDfTypedObject object) throws Exception {
		if (object != null) {
			mutexLocked(() -> {
				if (this.first) {
					this.xml.writeStartElement("results");
				} else {
					this.out.write(System.lineSeparator());
					this.out.flush();
				}
				this.first = false;
				render(object);
				this.xml.flush();
			});
		}
	}

	@Override
	public void close() throws Exception {
		try (MutexAutoLock lock = autoMutexLock()) {
			if (!this.first) {
				this.xml.writeEndElement();
			}
			this.xml.flush();
			this.xml.writeEndDocument();
			this.xml.close();
			this.out.flush();
			this.out.close();
		} finally {
			this.attributes = null;
			this.xml = null;
			this.out = null;
		}
	}
}