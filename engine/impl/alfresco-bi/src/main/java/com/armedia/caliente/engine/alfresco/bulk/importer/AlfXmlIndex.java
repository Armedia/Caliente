package com.armedia.caliente.engine.alfresco.bulk.importer;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.alfresco.bulk.common.AlfXmlTools;
import com.armedia.commons.utilities.Tools;

import javanet.staxutils.IndentingXMLStreamWriter;

public class AlfXmlIndex implements Closeable {

	private final File target;
	private final Collection<Class<?>> supportedClasses;
	private final Class<?> rootClass;
	private final String rootElement;

	private Marshaller marshaller = null;
	private OutputStream out = null;
	private XMLStreamWriter xml = null;
	private boolean initialized = false;
	private boolean closed = false;

	public AlfXmlIndex(File target, Class<?>... classes) throws JAXBException {
		this.target = target;
		List<Class<?>> l = new ArrayList<>();
		if (classes != null) {
			for (Class<?> c : classes) {
				if (c != null) {
					l.add(c);
				}
			}
		}
		if (l
			.isEmpty()) { throw new IllegalArgumentException("No classes provided to support the indexing operation"); }
		this.supportedClasses = Tools.freezeList(l);
		this.rootClass = this.supportedClasses.iterator().next();
		final XmlRootElement rootElementDecl = this.rootClass.getAnnotation(XmlRootElement.class);
		if (rootElementDecl == null) { throw new JAXBException(String
			.format("The root class [%s] lacks an XmlRootElement annotation", this.rootClass.getCanonicalName())); }
		String rootElement = rootElementDecl.name();
		if ((rootElement == null) || "##default".equals(rootElement)) {
			rootElement = StringUtils.uncapitalize(this.rootClass.getSimpleName());
		}
		this.rootElement = rootElement;
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
		return new IndentingXMLStreamWriter(factory.createXMLStreamWriter(out)) {
			@Override
			public NamespaceContext getNamespaceContext() {
				return AlfXmlTools.NO_NAMESPACES;
			}
		};
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
			JAXBContext jaxbContext = getJAXBContext(this.supportedClasses.toArray(AlfXmlTools.NO_CLASSES));
			this.marshaller = getMarshaller(jaxbContext);

			this.out = new FileOutputStream(this.target);

			XMLOutputFactory factory = getXMLOutputFactory();
			this.xml = getXMLStreamWriter(factory, this.out);

			this.xml.writeStartDocument(getEncoding(), getVersion());
			// TODO: Enable this in concert with the BI AMP, since it's not built to handle DTD
			// this.xml.writeDTD(String.format("<!DOCTYPE %s>", this.rootElement));
			this.xml.writeStartElement(this.rootElement);
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
		} catch (JAXBException | FileNotFoundException e) {
			throw new XMLStreamException("Exception caught while initializing the XML stream", e);
		} finally {
			this.initialized = ok;
			if (!ok) {
				close();
				try {
					FileUtils.forceDelete(this.target);
				} catch (IOException e) {
					// Ignore...
				}
			}
		}
	}

	protected String getVersion() {
		return "1.0";
	}

	protected String getEncoding() {
		return Charset.defaultCharset().name();
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
		if ((objects == null) || (objects.length == 0)) { return 0; }
		return marshal(Arrays.asList(objects));
	}

	public synchronized long marshal(Collection<Object> objects) throws JAXBException, XMLStreamException {
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

	public synchronized void end() throws XMLStreamException {
		assertOpen();
		this.xml.writeEndDocument();
		flush();
	}

	private void closeXml() {
		try {
			this.xml.close();
		} catch (XMLStreamException e) {
			// Log it...???
		}
	}

	@Override
	public synchronized void close() {
		if (isClosed()) { return; }
		try {
			if (this.initialized) {
				try {
					end();
				} catch (XMLStreamException e) {
					// Log it...???
				} finally {
					closeXml();
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