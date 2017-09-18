package com.armedia.caliente.engine.ucm;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.ucm.UcmSession.RequestPreparation;
import com.armedia.caliente.engine.ucm.model.FolderContentsIterator;
import com.armedia.caliente.engine.ucm.model.UcmAttributes;
import com.armedia.caliente.engine.ucm.model.UcmServiceException;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.protocol.ServiceException;
import oracle.stellent.ridc.protocol.ServiceResponse;
import oracle.stellent.ridc.protocol.ServiceResponse.ResponseType;

public class UcmSessionFactoryTest {

	private static final String NULL = "<null>";

	private static UcmSessionFactory factory = null;

	@BeforeClass
	public static final void setUpClass() throws Exception {
		CmfCrypt crypto = new CmfCrypt();
		Map<String, String> settingsMap = new TreeMap<>();
		CfgTools settings = new CfgTools(settingsMap);

		settingsMap.put(UcmSessionSetting.USER.getLabel(), "weblogic");
		settingsMap.put(UcmSessionSetting.PASSWORD.getLabel(), "system01");
		settingsMap.put(UcmSessionSetting.HOST.getLabel(), "armdec6aapp06.dev.armedia.com");

		UcmSessionFactoryTest.factory = new UcmSessionFactory(settings, crypto);
	}

	public ServiceResponse callService(String service) throws Exception {
		return callService(service, null);
	}

	public ServiceResponse callService(String service, RequestPreparation prep) throws Exception {
		final SessionWrapper<UcmSession> w = UcmSessionFactoryTest.factory.acquireSession();
		try {
			return w.getWrapped().callService(service, prep);
		} catch (final Exception e) {
			handleException(e);
			throw e;
		} finally {
			w.close();
		}
	}

	private final <E extends Throwable> void handleException(E e) throws E {
		if (e == null) { return; }
		if (ServiceException.class.isInstance(e)) {
			ServiceException se = ServiceException.class.cast(e);
			dumpBinder(se.getBinder());
		}
		throw e;
	}

	@AfterClass
	public static final void closeClass() throws Exception {
		try {
			if (UcmSessionFactoryTest.factory != null) {
				UcmSessionFactoryTest.factory.close();
			}
		} finally {
			UcmSessionFactoryTest.factory = null;
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

	private void dumpObject(int indent, UcmAttributes o) {
		final String indentStr = StringUtils.repeat('\t', indent);
		Map<String, String> m = o.getData();
		for (String s : new TreeSet<>(m.keySet())) {
			String v = m.get(s);
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

	@Test
	public void testURI() throws Exception {
		URI uri = new URI("file", "somearbitrarygarbage", "fragmentCrap");
		System.out.printf("%s%n", uri);
		System.out.printf("SCHEME: %s%n", uri.getScheme());
		System.out.printf("SSP   : %s%n", uri.getSchemeSpecificPart());
		System.out.printf("FRAG  : %s%n", uri.getFragment());
	}

	@Test
	public void testIterator() throws Throwable {
		SessionWrapper<UcmSession> w = UcmSessionFactoryTest.factory.acquireSession();
		try {
			UcmSession s = w.getWrapped();

			FolderContentsIterator it = new FolderContentsIterator(s, "/", 3);
			while (it.hasNext()) {
				System.out.printf("Item [%d] (p%d, c%d):%n", it.getCurrentPos(), it.getPageCount(),
					it.getCurrentInPage());
				System.out.printf("%s%n", StringUtils.repeat('-', 40));
				dumpObject(1, it.next());
			}

			System.out.printf("Base Folder @ [%s]:%n", it.getSearchKey());
			System.out.printf("%s%n", StringUtils.repeat('-', 40));
			dumpObject(1, it.getFolder());

			System.out.printf("Local Data@ [%s]:%n", it.getSearchKey());
			System.out.printf("%s%n", StringUtils.repeat('-', 40));
			dumpObject(1, it.getLocalData());
		} catch (UcmServiceException e) {
			handleException(e.getCause());
		} finally {
			w.close();
		}
	}

	@Test
	public void FLD_BROWSE() throws Exception {
		try {
			ServiceResponse response = callService("FLD_BROWSE", new RequestPreparation() {
				@Override
				public void prepareRequest(DataBinder binder) {
					binder.putLocal("path", "/");
					binder.putLocal("doCombinedBrowse", "1");
					binder.putLocal("foldersFirst", "1");
					binder.putLocal("combinedCount", "100");
					binder.putLocal("combinedStartRow", "0");
					binder.putLocal("doRetrieveTargetInfo", "1");
				}
			});
			DataBinder responseData = response.getResponseAsBinder();

			for (String rs : responseData.getResultSetNames()) {
				dumpMap(rs, responseData.getResultSet(rs));
			}

			System.out.printf("Local Data%n");
			System.out.printf("%s%n", StringUtils.repeat('-', 80));
			dumpObject(1, responseData.getLocalData());
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Test
	public void DOC_INFO_BY_NAME() throws Exception {
		try {
			ServiceResponse response = callService("DOC_INFO_BY_NAME", new RequestPreparation() {
				@Override
				public void prepareRequest(DataBinder binder) {
					binder.putLocal("dDocName", "ARMDEC6AAP9055000001");
					binder.putLocal("includeFileRenditionsInfo", "1");
				}
			});
			DataBinder responseData = response.getResponseAsBinder();

			for (String rs : responseData.getResultSetNames()) {
				dumpMap(rs, responseData.getResultSet(rs));
			}

			System.out.printf("Local Data%n");
			System.out.printf("%s%n", StringUtils.repeat('-', 80));
			dumpObject(1, responseData.getLocalData());
		} catch (IdcClientException e) {
			handleException(e);
		}
	}

	@Test
	public void FLD_INFO() throws Exception {
		// Join the binder and the user context and perform the service call
		try {
			ServiceResponse response = callService("FLD_INFO", new RequestPreparation() {
				@Override
				public void prepareRequest(DataBinder binder) {
					binder.putLocal("path", "/Caliente 3.0 Concept Document v4.0.docx");
				}
			});
			DataBinder responseData = response.getResponseAsBinder();

			for (String rs : responseData.getResultSetNames()) {
				dumpMap(rs, responseData.getResultSet(rs));
			}

			System.out.printf("Local Data%n");
			System.out.printf("%s%n", StringUtils.repeat('-', 80));
			dumpObject(1, responseData.getLocalData());
		} catch (IdcClientException e) {
			handleException(e);
		}
	}

	@Test
	public void REV_HISTORY() throws Exception {
		try {
			ServiceResponse response = callService("REV_HISTORY", new RequestPreparation() {
				@Override
				public void prepareRequest(DataBinder binder) {
					binder.putLocal("dID", "5");
					binder.putLocal("includeFileRenditionsInfo", "1");
				}
			});
			DataBinder responseData = response.getResponseAsBinder();

			for (String rs : responseData.getResultSetNames()) {
				dumpMap(rs, responseData.getResultSet(rs));
			}

			System.out.printf("Local Data%n");
			System.out.printf("%s%n", StringUtils.repeat('-', 80));
			dumpObject(1, responseData.getLocalData());
		} catch (IdcClientException e) {
			handleException(e);
		}
	}

	@Test
	public void DOC_INFO() throws Exception {
		String[] ids = {
			"1", "2", "3", "5"
		};
		try {
			for (final String id : ids) {
				ServiceResponse response = callService("DOC_INFO", new RequestPreparation() {
					@Override
					public void prepareRequest(DataBinder binder) {
						binder.putLocal("dID", id);
						binder.putLocal("includeFileRenditionsInfo", "1");
					}
				});
				DataBinder responseData = response.getResponseAsBinder();

				for (String rs : responseData.getResultSetNames()) {
					dumpMap(rs, responseData.getResultSet(rs));
				}

				System.out.printf("Local Data for [%s]%n", id);
				System.out.printf("%s%n", StringUtils.repeat('-', 80));
				dumpObject(1, responseData.getLocalData());
			}
		} catch (IdcClientException e) {
			handleException(e);
		}

	}

	@Test
	public void GET_USERS() throws Exception {
		try {
			ServiceResponse response = callService("GET_USERS", new RequestPreparation() {
				@Override
				public void prepareRequest(DataBinder binder) {
					// binder.putLocal("dGroupName", "Public");
				}
			});
			DataBinder responseData = response.getResponseAsBinder();

			for (String rs : responseData.getResultSetNames()) {
				dumpMap(rs, responseData.getResultSet(rs));
			}

			System.out.printf("Local Data%n");
			System.out.printf("%s%n", StringUtils.repeat('-', 80));
			dumpObject(1, responseData.getLocalData());
		} catch (IdcClientException e) {
			handleException(e);
		}
	}

	@Test
	public void GET_FILE() throws Exception {
		try {
			ServiceResponse response = callService("GET_FILE", new RequestPreparation() {
				@Override
				public void prepareRequest(DataBinder binder) {
					binder.putLocal("dID", "5");
					binder.putLocal("Rendition", "primary");
				}
			});
			if (response.getResponseType() == ResponseType.STREAM) {
				byte[] sha = DigestUtils.sha256(response.getResponseStream());
				int reportedSize = Integer.parseInt(response.getHeader("Content-Length"));
				System.out.printf("SIZE   = %d%n", reportedSize);
				System.out.printf("SHA256 = %s%n", Hex.encodeHexString(sha));
			} else {
				dumpBinder(response.getResponseAsBinder());
			}
		} catch (IdcClientException e) {
			handleException(e);
		}
	}
}