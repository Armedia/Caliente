
package com.armedia.caliente.engine.dynamic.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.armedia.commons.utilities.XmlTools;

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