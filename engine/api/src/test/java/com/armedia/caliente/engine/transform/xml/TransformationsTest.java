package com.armedia.caliente.engine.transform.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import com.armedia.caliente.engine.transform.xml.Transformations;
import com.armedia.commons.utilities.XmlTools;

public class TransformationsTest {

	private InputStream getXml(String name) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
	}

	@Test
	public void testUnmarshall() throws IOException, JAXBException {
		try (InputStream in = getXml("transformations-test-1.xml")) {
			Transformations xform = XmlTools.unmarshal(Transformations.class, in);
			XmlTools.marshal(xform, System.out);
		}
	}

}