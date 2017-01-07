package com.armedia.caliente.engine.alfresco.bulk.common;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.ctc.wstx.api.WstxOutputProperties;
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

	public static final LazyInitializer<XMLOutputFactory> FACTORY = new LazyInitializer<XMLOutputFactory>() {
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

	public static XMLStreamWriter getXMLStreamWriter(OutputStream out) throws XMLStreamException {
		XMLOutputFactory factory;
		try {
			factory = AlfXmlTools.FACTORY.get();
		} catch (ConcurrentException e) {
			throw new XMLStreamException("Failed to initialize the XMLOutputFactory", e);
		}
		return new IndentingXMLStreamWriter(factory.createXMLStreamWriter(out)) {
			@Override
			public NamespaceContext getNamespaceContext() {
				return AlfXmlTools.NO_NAMESPACES;
			}
		};
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
			XMLOutputFactory factory = AlfXmlTools.FACTORY.get();
			XMLStreamWriter writer = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(out, charsetName)) {
				@Override
				public NamespaceContext getNamespaceContext() {
					return AlfXmlTools.NO_NAMESPACES;
				}
			};

			writer.writeStartDocument(charsetName, "1.1");
			writer.writeDTD(AlfXmlTools.PROPERTIES_DTD);
			writer.writeStartElement("properties");
			writer.flush();
			out.flush();
			if (comment != null) {
				writer.writeStartElement("comment");
				writer.writeCharacters(comment);
				writer.writeEndElement();
				writer.flush();
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
			for (final String key : keys) {
				String value = serializer.serialize(p.get(key));
				if (value == null) {
					continue;
				}
				writer.writeStartElement("entry");
				writer.writeAttribute("key", key);
				writer.writeCharacters(value);
				writer.writeEndElement();
				writer.flush();
				out.flush();
			}
			writer.writeEndElement();
			writer.writeEndDocument();
			writer.flush();
			out.flush();
		} catch (XMLStreamException | ConcurrentException e) {
			throw new IOException("An XML serialization exception was detected - failed to serialize the properties",
				e);
		}
	}
}