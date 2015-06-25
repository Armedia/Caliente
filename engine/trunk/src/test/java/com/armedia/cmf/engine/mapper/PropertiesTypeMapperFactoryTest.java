package com.armedia.cmf.engine.mapper;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.commons.utilities.CfgTools;

public class PropertiesTypeMapperFactoryTest {

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
	public void testGetMapperInstance() throws Exception {
		Map<String, String> settings = new HashMap<String, String>();
		settings.put(PropertiesTypeMapperFactory.Setting.MAPPING_FILE.getLabel(), "type-mappings.xml");
		CfgTools cfg = new CfgTools(settings);
		CmfTypeMapper mapper = CmfTypeMapper.getTypeMapper("xml", cfg);

		String[][] arr = {
			{
				"a", "b"

			}, {
				"b", "c"
			}, {
				"c", "d"
			}, {
				"d", null
			}
		};

		for (String[] s : arr) {
			final String src = s[0];
			final String tgt = s[1];

			String mapping = mapper.mapType(src);
			if (tgt == null) {
				Assert.assertNull(mapping);
			} else {
				Assert.assertNotNull(mapping);
				Assert.assertEquals(tgt, mapping);
			}
		}
	}
}