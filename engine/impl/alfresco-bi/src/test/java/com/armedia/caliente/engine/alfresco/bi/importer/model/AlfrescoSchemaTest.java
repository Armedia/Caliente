/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.alfresco.bi.importer.model;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AlfrescoSchemaTest {

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	public void setUp() throws Exception {
	}

	@AfterEach
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
	public void testAlfrescoType() throws Exception {
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
		AlfrescoType type = schema.buildType("cm:content");
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
	}
}