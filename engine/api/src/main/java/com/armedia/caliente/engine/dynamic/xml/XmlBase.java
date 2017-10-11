
package com.armedia.caliente.engine.dynamic.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.armedia.commons.utilities.XmlTools;

@XmlTransient
public abstract class XmlBase {

	protected static final String SCHEMA = "engine.xsd";

	public final void storeToXML(OutputStream out) throws JAXBException {
		storeToXML(out, false);
	}

	public final void storeToXML(Writer out) throws JAXBException {
		storeToXML(out, false);
	}

	public final void storeToXML(XMLStreamWriter out) throws JAXBException {
		storeToXML(out, false);
	}

	public final void storeToXML(OutputStream out, boolean format) throws JAXBException {
		XmlTools.marshal(this, XmlBase.SCHEMA, out, format);
	}

	public final void storeToXML(Writer out, boolean format) throws JAXBException {
		XmlTools.marshal(this, XmlBase.SCHEMA, out, format);
	}

	public final void storeToXML(XMLStreamWriter out, boolean format) throws JAXBException {
		XmlTools.marshal(this, XmlBase.SCHEMA, out, format);
	}

	public static <T extends XmlBase> T loadFromXML(Class<T> klass, InputStream in) throws JAXBException {
		return XmlTools.unmarshal(klass, XmlBase.SCHEMA, in);
	}

	public static <T extends XmlBase> T loadFromXML(Class<T> klass, Reader in) throws JAXBException {
		return XmlTools.unmarshal(klass, XmlBase.SCHEMA, in);
	}

	public static <T extends XmlBase> T loadFromXML(Class<T> klass, XMLStreamReader in) throws JAXBException {
		return XmlTools.unmarshal(klass, XmlBase.SCHEMA, in);
	}
}