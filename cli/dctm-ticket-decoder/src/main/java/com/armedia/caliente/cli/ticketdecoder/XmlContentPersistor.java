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
package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.xml.bind.Marshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.ticketdecoder.xml.Rendition;
import com.armedia.caliente.tools.xml.FlexibleCharacterEscapeHandler;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.function.CheckedLazySupplier;
import com.armedia.commons.utilities.xml.XmlTools;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxOutputFactory;

import javanet.staxutils.IndentingXMLStreamWriter;

public class XmlContentPersistor extends FileContentPersistor {

	private static final String BASE_NAME = "XML";
	private static final CheckedLazySupplier<XMLOutputFactory, XMLStreamException> OUTPUT_FACTORY = new CheckedLazySupplier<>(
		() -> {
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
		});

	private XMLStreamWriter xml = null;
	private Marshaller marshaller = null;

	public XmlContentPersistor(File target) {
		super(Objects.requireNonNull(target), XmlContentPersistor.BASE_NAME);
	}

	@Override
	protected void startup() throws Exception {
		super.startup();

		XMLOutputFactory factory = XmlContentPersistor.OUTPUT_FACTORY.get();
		XMLStreamWriter writer = factory.createXMLStreamWriter(this.out);
		this.xml = new IndentingXMLStreamWriter(writer) {
			@Override
			public NamespaceContext getNamespaceContext() {
				return XmlProperties.NO_NAMESPACES;
			}
		};
		this.xml.writeStartDocument(Charset.defaultCharset().name(), "1.1");
		String rootElement = "contents";
		this.xml.writeDTD(String.format("<!DOCTYPE %s>", rootElement));
		this.xml.writeStartElement(rootElement);
		this.xml.flush();

		this.marshaller = XmlTools.getMarshaller("ticket-decoder.xsd", Content.class, Rendition.class);
		Charset charset = StandardCharsets.UTF_8;
		this.marshaller.setProperty(Marshaller.JAXB_ENCODING, charset.name());
		this.marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
		this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		FlexibleCharacterEscapeHandler.getInstance(charset).configure(this.marshaller);
	}

	@Override
	protected void persistContent(Content content) throws Exception {
		this.marshaller.marshal(content, this.xml);
		this.out.flush();
	}

	@Override
	protected void cleanup() throws Exception {
		if (this.xml != null) {
			this.xml.flush();
			this.xml.writeEndDocument();
			this.xml.close();
			this.xml = null;
		}
		super.cleanup();
	}
}