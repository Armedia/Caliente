package com.armedia.caliente.tools.datasource;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.caliente.tools.datasource.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;

public class TestDSL_2_Dupe extends DataSourceLocator {

	public static final String TYPE = TestDSL_2.TYPE;

	public TestDSL_2_Dupe() {
		TestDSLCommon.registerInstance(TestDSL_2_Dupe.TYPE, this);
	}

	@Override
	public boolean supportsLocationType(String locationType) {
		return StringUtils.equalsIgnoreCase(TestDSL_2_Dupe.TYPE, locationType);
	}

	@Override
	public DataSourceDescriptor<?> locateDataSource(CfgTools settings) throws Exception {
		return null;
	}

}