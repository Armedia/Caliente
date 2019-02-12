package com.armedia.caliente.tools.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;

import javanet.staxutils.IndentingXMLStreamWriter;

public final class XmlProperties {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	public static final Class<?>[] NO_CLASSES = {};

	public static final <T> Function<T, String> getDefaultSerializer() {
		return (value) -> (value != null ? value.toString() : null);
	}

	public static final NamespaceContext NO_NAMESPACES = new NamespaceContext() {

		@Override
		public String getNamespaceURI(String prefix) {
			return "";
		}

		@Override
		public String getPrefix(String namespaceURI) {
			return "";
		}

		@Override
		public Iterator<?> getPrefixes(String namespaceURI) {
			return null;
		}
	};

	private static final String PROPERTIES_DTD = String
		.format("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">%n");

	private static final LazyInitializer<XMLOutputFactory> OUTPUT_FACTORY = new LazyInitializer<XMLOutputFactory>() {
		@Override
		protected XMLOutputFactory initialize() throws ConcurrentException {
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
		}
	};

	private static final LazyInitializer<XMLInputFactory> INPUT_FACTORY = new LazyInitializer<XMLInputFactory>() {
		@Override
		protected XMLInputFactory initialize() throws ConcurrentException {
			return new WstxInputFactory();
		}
	};

	private static final LazyInitializer<JAXBContext> JAXB_CONTEXT = new LazyInitializer<JAXBContext>() {
		@Override
		protected JAXBContext initialize() throws ConcurrentException {
			try {
				return JAXBContext.newInstance(PropertiesRoot.class, PropertiesComment.class, PropertiesEntry.class);
			} catch (JAXBException e) {
				throw new ConcurrentException("Failed to initialize the JAXB Context", e);
			}
		}

	};

	public static XMLOutputFactory getXMLOutputFactory() throws XMLStreamException {
		try {
			return XmlProperties.OUTPUT_FACTORY.get();
		} catch (ConcurrentException e) {
			throw new XMLStreamException("Failed to initialize the XMLOutputFactory", e);
		}
	}

	private static XMLStreamWriter getWrappedStreamWriter(XMLStreamWriter writer) throws XMLStreamException {
		return new IndentingXMLStreamWriter(writer) {
			@Override
			public NamespaceContext getNamespaceContext() {
				return XmlProperties.NO_NAMESPACES;
			}
		};
	}

	private static JAXBContext getJAXBContext() throws JAXBException {
		try {
			return XmlProperties.JAXB_CONTEXT.get();
		} catch (ConcurrentException e) {
			throw new JAXBException("Failed to initialize the JAXB Context", e);
		}
	}

	public static XMLStreamWriter getXMLStreamWriter(Writer out) throws XMLStreamException {
		XMLOutputFactory factory = XmlProperties.getXMLOutputFactory();
		return XmlProperties.getWrappedStreamWriter(factory.createXMLStreamWriter(out));
	}

	public static XMLStreamWriter getXMLStreamWriter(OutputStream out) throws XMLStreamException {
		XMLOutputFactory factory = XmlProperties.getXMLOutputFactory();
		XMLStreamWriter writer = factory.createXMLStreamWriter(out);
		return XmlProperties.getWrappedStreamWriter(writer);
	}

	public static XMLStreamWriter getXMLStreamWriter(OutputStream out, Charset charset) throws XMLStreamException {
		return XmlProperties.getXMLStreamWriter(out, charset != null ? charset.name() : null);
	}

	public static XMLStreamWriter getXMLStreamWriter(OutputStream out, String encoding) throws XMLStreamException {
		XMLOutputFactory factory = XmlProperties.getXMLOutputFactory();
		if (encoding == null) {
			encoding = XmlProperties.DEFAULT_CHARSET.name();
		}
		return XmlProperties.getWrappedStreamWriter(factory.createXMLStreamWriter(out, encoding));
	}

	public static XMLInputFactory getXMLInputFactory() throws XMLStreamException {
		try {
			return XmlProperties.INPUT_FACTORY.get();
		} catch (ConcurrentException e) {
			throw new XMLStreamException("Failed to initialize the XMLInputFactory", e);
		}
	}

	public static XMLStreamReader getXMLStreamReader(Reader in) throws XMLStreamException {
		XMLInputFactory factory = XmlProperties.getXMLInputFactory();
		return factory.createXMLStreamReader(in);
	}

	public static XMLStreamReader getXMLStreamReader(InputStream in) throws XMLStreamException {
		XMLInputFactory factory = XmlProperties.getXMLInputFactory();
		return factory.createXMLStreamReader(in);
	}

	public static XMLStreamReader getXMLStreamReader(InputStream in, Charset charset) throws XMLStreamException {
		return XmlProperties.getXMLStreamReader(in, charset != null ? charset.name() : null);
	}

	public static XMLStreamReader getXMLStreamReader(InputStream in, String encoding) throws XMLStreamException {
		XMLInputFactory factory = XmlProperties.getXMLInputFactory();
		if (encoding == null) {
			encoding = XmlProperties.DEFAULT_CHARSET.name();
		}
		return factory.createXMLStreamReader(in, encoding);
	}

	private static Map<String, String> toMap(Properties p) {
		if (p == null) { return null; }

		Map<String, String> m = new LinkedHashMap<>();
		for (String s : p.stringPropertyNames()) {
			String v = p.getProperty(s);
			if (v != null) {
				m.put(s, v);
			}
		}
		return m;
	}

	public static void saveToXML(Properties p, OutputStream out, String comment) throws IOException {
		XmlProperties.saveToXML(p, out, comment, (Charset) null);
	}

	public static void saveToXML(Properties p, OutputStream out, String comment, String encoding) throws IOException {
		XmlProperties.saveToXML(p, out, comment, (encoding != null ? Charset.forName(encoding) : null));
	}

	public static void saveToXML(Properties p, OutputStream out, String comment, Charset charset) throws IOException {
		XmlProperties.saveToXML(XmlProperties.toMap(p), out, comment, charset, null);
	}

	public static void saveToXML(Map<String, String> m, OutputStream out, String comment) throws IOException {
		XmlProperties.saveToXML(m, out, comment, (Charset) null, null);
	}

	public static void saveToXML(Map<String, String> m, OutputStream out, String comment, String encoding)
		throws IOException {
		XmlProperties.saveToXML(m, out, comment, (encoding != null ? Charset.forName(encoding) : null), null);
	}

	public static void saveToXML(Map<String, String> m, OutputStream out, String comment, Charset charset)
		throws IOException {
		XmlProperties.saveToXML(m, out, comment, charset, null);
	}

	public static <T> void saveToXML(Map<String, T> p, OutputStream out, String comment, Charset charset,
		Function<T, String> serializer) throws IOException {
		if (charset == null) {
			// Default to UTF-8
			charset = XmlProperties.DEFAULT_CHARSET;
		}

		if (serializer == null) {
			serializer = XmlProperties.getDefaultSerializer();
		}

		final String charsetName = charset.name();
		try {
			XMLStreamWriter xml = XmlProperties.getXMLStreamWriter(out, charsetName);
			Marshaller marshaller = XmlProperties.getJAXBContext().createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

			xml.writeStartDocument(charsetName, "1.1");
			// Remove the DTD declaration - this can cause problems in some environments
			xml.writeDTD(XmlProperties.PROPERTIES_DTD);
			xml.writeStartElement("properties");
			xml.flush();
			out.flush();
			if (comment != null) {
				PropertiesComment c = new PropertiesComment();
				c.setValue(comment);
				marshaller.marshal(c, xml);
				xml.flush();
				out.flush();
			}

			Set<String> keys = new TreeSet<>();
			// Filter out null keys
			for (String key : p.keySet()) {
				if (key != null) {
					keys.add(key);
				}
			}

			// Output the the values...
			PropertiesEntry entry = new PropertiesEntry();
			for (final String key : keys) {
				String value = serializer.apply(p.get(key));
				if (value == null) {
					continue;
				}
				entry.setKey(key);
				entry.setValue(value);
				marshaller.marshal(entry, xml);
				xml.flush();
				out.flush();
			}
			xml.writeEndElement();
			xml.writeEndDocument();
			xml.flush();
			out.flush();
		} catch (XMLStreamException | JAXBException e) {
			throw new IOException("An XML serialization exception was detected - failed to store the properties", e);
		}
	}

	public static Properties loadFromXML(InputStream in) throws IOException, XMLStreamException {
		return XmlProperties.loadFromXML(in, (Charset) null);
	}

	public static Properties loadFromXML(InputStream in, String encoding) throws IOException, XMLStreamException {
		return XmlProperties.loadFromXML(in, encoding != null ? Charset.forName(encoding) : null);
	}

	public static Properties loadFromXML(InputStream in, Charset charset) throws IOException, XMLStreamException {
		if (in == null) { throw new IllegalArgumentException("Must provide a stream to read from"); }
		if (charset == null) {
			charset = XmlProperties.DEFAULT_CHARSET;
		}
		Properties properties = new Properties();
		try {
			XMLStreamReader xml = XmlProperties.getXMLStreamReader(in, charset);

			// Find the <properties> element...
			while (xml.hasNext() && (xml.next() != XMLStreamConstants.START_ELEMENT)) {
				continue;
			}

			if (!xml.hasNext()) {
				throw new XMLStreamException("Malformed XML document - no root element", xml.getLocation());
			}

			// Go through the <comment> or <entry> elements
			if (!"properties".equals(xml.getLocalName())) {
				throw new XMLStreamException(String.format("Unknown XML element '%s'", xml.getLocalName()),
					xml.getLocation());
			}

			Unmarshaller unmarshaller = XmlProperties.getJAXBContext().createUnmarshaller();

			boolean hasComment = false;
			while (xml.nextTag() == XMLStreamConstants.START_ELEMENT) {
				final String element = xml.getLocalName();

				// Not interested in the comment
				if ("comment".equals(element)) {
					Location location = xml.getLocation();
					if (!hasComment) {
						// Consume the comment
						unmarshaller.unmarshal(xml, PropertiesComment.class);
						hasComment = true;
						continue;
					}
					throw new XMLStreamException("Multiple <comment> elements found - this is a violation", location);
				}

				if (!"entry".equals(element)) {
					throw new XMLStreamException(String.format("Unknown XML element '%s'", element), xml.getLocation());
				}

				JAXBElement<PropertiesEntry> xmlItem = unmarshaller.unmarshal(xml, PropertiesEntry.class);
				if (xmlItem == null) {
					continue;
				}
				PropertiesEntry entry = xmlItem.getValue();
				if (entry == null) {
					continue;
				}
				if ((entry.getKey() == null) || (entry.getValue() == null)) {
					continue;
				}
				if (properties.containsKey(entry.getKey())) {
					// do not clobber
					continue;
				}
				properties.setProperty(entry.getKey(), entry.getValue());
			}
		} catch (JAXBException e) {
			throw new XMLStreamException(
				"An XML deserialization exception was detected - failed to load the properties", e);
		}
		return properties;
	}

	private XmlProperties() {
		// No class can instantiate this type
	}
}