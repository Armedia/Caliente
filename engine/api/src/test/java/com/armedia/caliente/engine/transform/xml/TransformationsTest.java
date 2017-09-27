package com.armedia.caliente.engine.transform.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import com.armedia.commons.utilities.XmlTools;

public class TransformationsTest {

	private InputStream getXml(String name) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
	}

	@Test
	public void testUnmarshall() throws IOException, JAXBException {
		try (InputStream in = getXml("transformations-test-1.xml")) {
			Transformations xform = XmlTools.unmarshal(Transformations.class, "transformations.xsd", in);
			XmlTools.marshal(xform, "transformations.xsd", System.out);
		}
	}

}