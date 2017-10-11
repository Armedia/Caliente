package com.armedia.caliente.engine.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import com.armedia.caliente.engine.dynamic.xml.ExternalMetadata;
import com.armedia.caliente.engine.dynamic.xml.Transformations;

public class XmlBaseTest {

	private InputStream getXml(String name) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
	}

	@Test
	public void testTransformations() throws IOException, JAXBException {
		try (InputStream in = getXml("transformations-test-1.xml")) {
			Transformations.loadFromXML(in).storeToXML(System.out, true);
		}
	}

	@Test
	public void testExternalMetadata() throws IOException, JAXBException {
		try (InputStream in = getXml("external-metadata-test-1.xml")) {
			ExternalMetadata.loadFromXML(in).storeToXML(System.out, true);
		}
	}

}