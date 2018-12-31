package com.armedia.caliente.engine.alfresco.bi.importer.model;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.caliente.engine.alfresco.bi.importer.AlfSchemaService;
import com.armedia.caliente.engine.dynamic.mapper.schema.AttributeDeclaration;
import com.armedia.caliente.engine.dynamic.mapper.schema.ConstructedType;
import com.armedia.caliente.engine.dynamic.mapper.schema.ConstructedTypeFactory;

public class AttributeMapperTest {

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
			cl.getResource("calienteUcmModel.xml"), //
		};
		List<URI> urlList = new ArrayList<>();
		for (URL u : urls) {
			if (u != null) {
				urlList.add(u.toURI());
			}
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
		Set<String> typeNames = new TreeSet<>(schema.getTypeNames());
		typeNames.addAll(schema.getAspectNames());
		typeNames = new LinkedHashSet<>(Arrays.asList("sys:base", "cm:cmobject", "cm:content"));
		for (String typeName : typeNames) {
			SchemaMember<?> type = schema.getType(typeName);
			if (type == null) {
				type = schema.getAspect(typeName);
			}
			SchemaMember<?> parent = type.getParent();
			String parentStr = (parent == null ? "" : String.format(" extends [%s]", parent.name));
			String typeType = type.getClass().getSimpleName().toLowerCase();
			System.out.printf("\tSchema for %s [%s]%s%n", typeType, typeName, parentStr);
			if (!type.getMandatoryAspects().isEmpty()) {
				System.out.printf("\t\tMandatory Aspects : %s%n", new TreeSet<>(type.getMandatoryAspects()));
			}
			for (String attributeName : new TreeSet<>(type.getAllAttributeNames())) {
				SchemaAttribute attribute = type.getAttribute(attributeName);
				if (type.isAttributeInherited(attributeName)) {
					continue;
				}
				String reqFlag = attribute.mandatory.name().substring(0, 1);
				System.out.printf("\t\t%s[%s]=[%s:%s] from %s [%s]%n", reqFlag, attribute.name,
					attribute.multiple ? "R" : "S", attribute.type.name(),
					attribute.declaration.getClass().getSimpleName().toLowerCase(), attribute.declaration.name);
			}
		}
	}

	@Test
	public void testConstructedType() throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL[] urls = {
			cl.getResource("systemModel.xml"), //
			cl.getResource("contentModel.xml"), //
			cl.getResource("cmisModel.xml"), //
			cl.getResource("calienteBaseModel.xml"), //
			cl.getResource("calienteDctmModel.xml"), //
			cl.getResource("calienteUcmModel.xml"), //
		};
		List<URI> urlList = new ArrayList<>();
		for (URL u : urls) {
			if (u != null) {
				urlList.add(u.toURI());
			}
		}
		AlfrescoSchema alfrescoSchema = new AlfrescoSchema(urlList);
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

		AlfSchemaService alfSchemaService = new AlfSchemaService(alfrescoSchema);
		ConstructedTypeFactory constructedTypeFactory = new ConstructedTypeFactory(alfSchemaService);

		Collection<String> secondaries = Arrays.asList("cm:titled", "cm:versionable");

		AlfrescoType type = alfrescoSchema.buildType("cm:content", secondaries);
		System.out.printf("\tSchema for type %s%n", type.getName());
		if (!type.getExtraAspects().isEmpty()) {
			System.out.printf("\t\tExtra Aspects : %s%n", new TreeSet<>(type.getExtraAspects()));
		}
		if (!type.getDeclaredAspects().isEmpty()) {
			System.out.printf("\t\tDeclared Aspects : %s%n", new TreeSet<>(type.getDeclaredAspects()));
		}
		for (String attributeName : new TreeSet<>(type.getAttributeNames())) {
			SchemaAttribute attribute = type.getAttribute(attributeName);
			String reqFlag = attribute.mandatory.name().substring(0, 1);
			System.out.printf("\t\t%s[%s]=[%s:%s] from %s [%s]%n", reqFlag, attribute.name,
				attribute.multiple ? "R" : "S", attribute.type.name(),
				attribute.declaration.getClass().getSimpleName().toLowerCase(), attribute.declaration.name);
		}

		ConstructedType ct = constructedTypeFactory.constructType(alfSchemaService, "cm:content", secondaries);
		System.out.printf("\tConstructed Schema for type %s%n", ct.getName());
		if (!ct.getSecondaries().isEmpty()) {
			System.out.printf("\t\tAspects : %s%n", new TreeSet<>(ct.getSecondaries()));
		}
		for (String attributeName : new TreeSet<>(ct.getAttributeNames())) {
			AttributeDeclaration attribute = ct.getAttribute(attributeName);
			System.out.printf("\t\t%s[%s]=[%s:%s]%n", attribute.required ? "E" : "O", attribute.name,
				attribute.multiple ? "R" : "S", attribute.type.name());
		}
	}
}