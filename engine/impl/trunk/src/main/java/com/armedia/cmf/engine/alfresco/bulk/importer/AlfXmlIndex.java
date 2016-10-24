package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.IOUtils;

import javanet.staxutils.IndentingXMLStreamWriter;

public class AlfXmlIndex implements Closeable {

	private final File target;
	private final Collection<Class<?>> supportedClasses;

	private Marshaller marshaller = null;
	private OutputStream out = null;
	private XMLStreamWriter xml = null;
	private boolean initialized = false;
	private boolean closed = false;

	public AlfXmlIndex(File target, Class<?>... classes)
		throws XMLStreamException, JAXBException, FileNotFoundException {
		this.target = target;
		this.supportedClasses = Arrays.asList(classes);

		JAXBContext jaxbContext = getJAXBContext(classes);
		this.marshaller = getMarshaller(jaxbContext);

		this.out = new FileOutputStream(target);

		XMLOutputFactory factory = getXMLOutputFactory();
		this.xml = getXMLStreamWriter(factory, this.out);
	}

	protected JAXBContext getJAXBContext(Class<?>... classes) throws JAXBException {
		return JAXBContext.newInstance(classes);
	}

	protected Marshaller getMarshaller(JAXBContext context) throws JAXBException {
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		return m;
	}

	protected Marshaller getMarshaller() {
		return this.marshaller;
	}

	protected XMLStreamWriter getXMLStreamWriter(XMLOutputFactory factory, OutputStream out) throws XMLStreamException {
		return new IndentingXMLStreamWriter(factory.createXMLStreamWriter(out));
	}

	protected final XMLStreamWriter getXMLStreamWriter() {
		return this.xml;
	}

	protected XMLOutputFactory getXMLOutputFactory() {
		return XMLOutputFactory.newInstance();
	}

	public synchronized boolean isClosed() {
		return this.closed;
	}

	public File getTarget() {
		return this.target;
	}

	public Collection<Class<?>> getSupportedClasses() {
		return this.supportedClasses;
	}

	protected synchronized void assertOpen() {
		if (isClosed()) { throw new IllegalStateException("Already closed"); }
	}

	public synchronized boolean init() throws XMLStreamException {
		assertOpen();
		if (this.initialized) { return false; }
		boolean ok = false;
		try {
			this.xml.writeStartDocument(getEncoding(), getVersion());
			this.xml.writeStartElement(getRootElement());
			Map<String, String> m = getRootAttributes();
			if (m != null) {
				for (String k : m.keySet()) {
					String v = m.get(k);
					if (v != null) {
						this.xml.writeAttribute(k, v);
					}
				}
			}
			this.xml.flush();
			ok = true;
			return true;
		} finally {
			this.initialized = ok;
			if (!ok) {
				close();
			}
		}
	}

	protected String getVersion() {
		return "1.0";
	}

	protected String getEncoding() {
		return Charset.defaultCharset().name();
	}

	protected String getRootElement() {
		return "scan";
	}

	protected Map<String, String> getRootAttributes() {
		return Collections.emptyMap();
	}

	public synchronized void addElement(String name) throws XMLStreamException {
		addElement(name, null);
	}

	public synchronized void addElement(String name, Map<String, String> attributes) throws XMLStreamException {
		init();
		this.xml.writeStartElement(name);
		if (attributes != null) {
			for (String k : attributes.keySet()) {
				String v = attributes.get(k);
				if (v != null) {
					this.xml.writeAttribute(k, v);
				}
			}
		}
		flush();
	}

	public synchronized void closeElement() throws XMLStreamException {
		assertOpen();
		this.xml.writeEndElement();
	}

	public synchronized long marshal(Object... objects) throws JAXBException, XMLStreamException {
		init();
		if (objects == null) { return 0; }
		long ret = 0;
		for (Object o : objects) {
			if (o == null) {
				continue;
			}
			this.marshaller.marshal(o, this.xml);
			ret++;
		}
		this.xml.flush();
		return ret;
	}

	public synchronized void flush() throws XMLStreamException {
		assertOpen();
		this.xml.flush();
	}

	@Override
	public synchronized void close() {
		if (isClosed()) { return; }
		try {
			if (this.initialized) {
				try {
					flush();
					this.xml.close();
				} catch (XMLStreamException e) {
					// Log it...???
				} finally {
					IOUtils.closeQuietly(this.out);
					this.out = null;
					this.xml = null;
					this.marshaller = null;
				}
			}
		} finally {
			this.closed = true;
		}
	}
}
