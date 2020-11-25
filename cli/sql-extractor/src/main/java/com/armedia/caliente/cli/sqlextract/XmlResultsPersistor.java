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
package com.armedia.caliente.cli.sqlextract;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.sql.ResultSet;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.function.CheckedLazySupplier;
import com.armedia.commons.utilities.xml.XmlStreamElement;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxOutputFactory;

import javanet.staxutils.IndentingXMLStreamWriter;

public class XmlResultsPersistor extends BaseShareableLockable implements ResultsPersistor {

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
	private XmlStreamElement xmlRoot = null;

	@Override
	public void initialize(final File target, String dql, ResultSet resultSet) throws Exception {
		final File finalTarget = Tools.canonicalize(target);
		try (MutexAutoLock lock = autoMutexLock()) {
			this.out = new BufferedWriter(new FileWriter(finalTarget));

			XMLOutputFactory factory = XmlResultsPersistor.OUTPUT_FACTORY.get();
			XMLStreamWriter writer = factory.createXMLStreamWriter(this.out);
			this.xml = new IndentingXMLStreamWriter(writer) {
				@Override
				public NamespaceContext getNamespaceContext() {
					return XmlProperties.NO_NAMESPACES;
				}
			};
			this.xml.writeStartDocument(Charset.defaultCharset().name(), "1.1");
			this.xmlRoot = new XmlStreamElement(this.xml, "extraction");

			try (XmlStreamElement xmlDql = this.xmlRoot.newElement("dql")) {
				this.xml.writeCData(dql);
			}

			/*-
			try (XmlStreamElement xmlAttributes = this.xmlRoot.newElement("attributes")) {
				int pos = 0;
				for (IDfAttr attr : this.attributes) {
					this.attributes.put(attr.getName(), attr);
			
					final String rawName = attr.getName();
					final String attrName = sanitizeAttributeName(++pos, rawName);
			
					try (XmlStreamElement xmlAtt = xmlAttributes.newElement(attrName)) {
						if (!StringUtils.equals(rawName, attrName)) {
							this.sanitized.put(rawName, attrName);
							this.xml.writeComment(" " + rawName + " ");
						}
			
						try (XmlStreamElement xmlId = xmlAttributes.newElement("id")) {
							this.xml.writeCharacters(attr.getId());
						}
			
						try (XmlStreamElement xmlType = xmlAttributes.newElement("type")) {
							this.xml.writeAttribute("code", String.valueOf(attr.getDataType()));
							Pair<String, ?> p = XmlResultsPersistor.TYPES.get(attr.getDataType());
							this.xml.writeCharacters(p != null ? p.getLeft() : XmlResultsPersistor.DM_UNKNOWN);
						}
			
						try (XmlStreamElement xmlRepeating = xmlAttributes.newElement("repeating")) {
							this.xml.writeCharacters(String.valueOf(attr.isRepeating()));
						}
			
						try (XmlStreamElement xmlQualifiable = xmlAttributes.newElement("qualifiable")) {
							this.xml.writeCharacters(String.valueOf(attr.isQualifiable()));
						}
			
						try (XmlStreamElement xmlLength = xmlAttributes.newElement("length")) {
							this.xml.writeCharacters(String.valueOf(attr.getLength()));
						}
					}
				}
			}
			this.attributes = Tools.freezeMap(this.attributes);
			*/

			this.first = true;
		}
	}

	/*-
	private String render(IDfValue value) {
		Pair<String, CheckedFunction<IDfValue, String, DfException>> p = XmlResultsPersistor.TYPES
			.get(value.getDataType());
		CheckedFunction<IDfValue, String, DfException> f = IDfValue::asString;
		if ((p != null) && (p.getRight() != null)) {
			f = p.getRight();
		}
		return f.apply(value);
	}
	
	private void render(IDfTypedObject object) throws DfException, XMLStreamException {
		final int atts = object.getAttrCount();
	
		try (XmlStreamElement xmlResult = this.xmlRoot.newElement("result")) {
			for (int i = 0; i < atts; i++) {
				IDfAttr attr = object.getAttr(i);
				final String rawName = attr.getName();
				final String attrName = this.sanitized.getOrDefault(rawName, rawName);
				try (XmlStreamElement xmlAttr = xmlResult.newElement(attrName)) {
					if (!StringUtils.equals(rawName, attrName)) {
						this.xml.writeComment(" " + rawName + " ");
					}
					if (!this.attributes.containsKey(rawName)) {
						this.xml.writeComment(" This attribute wasn't included in the main query structure ");
					}
					final int values = object.getValueCount(rawName);
					for (int v = 0; v < values; v++) {
						try (XmlStreamElement xmlValue = xmlAttr.newElement("value")) {
							this.xml.writeCharacters(render(object.getRepeatingValue(rawName, v)));
						}
					}
				}
			}
		}
	}
	*/

	@Override
	public void persist(ResultSet rs) throws Exception {
		if (rs != null) {
			mutexLocked(() -> {
				if (this.first) {
					this.xml.writeStartElement("results");
				} else {
					this.out.write(System.lineSeparator());
					this.out.flush();
				}
				this.first = false;
				// render(rs);
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
			this.xmlRoot.close();
			this.xml.writeEndDocument();
			this.xml.close();
			this.out.flush();
			this.out.close();
		} finally {
			this.xml = null;
			this.out = null;
		}
	}
}