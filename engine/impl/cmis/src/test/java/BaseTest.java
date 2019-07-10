
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
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.cmis.CmisSessionSetting;
import com.armedia.caliente.engine.cmis.CmisSetting;
import com.armedia.caliente.engine.cmis.exporter.CmisExportEngineFactory;
import com.armedia.caliente.engine.exporter.ExportSetting;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStores;
import com.armedia.commons.utilities.CfgTools;

public class BaseTest {

	@Test
	public void test() throws Exception {
		final CmisExportEngineFactory factory = new CmisExportEngineFactory();
		Logger output = LoggerFactory.getLogger("console");

		Map<String, String> settings = new TreeMap<>();
		settings.put(CmisSessionSetting.ATOMPUB_URL.getLabel(),
			"http://armedia-vm.rivera.prv/alfresco/api/-default-/public/cmis/versions/1.0/atom");
		settings.put(CmisSessionSetting.USER.getLabel(), "admin");
		settings.put(CmisSessionSetting.PASSWORD.getLabel(), "123");
		settings.put(CmisSessionSetting.REPOSITORY_ID.getLabel(), "-default-");
		// settings.put(CmisSetting.EXPORT_QUERY.getLabel(), "SELECT * FROM cmis:document");
		settings.put(ExportSetting.FROM.getLabel(), "/Shared");
		settings.put(CmisSetting.EXPORT_PAGE_SIZE.getLabel(), "5");

		CmfObjectStore<?> objectStore = CmfStores.getObjectStore("default");
		objectStore.clearProperties();
		objectStore.clearAllObjects();
		objectStore.clearAttributeMappings();
		CmfContentStore<?, ?> contentStore = CmfStores.getContentStore("default");
		contentStore.clearProperties();
		contentStore.clearAllStreams();

		factory.newInstance(output, null, contentStore.getRootLocation(), objectStore, contentStore,
			new CfgTools(settings)).run(null);
	}
}