package com.armedia.caliente.engine.dynamic.mapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
	public void testAttributeMapperLoad() throws Exception {
		AttributeMapper.getAttributeMapper(AttributeMapperTest.SCHEMA_SERVICE, "cp:/test-mappings.xml", null, false);
	}
}