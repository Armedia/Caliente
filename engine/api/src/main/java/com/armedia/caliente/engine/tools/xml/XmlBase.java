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

package com.armedia.caliente.engine.tools.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.armedia.commons.utilities.xml.XmlTools;

public abstract class XmlBase {

	protected static final String DEFAULT_SCHEMA = "engine.xsd";
	public static final boolean DEFAULT_FORMAT = true;

	public static <T> void storeToXML(T object, OutputStream out) throws JAXBException {
		XmlBase.storeToXML(object, XmlBase.DEFAULT_SCHEMA, out, XmlBase.DEFAULT_FORMAT);
	}

	public static <T> void storeToXML(T object, OutputStream out, boolean format) throws JAXBException {
		XmlBase.storeToXML(object, XmlBase.DEFAULT_SCHEMA, out, format);
	}

	public static <T> void storeToXML(T object, String schema, OutputStream out, boolean format) throws JAXBException {
		XmlTools.marshal(object, schema, out, format);
	}

	public static <T> void storeToXML(T object, Writer out) throws JAXBException {
		XmlBase.storeToXML(object, XmlBase.DEFAULT_SCHEMA, out, XmlBase.DEFAULT_FORMAT);
	}

	public static <T> void storeToXML(T object, Writer out, boolean format) throws JAXBException {
		XmlBase.storeToXML(object, XmlBase.DEFAULT_SCHEMA, out, format);
	}

	public static <T> void storeToXML(T object, String schema, Writer out, boolean format) throws JAXBException {
		XmlTools.marshal(object, schema, out, format);
	}

	public static <T> void storeToXML(T object, XMLStreamWriter out) throws JAXBException {
		XmlBase.storeToXML(object, XmlBase.DEFAULT_SCHEMA, out, XmlBase.DEFAULT_FORMAT);
	}

	public static <T> void storeToXML(T object, XMLStreamWriter out, boolean format) throws JAXBException {
		XmlBase.storeToXML(object, XmlBase.DEFAULT_SCHEMA, out, format);
	}

	public static <T> void storeToXML(T object, String schema, XMLStreamWriter out, boolean format)
		throws JAXBException {
		XmlTools.marshal(object, schema, out, format);
	}

	public static <T> T loadFromXML(Class<T> klass, InputStream in) throws JAXBException {
		return XmlBase.loadFromXML(klass, XmlBase.DEFAULT_SCHEMA, in);
	}

	public static <T> T loadFromXML(Class<T> klass, String schema, InputStream in) throws JAXBException {
		return XmlTools.unmarshal(klass, schema, in);
	}

	public static <T> T loadFromXML(Class<T> klass, Reader in) throws JAXBException {
		return XmlBase.loadFromXML(klass, XmlBase.DEFAULT_SCHEMA, in);
	}

	public static <T> T loadFromXML(Class<T> klass, String schema, Reader in) throws JAXBException {
		return XmlTools.unmarshal(klass, schema, in);
	}

	public static <T> T loadFromXML(Class<T> klass, XMLStreamReader in) throws JAXBException {
		return XmlBase.loadFromXML(klass, XmlBase.DEFAULT_SCHEMA, in);
	}

	public static <T> T loadFromXML(Class<T> klass, String schema, XMLStreamReader in) throws JAXBException {
		return XmlTools.unmarshal(klass, schema, in);
	}
}