package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;

import javax.xml.bind.Marshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.ticketdecoder.xml.Rendition;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.XmlTools;
import com.armedia.commons.utilities.concurrent.BaseReadWriteLockable;
import com.armedia.commons.utilities.function.CheckedLazySupplier;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxOutputFactory;

import javanet.staxutils.IndentingXMLStreamWriter;

public class XmlContentPersistor extends BaseReadWriteLockable implements ContentPersistor {

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

	private OutputStream out = null;
	private XMLStreamWriter xml = null;
	private Marshaller marshaller = null;

	@Override
	public void initialize(final File target) throws Exception {
		final File finalTarget = Tools.canonicalize(target);
		writeLocked(() -> {

			this.out = new FileOutputStream(finalTarget);

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
			writer.flush();

			this.marshaller = XmlTools.getMarshaller(Content.class, Rendition.class);
			this.marshaller.setProperty(Marshaller.JAXB_ENCODING, Charset.defaultCharset().name());
			this.marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		});
	}

	@Override
	public void persist(Content content) throws Exception {
		if (content != null) {
			writeLocked(() -> this.marshaller.marshal(content, this.xml));
		}
	}

	@Override
	public void close() throws Exception {
		writeLocked(() -> {
			this.xml.flush();
			this.xml.writeEndDocument();
			this.xml.close();
			this.out.flush();
			this.out.close();
		});
	}
}