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