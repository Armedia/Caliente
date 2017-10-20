package com.armedia.caliente.engine.alfresco.bi.importer.model;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AlfrescoSchemaTest {

	private static final Pattern TYPE_MAPPING_PARSER = Pattern.compile("^([^\\[]+)(?:\\[(.*)\\])?$");

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
			cl.getResource("systemModel.xml"), //
			cl.getResource("contentModel.xml"), //
			cl.getResource("cmisModel.xml"), //
			cl.getResource("calienteBaseModel.xml"), //
			cl.getResource("calienteDctmModel.xml"), //
			cl.getResource("jsapModel.xml"), //
		};
		List<URI> urlList = new ArrayList<>();
		for (URL u : urls) {
			urlList.add(u.toURI());
		}
		AlfrescoSchema schema = new AlfrescoSchema(urlList);
		/*
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
				System.out.printf("\t\t[%s]=[%s:%s] from %s [%s]%n", attribute.name, attribute.multiple ? "R" : "S",
					attribute.type.name(), attribute.declaration.getClass().getSimpleName().toLowerCase(),
					attribute.declaration.name);
			}
		}
		*/
		System.out.printf("Types%n");
		for (String typeName : new TreeSet<>(schema.getTypeNames())) {
			SchemaMember<?> type = schema.getType(typeName);
			SchemaMember<?> parent = type.getParent();
			String parentStr = (parent == null ? "" : String.format(" extends [%s]", parent.name));
			System.out.printf("\tSchema for type [%s]%s%n", typeName, parentStr);
			if (!type.getMandatoryAspects().isEmpty()) {
				System.out.printf("\t\tMandatory Aspects : %s%n", new TreeSet<>(type.getMandatoryAspects()));
			}
			for (String attributeName : new TreeSet<>(type.getAllAttributeNames())) {
				SchemaAttribute attribute = type.getAttribute(attributeName);
				System.out.printf("\t\t[%s]=[%s:%s] from %s [%s]%n", attribute.name, attribute.multiple ? "R" : "S",
					attribute.type.name(), attribute.declaration.getClass().getSimpleName().toLowerCase(),
					attribute.declaration.name);
			}
		}
	}

	@Test
	public void test2() throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL[] urls = {
			cl.getResource("systemModel.xml"), //
			cl.getResource("contentModel.xml"), //
			cl.getResource("cmisModel.xml"), //
			cl.getResource("calienteBaseModel.xml"), //
			cl.getResource("calienteDctmModel.xml"), //
			cl.getResource("jsapModel.xml"), //
		};
		List<URI> urlList = new ArrayList<>();
		for (URL u : urls) {
			urlList.add(u.toURI());
		}
		AlfrescoSchema schema = new AlfrescoSchema(urlList);

		Properties p = new Properties();
		InputStream propIn = cl.getResourceAsStream("jsap-type-map.xml");
		Assert.assertNotNull(propIn);
		try {
			p.loadFromXML(propIn);
		} finally {
			IOUtils.closeQuietly(propIn);
		}

		for (String s : new TreeSet<>(p.stringPropertyNames())) {
			if (!s.startsWith("j")) {
				continue;
			}
			final String m = p.getProperty(s);
			Assert.assertFalse(s, StringUtils.isEmpty(m));
			Matcher matcher = AlfrescoSchemaTest.TYPE_MAPPING_PARSER.matcher(m);
			Assert.assertTrue(s, matcher.matches());

			final String typeName = matcher.group(1);
			String a = matcher.group(2);
			Collection<String> aspects = Collections.emptyList();
			if (a != null) {
				aspects = Arrays.asList(a.split(","));
			}
			AlfrescoType type = schema.buildType(typeName, aspects);
			Assert.assertNotNull(type);
			for (String attributeName : new TreeSet<>(type.getAttributeNames())) {
				if (attributeName.startsWith("cm:") || attributeName.startsWith("sys:")
					|| attributeName.startsWith("arm:")) {
					continue;
				}
				SchemaAttribute attribute = type.getAttribute(attributeName);
				System.out.printf("%s\t%s%n", s, attribute.name.replaceAll("^jsap:", "dctm:"));
			}
		}
	}
}