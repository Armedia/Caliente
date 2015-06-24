package com.armedia.cmf.engine.tools;

import java.util.Map;

import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfTypeMapperFactory;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class PropertiesTypeMapperFactory extends CmfTypeMapperFactory {

	private static class Mapper extends CmfTypeMapper {

		private final Map<String, TypeSpec> mappings;

		private Mapper(Map<String, TypeSpec> mappings) {
			this.mappings = Tools.freezeMap(mappings);
		}

		@Override
		protected TypeSpec getMapping(TypeSpec sourceType) {
			return null;
		}

	}

	// The name or path of the file. Could be a URL as well
	public static String FILE_NAME = "fileName";

	// Search order: classpath, filesystem
	public static String SEARCH_ORDER = "searchOrder";

	public PropertiesTypeMapperFactory() {
		super("properties");
	}

	@Override
	public CmfTypeMapper getMapperInstance(CfgTools cfg) throws Exception {
		return null;
	}

}