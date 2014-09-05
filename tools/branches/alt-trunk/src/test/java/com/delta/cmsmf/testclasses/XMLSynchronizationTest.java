package com.delta.cmsmf.testclasses;

import java.beans.XMLEncoder;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class XMLSynchronizationTest {

	/**
	 * @param args
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException {

		XMLSynchronizationTest.testXMLSynchronization();
	}

	/**
	 * Test xml synchronization.
	 * 
	 * @throws FileNotFoundException
	 *             the file not found exception
	 */
	private static void testXMLSynchronization() throws FileNotFoundException {
		Map<String, String> testMap = new HashMap<String, String>();
		testMap.put("key1", "Value1");
		testMap.put("Key2", "Value2");
		XMLEncoder xEnc = new XMLEncoder(new FileOutputStream("c:/TempFile"));
		xEnc.writeObject(testMap);
		xEnc.close();

	}

}
