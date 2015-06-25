package com.armedia.cmf.engine.mapper;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfTypeMapper.TypeSpec;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class XmlTypeMapperFactoryTest {

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
		settings.put(XmlTypeMapperFactory.Setting.MAPPING_FILE.getLabel(), "type-mappings.xml");
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

			TypeSpec mapping = mapper.mapType(CmfType.DOCUMENT, src);
			Assert.assertNotNull(mapping);
			Assert.assertEquals(CmfType.DOCUMENT, mapping.getBaseType());
			Assert.assertEquals(Tools.coalesce(tgt, src), mapping.getSubType());
		}
	}
}