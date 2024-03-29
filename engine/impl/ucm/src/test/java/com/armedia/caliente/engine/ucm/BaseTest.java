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
package com.armedia.caliente.engine.ucm;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.armedia.caliente.engine.common.SessionWrapper;
import com.armedia.caliente.engine.ucm.model.UcmAttributes;
import com.armedia.caliente.engine.ucm.model.UcmServiceException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.function.CheckedConsumer;

import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.protocol.ServiceException;
import oracle.stellent.ridc.protocol.ServiceResponse;
import oracle.stellent.ridc.protocol.ServiceResponse.ResponseType;

public class BaseTest {

	private static final String NULL = "<null>";

	protected static UcmSessionFactory factory = null;

	@BeforeAll
	public static final void setUpClass() throws Exception {
		CmfCrypt crypto = CmfCrypt.DEFAULT;
		Map<String, String> settingsMap = new TreeMap<>();
		CfgTools settings = new CfgTools(settingsMap);

		settingsMap.put(UcmSessionSetting.USER.getLabel(), "weblogic");
		settingsMap.put(UcmSessionSetting.PASSWORD.getLabel(), "system01");
		settingsMap.put(UcmSessionSetting.HOST.getLabel(), "armdec6aapp06.dev.armedia.com");

		BaseTest.factory = new UcmSessionFactory(settings, crypto);
	}

	public ServiceResponse callService(String service) throws Exception {
		return callService(service, null);
	}

	public ServiceResponse callService(String service, CheckedConsumer<DataBinder, UcmServiceException> prep)
		throws Exception {
		final SessionWrapper<UcmSession> w = BaseTest.factory.acquireSession();
		try {
			ServiceResponse r = w.get().callService(service, prep);
			if (r.getResponseType() == ResponseType.BINDER) {
				dumpBinder(service, r.getResponseAsBinder());
			}
			return r;
		} catch (final Exception e) {
			handleException(e);
			throw e;
		} finally {
			w.close();
		}
	}

	protected final <E extends Throwable> void handleException(E e) throws E {
		if (e == null) { return; }
		Throwable t = e;
		while (t != null) {
			if (ServiceException.class.isInstance(t)) {
				ServiceException se = ServiceException.class.cast(t);
				dumpBinder(se.getClass().getSimpleName(), se.getBinder());
				break;
			}
			t = t.getCause();
			if (t == e) {
				break;
			}
		}
		throw e;
	}

	@AfterAll
	public static final void closeClass() throws Exception {
		try {
			if (BaseTest.factory != null) {
				BaseTest.factory.close();
			}
		} finally {
			BaseTest.factory = null;
		}
	}

	protected final void dumpBinder(String label, DataBinder binder) {
		System.out.printf("Binder %s%n", label);
		System.out.printf("%s%n", StringUtils.repeat('-', 80));
		System.out.printf("Local Data%n");
		System.out.printf("%s%n", StringUtils.repeat('-', 60));
		dumpObject(1, binder.getLocalData());
		System.out.printf("Result Sets%n");
		System.out.printf("%s%n", StringUtils.repeat('-', 60));
		for (String rs : binder.getResultSetNames()) {
			dumpMap(rs, binder.getResultSet(rs));
		}
	}

	protected final void dumpObject(int indent, DataObject o) {
		final String indentStr = StringUtils.repeat('\t', indent);
		for (String s : new TreeSet<>(o.keySet())) {
			Object v = o.get(s);
			if (v == null) {
				v = BaseTest.NULL;
			} else {
				v = String.format("[%s]", v);
			}
			System.out.printf("%s[%s] -> %s%n", indentStr, s, v);
		}
	}

	protected final void dumpObject(int indent, UcmAttributes o) {
		final String indentStr = StringUtils.repeat('\t', indent);
		Map<String, CmfValue> m = o.getData();
		for (String s : new TreeSet<>(m.keySet())) {
			CmfValue v = m.get(s);
			String V = null;
			if (v == null) {
				V = BaseTest.NULL;
			} else {
				V = String.format("[%s]", v.asString());
			}
			System.out.printf("%s[%s] -> %s%n", indentStr, s, V);
		}
	}

	protected final void dumpMap(String label, DataResultSet map) {
		if (map == null) {
			System.out.printf("WARNING: Map [%s] is null", label);
			return;
		}
		System.out.printf("\tMap contents: %s%n", label);
		System.out.printf("\t%s%n", StringUtils.repeat('-', 50));
		int i = 0;
		for (DataObject o : map.getRows()) {
			System.out.printf("\t\tItem [%d]:%n", i++);
			System.out.printf("\t\t%s%n", StringUtils.repeat('-', 40));
			dumpObject(3, o);
		}
		System.out.printf("%n");
	}

	/*
	@Test
	public void test() throws Exception {
		final ExportEngine<?, ?, ?, ?, ?, ?> engine = UcmExportEngine.getExportEngine();
		Logger output = LoggerFactory.getLogger("console");
	
		Map<String, String> settings = new TreeMap<>();
		settings.put(UcmSessionSetting.ATOMPUB_URL.getLabel(),
			"http://armedia-vm.rivera.prv/alfresco/api/-default-/public/cmis/versions/1.0/atom");
		settings.put(UcmSessionSetting.USER.getLabel(), "admin");
		settings.put(UcmSessionSetting.PASSWORD.getLabel(), "123");
		settings.put(UcmSessionSetting.REPOSITORY_ID.getLabel(), "-default-");
		// settings.put(CmisSetting.EXPORT_QUERY.getLabel(), "SELECT * FROM cmis:document");
		settings.put(CmisSetting.EXPORT_PATH.getLabel(), "/Shared");
		settings.put(CmisSetting.EXPORT_PAGE_SIZE.getLabel(), "5");
	
		CmfObjectStore<?> objectStore = CmfStores.getObjectStore("default");
		objectStore.clearProperties();
		objectStore.clearAllObjects();
		objectStore.clearAttributeMappings();
		CmfContentStore<?, ?> contentStore = CmfStores.getContentStore("default");
		contentStore.clearProperties();
		contentStore.clearAllStreams();
		engine.runExport(output, null, objectStore, contentStore, settings);
	}
	*/
}