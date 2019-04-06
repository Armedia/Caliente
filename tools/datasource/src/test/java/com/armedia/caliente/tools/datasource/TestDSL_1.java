package com.armedia.caliente.tools.datasource;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.caliente.tools.datasource.DataSourceLocator;
import com.armedia.commons.utilities.CfgTools;

public class TestDSL_1 extends DataSourceLocator {

	public static final String TYPE = UUID.randomUUID().toString();

	public TestDSL_1() {
		TestDSLCommon.registerInstance(TestDSL_1.TYPE, this);
	}

	@Override
	public boolean supportsLocationType(String locationType) {
		return StringUtils.equalsIgnoreCase(TestDSL_1.TYPE, locationType);
	}

	@Override
	public DataSourceDescriptor<?> locateDataSource(CfgTools settings) throws Exception {
		return null;
	}

}