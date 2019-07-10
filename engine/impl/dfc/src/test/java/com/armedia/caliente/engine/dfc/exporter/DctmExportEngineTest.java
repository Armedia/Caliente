/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.engine.dfc.exporter;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dfc.DctmEngineTest;
import com.armedia.caliente.engine.dfc.DctmSetting;
import com.armedia.caliente.engine.exporter.ExportSetting;
import com.armedia.commons.utilities.CfgTools;

public class DctmExportEngineTest extends DctmEngineTest {

	@BeforeEach
	public void setUp() throws Exception {
		this.cmfObjectStore.clearAllObjects();
		this.streamStore.clearAllStreams();
	}

	@Test
	public void test() throws Exception {
		DctmExportEngineFactory factory = new DctmExportEngineFactory();

		Map<String, String> settings = new HashMap<>();
		settings.put(DctmSetting.DOCBASE.getLabel(), "documentum");
		settings.put(DctmSetting.USERNAME.getLabel(), "dmadmin2");
		settings.put(DctmSetting.PASSWORD.getLabel(), "XZ6ZkrcrHEg=");
		settings.put(ExportSetting.FROM.getLabel(), "dm_sysobject where folder('/CMSMFTests', DESCEND)");

		factory.newInstance(this.output, null, this.baseData, this.cmfObjectStore, this.streamStore,
			new CfgTools(settings)).run(null);
	}

}