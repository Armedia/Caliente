/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.engine.dynamic.mapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.transformer.mapper.AttributeMapper;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaService;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaServiceException;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.TypeDeclaration;

public class AttributeMapperTest {

	private static final SchemaService SCHEMA_SERVICE = new SchemaService() {

		@Override
		public Collection<String> getObjectTypeNames() throws SchemaServiceException {
			return Arrays.asList("sn1", "sn2");
		}

		@Override
		public TypeDeclaration getObjectTypeDeclaration(String typeName) throws SchemaServiceException {
			return null;
		}

		@Override
		public Collection<String> getSecondaryTypeNames() throws SchemaServiceException {
			return Collections.emptyList();
		}

		@Override
		public TypeDeclaration getSecondaryTypeDeclaration(String secondaryTypeName) throws SchemaServiceException {
			return null;
		}

		@Override
		public void close() throws SchemaServiceException {
		}

	};

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
	public void testAttributeMapperLoad() throws Exception {
		AttributeMapper.getAttributeMapper(AttributeMapperTest.SCHEMA_SERVICE, "cp:/test-mappings.xml", null, false);
	}
}