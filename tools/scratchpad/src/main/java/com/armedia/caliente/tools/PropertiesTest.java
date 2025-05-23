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
package com.armedia.caliente.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
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

import com.armedia.commons.utilities.io.BinaryMemoryBuffer;
import com.armedia.commons.utilities.xml.XmlStreamElement;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxOutputFactory;

import javanet.staxutils.IndentingXMLStreamWriter;

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
		public Iterator<String> getPrefixes(String namespaceURI) {
			return null;
		}

	};

	private static final Charset CHARSET = Charset.forName("UTF-8");
	private static final String CHARSET_NAME = PropertiesTest.CHARSET.name();
	/*
	private static final String DTD = String
		.format("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">%n");
	*/
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
			// writer.writeDTD(PropertiesTest.DTD);
			try (XmlStreamElement xmlProperties = new XmlStreamElement(writer, "properties")) {
				writer.flush();
				out.flush();
				if (comment != null) {
					try (XmlStreamElement xmlComment = xmlProperties.newElement("comment")) {
						writer.writeCharacters(comment);
					}
					out.flush();
				}
				for (final String key : new TreeSet<>(p.stringPropertyNames())) {
					String value = p.getProperty(key);
					if (value == null) {
						continue;
					}
					try (XmlStreamElement xmlEntry = xmlProperties.newElement("entry")) {
						writer.writeAttribute("key", key);
						writer.writeCharacters(p.getProperty(key));
						out.flush();
					}
				}
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
		Charset charset = PropertiesTest.CHARSET;
		CharsetEncoder encoder = charset.newEncoder();
		for (int i = 1; i < 0xFFFE; i++) {
			char c = (char) i;
			if (!encoder.canEncode(c)) {
				continue;
			}
			String key = String.format("char[%02x]", i);
			// String key = String.format("char[%02x] == (%s)", i, c);
			p.setProperty(key, String.format("[%s]", c));
			System.out.printf("[%02x] = [%s]%n", i, c);
		}

		BinaryMemoryBuffer buf = new BinaryMemoryBuffer();
		/* Scratchpad.savePropertiesToXML(p, buf, "ASCII Table Test Properties", Scratchpad.CHARSET_NAME); */
		PropertiesTest.savePropertiesToXML(p, buf, "ASCII Table Test Properties");
		buf.close();

		try (InputStream in = buf.getInputStream()) {
			String s = IOUtils.toString(in, PropertiesTest.CHARSET);
			s.hashCode();
			System.out.println(s);
			System.out.flush();
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

		if (p.size() != q.size()) {
			throw new RuntimeException(String.format("Size difference: %,d vs %,d", p.size(), q.size()));
		}
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
			if (!Objects.equals(src, tgt)) {
				different.put(s, Pair.of(src, tgt));
				continue;
			}

			matched.put(s, src);
		}

		if (!missing.isEmpty()) {
			System.out.printf("Missing keys: %s%n", missing);
		}

		if (!different.isEmpty()) {
			System.out.printf("Different values:%n");
			for (String s : different.keySet()) {
				Pair<String, String> v = different.get(s);
				System.out.printf("\t[%s]: [%s] != [%s]%n", s, v.getLeft(), v.getRight());
			}
		}
	}
}