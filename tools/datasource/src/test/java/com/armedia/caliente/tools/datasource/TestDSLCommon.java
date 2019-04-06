package com.armedia.caliente.tools.datasource;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import com.armedia.caliente.tools.datasource.DataSourceLocator;

public class TestDSLCommon {

	protected static final ConcurrentMap<String, ConcurrentMap<String, DataSourceLocator>> TYPE_MAP = new ConcurrentHashMap<>();

	protected static void registerInstance(final String type, final DataSourceLocator dsl) {
		if (dsl == null) { return; }
		ConcurrentMap<String, DataSourceLocator> locators = ConcurrentUtils
			.createIfAbsentUnchecked(TestDSLCommon.TYPE_MAP, type, ConcurrentHashMap::new);
		ConcurrentUtils.createIfAbsentUnchecked(locators, dsl.getClass().getCanonicalName(), () -> dsl);
	}

	protected static void clearInstances() {
		TestDSLCommon.TYPE_MAP.clear();
	}
}