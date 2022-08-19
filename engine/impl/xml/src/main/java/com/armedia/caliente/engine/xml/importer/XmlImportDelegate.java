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
package com.armedia.caliente.engine.xml.importer;

import java.io.File;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;

import com.armedia.caliente.engine.importer.ImportDelegate;
import com.armedia.caliente.engine.xml.common.XmlRoot;
import com.armedia.caliente.engine.xml.common.XmlSessionWrapper;
import com.armedia.caliente.engine.xml.importer.jaxb.AttributeT;
import com.armedia.caliente.engine.xml.importer.jaxb.DataTypeT;
import com.armedia.caliente.engine.xml.importer.jaxb.PropertyT;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

public abstract class XmlImportDelegate extends
	ImportDelegate<File, XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportDelegateFactory, XmlImportEngine> {

	protected static final ZoneOffset REFERENCE_TZ = ZoneOffset.UTC;
	protected static final TimeZone TZUTC = TimeZone.getTimeZone(XmlImportDelegate.REFERENCE_TZ);
	protected static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME //
		.withZone(XmlImportDelegate.REFERENCE_TZ) //
	;

	protected final void dumpAttributes(List<AttributeT> list) {
		for (String name : this.cmfObject.getAttributeNames()) {
			final AttributeT attribute = new AttributeT();
			final CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(name);

			attribute.setName(name);
			attribute.setDataType(DataTypeT.convert(att.getType()));
			if (att.getType() == CmfValue.Type.DATETIME) {
				// Dump it out in XML format
				for (CmfValue v : att) {
					String V = "";
					if (!v.isNull()) {
						try {
							V = XmlImportDelegate.UTC_FORMAT.format(v.asTime().toInstant());
						} catch (Exception e) {
							throw new RuntimeException("Failed to produce a date value", e);
						}
					}

					attribute.getValue().add(V);
				}
			} else {
				for (CmfValue v : att) {
					attribute.getValue().add(v.asString());
				}
			}
			list.add(attribute);
		}
	}

	protected final String renderAttributeName(String name) {
		return this.factory.renderAttributeName(name);
	}

	protected final void dumpProperties(List<PropertyT> list) {
		for (String name : this.cmfObject.getPropertyNames()) {
			final PropertyT property = new PropertyT();
			final CmfProperty<CmfValue> prop = this.cmfObject.getProperty(name);

			property.setName(name);
			property.setDataType(DataTypeT.convert(prop.getType()));
			property.setRepeating(prop.isMultivalued());
			if (prop.getType() == CmfValue.Type.DATETIME) {
				// Dump it out in XML format
				for (CmfValue v : prop) {
					String V = "";
					if (!v.isNull()) {
						try {
							V = XmlImportDelegate.UTC_FORMAT.format(v.asTime().toInstant());
						} catch (Exception e) {
							throw new RuntimeException("Failed to produce a date value", e);
						}
					}
					property.getValue().add(V);
				}
			} else {
				for (CmfValue v : prop) {
					property.getValue().add(v.asString());
				}
			}

			list.add(property);
		}
	}

	protected XmlImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, File.class, storedObject);
	}
}