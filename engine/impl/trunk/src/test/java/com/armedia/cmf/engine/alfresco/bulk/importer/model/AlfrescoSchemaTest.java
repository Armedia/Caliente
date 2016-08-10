package com.armedia.cmf.engine.alfresco.bulk.importer.model;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AlfrescoSchemaTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAlfrescoSchema() throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL[] urls = {
			cl.getResource("systemModel.xml"), cl.getResource("contentModel.xml"), cl.getResource("cmisModel.xml"),
			cl.getResource("jsap-contentModel.xml"),
		};
		List<URI> urlList = new ArrayList<URI>();
		for (URL u : urls) {
			urlList.add(u.toURI());
		}
		AlfrescoSchema schema = new AlfrescoSchema(urlList);
		System.out.printf("Aspects%n");
		for (String aspectName : schema.getAspectNames()) {
			SchemaMember<?> aspect = schema.getAspect(aspectName);
			SchemaMember<?> parent = aspect.getParent();
			String parentStr = (parent == null ? "" : String.format(" extends [%s]", parent.name));
			System.out.printf("\tAttributes for aspect [%s]%s%n", aspectName, parentStr);
			if (!aspect.getMandatoryAspects().isEmpty()) {
				System.out.printf("\t\tMandatory Aspects : %s%n", aspect.getMandatoryAspects());
			}
			for (String attributeName : aspect.getAllAttributeNames()) {
				SchemaAttribute attribute = aspect.getAttribute(attributeName);
				System.out.printf("\t\t[%s]=[%s:%s]%n", attribute.name, attribute.multiple ? "R" : "S",
					attribute.type.name());
			}
		}
		System.out.printf("Types%n");
		for (String typeName : schema.getTypeNames()) {
			SchemaMember<?> type = schema.getType(typeName);
			SchemaMember<?> parent = type.getParent();
			String parentStr = (parent == null ? "" : String.format(" extends [%s]", parent.name));
			System.out.printf("\tSchema for type [%s]%s%n", typeName, parentStr);
			if (!type.getMandatoryAspects().isEmpty()) {
				System.out.printf("\t\tMandatory Aspects : %s%n", type.getMandatoryAspects());
			}
			for (String attributeName : type.getAllAttributeNames()) {
				SchemaAttribute attribute = type.getAttribute(attributeName);
				System.out.printf("\t\t[%s]=[%s:%s]%n", attribute.name, attribute.multiple ? "R" : "S",
					attribute.type.name());
			}
		}
	}

	@Test
	public void testGetType() {
	}
}