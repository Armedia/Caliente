package com.armedia.caliente.tools.datasource;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.caliente.tools.datasource.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;

public class TestDSL_3_Dupe extends DataSourceLocator {

	public static final String TYPE = TestDSL_3.TYPE;

	public TestDSL_3_Dupe() {
		TestDSLCommon.registerInstance(TestDSL_3_Dupe.TYPE, this);
	}

	@Override
	public boolean supportsLocationType(String locationType) {
		return StringUtils.equalsIgnoreCase(TestDSL_3_Dupe.TYPE, locationType);
	}

	@Override
	public DataSourceDescriptor<?> locateDataSource(CfgTools settings) throws Exception {
		return null;
	}

}