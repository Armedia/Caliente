package com.armedia.caliente.engine.dynamic.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.Test;

public class XmlBaseTest {

	private InputStream getXml(String name) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
	}

	@Test
	public void testTransformations() throws IOException, JAXBException {
		try (InputStream in = getXml("transformations-test-1.xml")) {
			XmlBase.storeToXML(Transformations.loadFromXML(in), System.out, true);
		}
	}

	@Test
	public void testExternalMetadata() throws IOException, JAXBException {
		try (InputStream in = getXml("external-metadata-test-1.xml")) {
			XmlBase.storeToXML(ExternalMetadata.loadFromXML(in), System.out, true);
		}
	}

	@Test
	public void testFilters() throws IOException, JAXBException {
		try (InputStream in = getXml("filters-test-1.xml")) {
			XmlBase.storeToXML(Filters.loadFromXML(in), System.out, true);
		}
	}
}