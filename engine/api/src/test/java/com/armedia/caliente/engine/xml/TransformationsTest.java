package com.armedia.caliente.engine.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import javax.xml.bind.JAXBException;

import org.junit.Test;

public class TransformationsTest {

	private InputStream getXml(String name) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
	}

	@Test
	public void testUnmarshall() throws IOException, JAXBException {

		LinkedList<String> l = new LinkedList<>();
		l.add("0");
		l.add("1");
		l.add("2");
		l.add(3, "x");
		l.remove(0);
		l.add(0, "first");
		l.remove(3);
		l.add(3, "last");

		try (InputStream in = getXml("transformations-test-1.xml")) {
			Transformations.loadFromXML(in).storeToXML(System.out, true);
		}
	}

}