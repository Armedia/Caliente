/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.tools.datasource;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;

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