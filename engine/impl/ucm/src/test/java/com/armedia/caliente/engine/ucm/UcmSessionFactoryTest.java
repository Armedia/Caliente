package com.armedia.caliente.engine.ucm;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.ucm.model.FolderContentsIterator;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.protocol.ServiceException;
import oracle.stellent.ridc.protocol.ServiceResponse;

public class UcmSessionFactoryTest {

	private static final String NULL = "<null>";

	@Test
	public void test1() throws Exception {
		UcmSessionFactory factory = null;
		CmfCrypt crypto = new CmfCrypt();
		Map<String, String> settingsMap = new TreeMap<>();
		CfgTools settings = new CfgTools(settingsMap);

		settingsMap.put(UcmSessionSetting.USER.getLabel(), "weblogic");
		settingsMap.put(UcmSessionSetting.PASSWORD.getLabel(), "system01");
		settingsMap.put(UcmSessionSetting.HOST.getLabel(), "armdec6aapp06.dev.armedia.com");

		factory = new UcmSessionFactory(settings, crypto);
		SessionWrapper<IdcSession> w = factory.acquireSession();
		IdcSession s = w.getWrapped();

		FolderContentsIterator it = new FolderContentsIterator(s, "/", 3);
		while (it.hasNext()) {
			System.out.printf("Item [%d] (p%d, c%d):%n", it.getCurrentPos(), it.getPageCount(), it.getCurrentInPage());
			System.out.printf("%s%n", StringUtils.repeat('-', 40));
			System.out.printf("\t%s%n", it.next());
		}

		System.out.printf("Base Folder @ [%s]:%n", it.getPath());
		System.out.printf("%s%n", StringUtils.repeat('-', 40));
		System.out.printf("\t%s%n", it.getFolder());
	}

	@Test
	public void test2() throws Exception {
		UcmSessionFactory factory = null;
		CmfCrypt crypto = new CmfCrypt();
		Map<String, String> settingsMap = new TreeMap<>();
		CfgTools settings = new CfgTools(settingsMap);

		settingsMap.put(UcmSessionSetting.USER.getLabel(), "weblogic");
		settingsMap.put(UcmSessionSetting.PASSWORD.getLabel(), "system01");
		settingsMap.put(UcmSessionSetting.HOST.getLabel(), "armdec6aapp06.dev.armedia.com");

		factory = new UcmSessionFactory(settings, crypto);
		SessionWrapper<IdcSession> w = factory.acquireSession();
		IdcSession s = w.getWrapped();

		DataBinder binder = s.createBinder();
		binder.putLocal("IdcService", "FLD_BROWSE");
		binder.putLocal("path", "/");
		binder.putLocal("doCombinedBrowse", "1");
		binder.putLocal("foldersFirst", "1");

		// These two are important for paging...
		binder.putLocal("combinedCount", "100");
		binder.putLocal("combinedStartRow", "0");
		binder.putLocal("doRetrieveTargetInfo", "1");

		// Join the binder and the user context and perform the service call
		ServiceResponse response = s.sendRequest(binder);
		DataBinder responseData = response.getResponseAsBinder();

		for (String rs : responseData.getResultSetNames()) {
			dumpMap(rs, responseData.getResultSet(rs));
		}

		System.out.printf("Local Data%n");
		System.out.printf("%s%n", StringUtils.repeat('-', 80));
		dumpObject(1, responseData.getLocalData());
	}

	@Test
	public void test3() throws Exception {
		UcmSessionFactory factory = null;
		CmfCrypt crypto = new CmfCrypt();
		Map<String, String> settingsMap = new TreeMap<>();
		CfgTools settings = new CfgTools(settingsMap);

		settingsMap.put(UcmSessionSetting.USER.getLabel(), "weblogic");
		settingsMap.put(UcmSessionSetting.PASSWORD.getLabel(), "system01");
		settingsMap.put(UcmSessionSetting.HOST.getLabel(), "armdec6aapp06.dev.armedia.com");

		factory = new UcmSessionFactory(settings, crypto);
		SessionWrapper<IdcSession> w = factory.acquireSession();
		IdcSession s = w.getWrapped();

		DataBinder binder = s.createBinder();
		binder.putLocal("IdcService", "DOC_INFO_BY_NAME");
		binder.putLocal("dDocName", "ARMDEC6AAP9055000001");
		binder.putLocal("includeFileRenditionsInfo", "1");

		// Join the binder and the user context and perform the service call
		ServiceResponse response = s.sendRequest(binder);
		DataBinder responseData = response.getResponseAsBinder();

		for (String rs : responseData.getResultSetNames()) {
			dumpMap(rs, responseData.getResultSet(rs));
		}

		System.out.printf("Local Data%n");
		System.out.printf("%s%n", StringUtils.repeat('-', 80));
		dumpObject(1, responseData.getLocalData());
	}

	@Test
	public void test4() throws Exception {
		UcmSessionFactory factory = null;
		CmfCrypt crypto = new CmfCrypt();
		Map<String, String> settingsMap = new TreeMap<>();
		CfgTools settings = new CfgTools(settingsMap);

		settingsMap.put(UcmSessionSetting.USER.getLabel(), "weblogic");
		settingsMap.put(UcmSessionSetting.PASSWORD.getLabel(), "system01");
		settingsMap.put(UcmSessionSetting.HOST.getLabel(), "armdec6aapp06.dev.armedia.com");

		factory = new UcmSessionFactory(settings, crypto);
		SessionWrapper<IdcSession> w = factory.acquireSession();
		IdcSession s = w.getWrapped();

		DataBinder binder = s.createBinder();
		binder.putLocal("IdcService", "FLD_INFO");
		binder.putLocal("path", "/NOTHING THERE Caliente 3.0 Concept Document v4.0.docx");

		// Join the binder and the user context and perform the service call
		try {
			ServiceResponse response = s.sendRequest(binder);
			DataBinder responseData = response.getResponseAsBinder();

			for (String rs : responseData.getResultSetNames()) {
				dumpMap(rs, responseData.getResultSet(rs));
			}

			System.out.printf("Local Data%n");
			System.out.printf("%s%n", StringUtils.repeat('-', 80));
			dumpObject(1, responseData.getLocalData());
		} catch (IdcClientException e) {
			if (ServiceException.class.isInstance(e)) {
				ServiceException se = ServiceException.class.cast(e);
				dumpBinder(se.getBinder());
			}
			throw e;
		}
	}

	private void dumpBinder(DataBinder binder) {
		for (String rs : binder.getResultSetNames()) {
			dumpMap(rs, binder.getResultSet(rs));
		}
		System.out.printf("Local Data%n");
		System.out.printf("%s%n", StringUtils.repeat('-', 80));
		dumpObject(1, binder.getLocalData());
	}

	private void dumpObject(int indent, DataObject o) {
		final String indentStr = StringUtils.repeat('\t', indent);
		for (String s : new TreeSet<>(o.keySet())) {
			Object v = o.get(s);
			if (v == null) {
				v = UcmSessionFactoryTest.NULL;
			} else {
				v = String.format("[%s]", v);
			}
			System.out.printf("%s[%s] -> %s%n", indentStr, s, v);
		}
	}

	private void dumpMap(String label, DataResultSet map) {
		if (map == null) {
			System.out.printf("WARNING: Map [%s] is null", label);
			return;
		}
		System.out.printf("Map contents: %s%n", label);
		System.out.printf("%s%n", StringUtils.repeat('-', 80));
		int i = 0;
		for (DataObject o : map.getRows()) {
			System.out.printf("\tItem [%d]:%n", i++);
			System.out.printf("%s%n", StringUtils.repeat('-', 40));
			dumpObject(2, o);
		}
		System.out.printf("%n");
	}

}