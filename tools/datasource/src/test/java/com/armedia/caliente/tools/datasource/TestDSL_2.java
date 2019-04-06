package com.armedia.caliente.tools.datasource;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.caliente.tools.datasource.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;

public class TestDSL_2 extends DataSourceLocator {

	public static final String TYPE = UUID.randomUUID().toString();

	public TestDSL_2() {
		TestDSLCommon.registerInstance(TestDSL_2.TYPE, this);
	}

	@Override
	public boolean supportsLocationType(String locationType) {
		return StringUtils.equalsIgnoreCase(TestDSL_2.TYPE, locationType);
	}

	@Override
	public DataSourceDescriptor<?> locateDataSource(CfgTools settings) throws Exception {
		return null;
	}

}