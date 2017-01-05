package com.armedia.caliente.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.tuple.Pair;

import com.armedia.commons.utilities.BinaryMemoryBuffer;
import com.armedia.commons.utilities.Tools;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

public class PropertiesTest {

	private static final NamespaceContext NO_NAMESPACES = new NamespaceContext() {

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

	private static final Charset CHARSET = Charset.forName("UTF-8");
	private static final String CHARSET_NAME = PropertiesTest.CHARSET.name();
	private static final String DTD = "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">";
	private static final LazyInitializer<XMLOutputFactory> FACTORY = new LazyInitializer<XMLOutputFactory>() {
		@Override
		protected XMLOutputFactory initialize() throws ConcurrentException {
			return XMLOutputFactory.newInstance();
		}
	};

	public static void savePropertiesToXML(Properties p, OutputStream out, String comment) throws IOException {
		try {
			XMLOutputFactory factory = PropertiesTest.FACTORY.get();
			XMLStreamWriter writer = new IndentingXMLStreamWriter(
				factory.createXMLStreamWriter(out, PropertiesTest.CHARSET_NAME)) {
				@Override
				public NamespaceContext getNamespaceContext() {
					return PropertiesTest.NO_NAMESPACES;
				}
			};

			writer.writeStartDocument(PropertiesTest.CHARSET_NAME, "1.1");
			writer.writeDTD(PropertiesTest.DTD);
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
			for (final String key : p.stringPropertyNames()) {
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
			writer.writeEndDocument();
			writer.flush();
			out.flush();
		} catch (XMLStreamException | ConcurrentException e) {
			throw new IOException("An XML serialization exception was detected - failed to serialize the properties",
				e);
		}
	}

	public static void test() throws Exception {
		Properties p = new Properties();
		for (int i = 1; i <= 255; i++) {
			char c = (char) i;
			p.setProperty(String.format("char[%02x] == (%s)", i, c), String.format("[%s]", c));
		}

		BinaryMemoryBuffer buf = new BinaryMemoryBuffer();
		/* Scratchpad.savePropertiesToXML(p, buf, "ASCII Table Test Properties", Scratchpad.CHARSET_NAME); */
		PropertiesTest.savePropertiesToXML(p, buf, "ASCII Table Test Properties");
		buf.close();

		try (InputStream in = buf.getInputStream()) {
			String s = IOUtils.toString(in, PropertiesTest.CHARSET);
			s.hashCode();
			/*
			s = s.replace("<?xml version=\"1.0\"", "<?xml version=\"1.1\"");
			buf = new BinaryMemoryBuffer();
			IOUtils.write(s, buf, Scratchpad.CHARSET);
			buf.close();
			*/
		}

		Properties q = new Properties();
		try (InputStream in = buf.getInputStream()) {
			q.loadFromXML(in);
		}

		if (p.size() != q
			.size()) { throw new RuntimeException(String.format("Size difference: %d vs %d", p.size(), q.size())); }
		Map<String, String> matched = new TreeMap<>();
		Set<String> missing = new TreeSet<>();
		Map<String, Pair<String, String>> different = new TreeMap<>();
		for (String s : p.stringPropertyNames()) {
			if (!q.containsKey(s)) {
				missing.add(s);
				continue;
			}
			String src = p.getProperty(s);
			String tgt = q.getProperty(s);
			if (!Tools.equals(src, tgt)) {
				different.put(s, Pair.of(src, tgt));
				continue;
			}

			matched.put(s, src);
		}

		if (!missing.isEmpty()) {
			System.out.printf("Missing keys: %s%n", missing);
		}

		if (!different.isEmpty()) {
			System.out.printf("Different keys:%n");
			for (String s : different.keySet()) {
				Pair<String, String> v = different.get(s);
				System.out.printf("\t[%s]: [%s] != [%s]%n", s, v.getLeft(), v.getRight());
			}
		}
	}
}