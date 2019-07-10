/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.caliente.engine.importer.ImportDelegate;
import com.armedia.caliente.engine.xml.common.XmlRoot;
import com.armedia.caliente.engine.xml.common.XmlSessionWrapper;
import com.armedia.caliente.engine.xml.importer.jaxb.AttributeT;
import com.armedia.caliente.engine.xml.importer.jaxb.DataTypeT;
import com.armedia.caliente.engine.xml.importer.jaxb.PropertyT;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

public abstract class XmlImportDelegate extends
	ImportDelegate<File, XmlRoot, XmlSessionWrapper, CmfValue, XmlImportContext, XmlImportDelegateFactory, XmlImportEngine> {

	protected static final TimeZone TZUTC = TimeZone.getTimeZone(("UTC"));

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
						Calendar gcal = Calendar.getInstance();
						try {
							gcal.setTime(v.asTime());
						} catch (ParseException e) {
							throw new RuntimeException("Failed to produce a date value", e);
						}
						gcal.setTimeZone(XmlImportDelegate.TZUTC);
						V = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(gcal);
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

	protected final CmfValue getAttributeValue(CmfEncodeableName attribute) {
		return getAttributeValue(attribute.encode());
	}

	protected final CmfValue getAttributeValue(String attribute) {
		CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(attribute);
		if (att == null) { return CmfValue.Type.OTHER.getNull(); }
		if (att.hasValues()) { return att.getValue(); }
		return att.getType().getNull();
	}

	protected final List<CmfValue> getAttributeValues(CmfEncodeableName attribute) {
		return getAttributeValues(attribute.encode());
	}

	protected final List<CmfValue> getAttributeValues(String attribute) {
		CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(attribute);
		if (att == null) { return Collections.emptyList(); }
		return att.getValues();
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
						Calendar gcal = Calendar.getInstance();
						try {
							gcal.setTime(v.asTime());
						} catch (ParseException e) {
							throw new RuntimeException("Failed to produce a date value", e);
						}
						gcal.setTimeZone(XmlImportDelegate.TZUTC);
						V = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(gcal);
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

	protected final CmfValue getPropertyValue(CmfEncodeableName attribute) {
		return getPropertyValue(attribute.encode());
	}

	protected final CmfValue getPropertyValue(String attribute) {
		CmfProperty<CmfValue> att = this.cmfObject.getProperty(attribute);
		if (att == null) { return CmfValue.Type.OTHER.getNull(); }
		if (att.hasValues()) { return att.getValue(); }
		return att.getType().getNull();
	}

	protected final List<CmfValue> getPropertyValues(CmfEncodeableName attribute) {
		return getPropertyValues(attribute.encode());
	}

	protected final List<CmfValue> getPropertyValues(String attribute) {
		CmfProperty<CmfValue> att = this.cmfObject.getProperty(attribute);
		if (att == null) { return Collections.emptyList(); }
		return att.getValues();
	}

	protected XmlImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, File.class, storedObject);
	}
}