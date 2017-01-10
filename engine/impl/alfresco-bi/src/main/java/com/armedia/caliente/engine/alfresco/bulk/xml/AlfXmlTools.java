package com.armedia.caliente.engine.alfresco.bulk.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

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

public class AlfXmlTools {

	public static final Class<?>[] NO_CLASSES = {};

	public static interface ValueSerializer<T> {
		public String serialize(T value);
	}

	public static final <T> ValueSerializer<T> getDefaultSerializer() {
		return new ValueSerializer<T>() {
			@Override
			public String serialize(T value) {
				if (value == null) { return null; }
				return value.toString();
			}
		};
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
			factory.setProperty(WstxOutputProperties.P_USE_DOUBLE_QUOTES_IN_XML_DECL, true);
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
			return AlfXmlTools.OUTPUT_FACTORY.get();
		} catch (ConcurrentException e) {
			throw new XMLStreamException("Failed to initialize the XMLOutputFactory", e);
		}
	}

	private static XMLStreamWriter getWrappedStreamWriter(XMLStreamWriter writer) throws XMLStreamException {
		return new IndentingXMLStreamWriter(writer) {
			@Override
			public NamespaceContext getNamespaceContext() {
				return AlfXmlTools.NO_NAMESPACES;
			}
		};
	}

	private static JAXBContext getJAXBContext() throws JAXBException {
		try {
			return AlfXmlTools.JAXB_CONTEXT.get();
		} catch (ConcurrentException e) {
			throw new JAXBException("Failed to initialize the JAXB Context", e);
		}
	}

	public static XMLStreamWriter getXMLStreamWriter(Writer out) throws XMLStreamException {
		XMLOutputFactory factory = AlfXmlTools.getXMLOutputFactory();
		return AlfXmlTools.getWrappedStreamWriter(factory.createXMLStreamWriter(out));
	}

	public static XMLStreamWriter getXMLStreamWriter(OutputStream out) throws XMLStreamException {
		XMLOutputFactory factory = AlfXmlTools.getXMLOutputFactory();
		XMLStreamWriter writer = factory.createXMLStreamWriter(out);
		return AlfXmlTools.getWrappedStreamWriter(writer);
	}

	public static XMLStreamWriter getXMLStreamWriter(OutputStream out, Charset charset) throws XMLStreamException {
		return AlfXmlTools.getXMLStreamWriter(out, charset != null ? charset.name() : null);
	}

	public static XMLStreamWriter getXMLStreamWriter(OutputStream out, String encoding) throws XMLStreamException {
		XMLOutputFactory factory = AlfXmlTools.getXMLOutputFactory();
		if (encoding == null) {
			encoding = Charset.defaultCharset().name();
		}
		return AlfXmlTools.getWrappedStreamWriter(factory.createXMLStreamWriter(out, encoding));
	}

	public static XMLInputFactory getXMLInputFactory() throws XMLStreamException {
		try {
			return AlfXmlTools.INPUT_FACTORY.get();
		} catch (ConcurrentException e) {
			throw new XMLStreamException("Failed to initialize the XMLInputFactory", e);
		}
	}

	public static XMLStreamReader getXMLStreamReader(Reader in) throws XMLStreamException {
		XMLInputFactory factory = AlfXmlTools.getXMLInputFactory();
		return factory.createXMLStreamReader(in);
	}

	public static XMLStreamReader getXMLStreamReader(InputStream in) throws XMLStreamException {
		XMLInputFactory factory = AlfXmlTools.getXMLInputFactory();
		return factory.createXMLStreamReader(in);
	}

	public static XMLStreamReader getXMLStreamReader(InputStream in, Charset charset) throws XMLStreamException {
		return AlfXmlTools.getXMLStreamReader(in, charset != null ? charset.name() : null);
	}

	public static XMLStreamReader getXMLStreamReader(InputStream in, String encoding) throws XMLStreamException {
		XMLInputFactory factory = AlfXmlTools.getXMLInputFactory();
		if (encoding == null) {
			encoding = Charset.defaultCharset().name();
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

	public static void savePropertiesToXML(Properties p, OutputStream out, String comment) throws IOException {
		AlfXmlTools.savePropertiesToXML(p, out, comment, (Charset) null);
	}

	public static void savePropertiesToXML(Properties p, OutputStream out, String comment, String encoding)
		throws IOException {
		AlfXmlTools.savePropertiesToXML(p, out, comment, (encoding != null ? Charset.forName(encoding) : null));
	}

	public static void savePropertiesToXML(Properties p, OutputStream out, String comment, Charset charset)
		throws IOException {
		AlfXmlTools.savePropertiesToXML(AlfXmlTools.toMap(p), out, comment, charset, null);
	}

	public static void savePropertiesToXML(Map<String, String> m, OutputStream out, String comment) throws IOException {
		AlfXmlTools.savePropertiesToXML(m, out, comment, (Charset) null, null);
	}

	public static void savePropertiesToXML(Map<String, String> m, OutputStream out, String comment, String encoding)
		throws IOException {
		AlfXmlTools.savePropertiesToXML(m, out, comment, (encoding != null ? Charset.forName(encoding) : null), null);
	}

	public static void savePropertiesToXML(Map<String, String> m, OutputStream out, String comment, Charset charset)
		throws IOException {
		AlfXmlTools.savePropertiesToXML(m, out, comment, charset, null);
	}

	public static <T> void savePropertiesToXML(Map<String, T> p, OutputStream out, String comment, Charset charset,
		ValueSerializer<T> serializer) throws IOException {
		if (charset == null) {
			charset = Charset.defaultCharset();
		}

		if (serializer == null) {
			serializer = AlfXmlTools.getDefaultSerializer();
		}

		final String charsetName = charset.name();
		try {
			XMLStreamWriter xml = AlfXmlTools.getXMLStreamWriter(out, charsetName);
			Marshaller marshaller = AlfXmlTools.getJAXBContext().createMarshaller();

			xml.writeStartDocument(charsetName, "1.1");
			xml.writeDTD(AlfXmlTools.PROPERTIES_DTD);
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
				String value = serializer.serialize(p.get(key));
				if (value == null) {
					continue;
				}
				entry.setKey(key);
				entry.setValue(value);
				marshaller.marshal(entry, out);
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

	public static void loadPropertiesFromXML(Properties properties, InputStream in)
		throws IOException, XMLStreamException {
		AlfXmlTools.loadPropertiesFromXML(properties, in, (Charset) null);
	}

	public static void loadPropertiesFromXML(Properties properties, InputStream in, String encoding)
		throws IOException, XMLStreamException {
		AlfXmlTools.loadPropertiesFromXML(properties, in, encoding != null ? Charset.forName(encoding) : null);
	}

	public static void loadPropertiesFromXML(Properties properties, InputStream in, Charset charset)
		throws IOException, XMLStreamException {

		properties.clear();

		if (charset == null) {
			charset = Charset.defaultCharset();
		}

		try {
			XMLStreamReader xml = AlfXmlTools.getXMLStreamReader(in, charset);

			// Find the <properties> element...
			while (xml.hasNext() && (xml.next() != XMLStreamConstants.START_ELEMENT)) {
				continue;
			}

			if (!xml.hasNext()) { throw new XMLStreamException("Malformed XML document - no root element",
				xml.getLocation()); }

			// Go through the <comment> or <entry> elements
			if (!"properties".equals(xml.getLocalName())) { throw new XMLStreamException(
				String.format("Unknown XML element '%s'", xml.getLocalName()), xml.getLocation()); }

			Unmarshaller unmarshaller = AlfXmlTools.getJAXBContext().createUnmarshaller();

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

				if (!"entry".equals(element)) { throw new XMLStreamException(
					String.format("Unknown XML element '%s'", element), xml.getLocation()); }

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
	}
}