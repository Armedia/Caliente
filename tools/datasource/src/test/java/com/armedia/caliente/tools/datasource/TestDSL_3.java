package com.armedia.caliente.tools.datasource;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.caliente.tools.datasource.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;

public class TestDSL_3 extends DataSourceLocator {

	public static final String TYPE = UUID.randomUUID().toString();

	public TestDSL_3() {
		TestDSLCommon.registerInstance(TestDSL_3.TYPE, this);
	}

	@Override
	public boolean supportsLocationType(String locationType) {
		return StringUtils.equalsIgnoreCase(TestDSL_3.TYPE, locationType);
	}

	@Override
	public DataSourceDescriptor<?> locateDataSource(CfgTools settings) throws Exception {
		return null;
	}

}