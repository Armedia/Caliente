package com.armedia.caliente.engine.alfresco.bulk.common;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlProperties {

	private static final String PROPS_DTD_URI = "http://java.sun.com/dtd/properties.dtd";

	public static void savePropertiesToXML(Properties properties, OutputStream outputStream, String comment)
		throws IOException {
		XmlProperties.savePropertiesToXML(properties, outputStream, comment, Charset.defaultCharset());
	}

	public static void savePropertiesToXML(Properties properties, OutputStream outputStream, String comment,
		String encoding) throws IOException {
		Charset charset = (encoding != null ? Charset.forName(encoding) : Charset.defaultCharset());
		XmlProperties.savePropertiesToXML(properties, outputStream, comment, charset);
	}

	public static void savePropertiesToXML(Properties properties, OutputStream outputStream, String comment,
		Charset charset) throws IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			assert (false);
		}
		Document doc = db.newDocument();
		// FIX for XML values with &#XX; entities not being recognized in XML 1.0...
		doc.setXmlVersion("1.1");
		doc.setXmlStandalone(false);
		Element propertiesElement = (Element) doc.appendChild(doc.createElement("properties"));

		if (comment != null) {
			Element comments = (Element) propertiesElement.appendChild(doc.createElement("comment"));
			comments.appendChild(doc.createTextNode(comment));
		}

		synchronized (properties) {
			for (String key : properties.stringPropertyNames()) {
				Element entry = (Element) propertiesElement.appendChild(doc.createElement("entry"));
				entry.setAttribute("key", key);
				entry.appendChild(doc.createTextNode(properties.getProperty(key)));
			}
		}
		XmlProperties.writeDocument(doc, outputStream, charset);
	}

	private static void writeDocument(Document doc, OutputStream os, Charset charset) throws IOException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = null;
		try {
			t = tf.newTransformer();
			t.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, XmlProperties.PROPS_DTD_URI);
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty(OutputKeys.METHOD, "xml");
			t.setOutputProperty(OutputKeys.ENCODING, charset.name());
		} catch (TransformerConfigurationException e) {
			throw new IOException("Failed to configure the XML transformer", e);
		}

		DOMSource doms = new DOMSource(doc);
		StreamResult sr = new StreamResult(os);
		try {
			t.transform(doms, sr);
		} catch (TransformerException e) {
			throw new IOException("Failed to write the XML properties", e);
		}
	}
}