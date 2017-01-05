package com.armedia.caliente.engine.alfresco.bulk.common;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Properties;
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

	private static final String DTD = String
		.format("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">%n");
	private static final LazyInitializer<XMLOutputFactory> FACTORY = new LazyInitializer<XMLOutputFactory>() {
		@Override
		protected XMLOutputFactory initialize() throws ConcurrentException {
			// return XMLOutputFactory.newInstance();
			WstxOutputFactory factory = new WstxOutputFactory();
			factory.setProperty(WstxOutputProperties.P_USE_DOUBLE_QUOTES_IN_XML_DECL, true);
			factory.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES, true);
			factory.setProperty(WstxOutputProperties.P_ADD_SPACE_AFTER_EMPTY_ELEM, true);
			factory.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_ATTR, true);
			return factory;
		}
	};

	public static void savePropertiesToXML(Properties p, OutputStream out, String comment) throws IOException {
		AlfXmlTools.savePropertiesToXML(p, out, comment, (Charset) null);
	}

	public static void savePropertiesToXML(Properties p, OutputStream out, String comment, String encoding)
		throws IOException {
		AlfXmlTools.savePropertiesToXML(p, out, comment, (encoding != null ? Charset.forName(encoding) : null));
	}

	public static void savePropertiesToXML(Properties p, OutputStream out, String comment, Charset charset)
		throws IOException {
		if (charset == null) {
			charset = Charset.defaultCharset();
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
			writer.writeDTD(AlfXmlTools.DTD);
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
			for (final String key : new TreeSet<>(p.stringPropertyNames())) {
				String value = p.getProperty(key);
				if (value == null) {
					continue;
				}
				writer.writeStartElement("entry");
				writer.writeAttribute("key", key);
				writer.writeCharacters(p.getProperty(key));
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