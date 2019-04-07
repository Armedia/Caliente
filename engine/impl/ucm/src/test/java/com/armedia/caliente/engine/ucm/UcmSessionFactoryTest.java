package com.armedia.caliente.engine.ucm;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.protocol.ServiceResponse;
import oracle.stellent.ridc.protocol.ServiceResponse.ResponseType;

public class UcmSessionFactoryTest extends BaseTest {

	@Test
	public void testURI() throws Exception {
		URI uri = new URI("idcs://www.nacion.com"); // new URI("file",
													// "somearbitrarygarbage",
													// "fragmentCrap");
		System.out.printf("%s%n", uri);
		System.out.printf("OPAQUE: %s%n", uri.isOpaque());
		System.out.printf("ABSOL : %s%n", uri.isAbsolute());
		System.out.printf("SCHEME: %s%n", uri.getScheme());
		System.out.printf("SSP   : %s%n", uri.getSchemeSpecificPart());
		System.out.printf("AUTH  : %s%n", uri.getAuthority());
		System.out.printf("HOST  : %s%n", uri.getHost());
		System.out.printf("PORT  : %d%n", uri.getPort());
		System.out.printf("PATH  : %s%n", uri.getPath());
		System.out.printf("FRAG  : %s%n", uri.getFragment());
		System.out.printf("QUERY : %s%n", uri.getQuery());
	}

	@Test
	public void FLD_BROWSE() throws Exception {
		callService("FLD_BROWSE", (binder) -> {
			binder.putLocal("path", "/");
			binder.putLocal("doCombinedBrowse", "1");
			binder.putLocal("foldersFirst", "1");
			binder.putLocal("combinedCount", "100");
			binder.putLocal("combinedStartRow", "0");
			binder.putLocal("doRetrieveTargetInfo", "1");
		});
	}

	@Test
	public void DOC_INFO_BY_NAME() throws Exception {
		callService("DOC_INFO_BY_NAME", (binder) -> {
			binder.putLocal("dDocName", "ARMDEC6AAP9055000001");
			binder.putLocal("includeFileRenditionsInfo", "1");
		});
	}

	@Test
	public void FLD_INFO() throws Exception {
		callService("FLD_INFO", (binder) -> binder.putLocal("path", "/"));
	}

	@Test
	public void REV_HISTORY() throws Exception {
		callService("REV_HISTORY", (binder) -> {
			binder.putLocal("dID", "5");
			binder.putLocal("includeFileRenditionsInfo", "1");
		});
	}

	@Test
	public void DOC_INFO() throws Exception {
		String[] ids = {
			"1", "2", "3", "5"
		};
		for (final String id : ids) {
			callService("DOC_INFO", (binder) -> {
				binder.putLocal("dID", id);
				binder.putLocal("includeFileRenditionsInfo", "1");
			});
		}
	}

	@Test
	public void GET_USERS() throws Exception {
		try {
			callService("GET_USERS");
		} catch (IdcClientException e) {
			handleException(e);
		}
	}

	@Test
	public void CONFIG_INFO() throws Exception {
		try {
			callService("CONFIG_INFO");
		} catch (IdcClientException e) {
			handleException(e);
		}
	}

	@Test
	public void GET_FILE() throws Exception {
		ServiceResponse response = callService("GET_FILE", (binder) -> {
			binder.putLocal("dID", "5");
			binder.putLocal("Rendition", "primary");
		});
		if (response.getResponseType() == ResponseType.STREAM) {
			byte[] sha = DigestUtils.sha256(response.getResponseStream());
			System.out.printf("SIZE   = %d%n", Integer.parseInt(response.getHeader("Content-Length")));
			System.out.printf("SHA256 = %s%n", Hex.encodeHexString(sha));
		}
	}

	@Test
	public void QUERY_GROUP() throws Exception {
		callService("QUERY_GROUP", (binder) -> binder.putLocal("dGroupName", "Public"));
	}

	@Test
	public void GET_SEARCH_RESULTS() throws Exception {
		final int pageSize = 999;
		final AtomicInteger currentRow = new AtomicInteger(1);
		final String query = "<not>(dID <matches> `-1`)";
		while (true) {
			ServiceResponse rsp = callService("GET_SEARCH_RESULTS", (binder) -> {
				binder.putLocal("QueryText", query);
				// binder.putLocal("SearchEngineName", "database");
				binder.putLocal("StartRow", String.valueOf(currentRow.get()));
				binder.putLocal("ResultCount", String.valueOf(pageSize));
				binder.putLocal("isAddFolderMetadata", "1");
				binder.putLocal("SortField", "dID");
				binder.putLocal("SortOrder", "Asc");
			});
			DataBinder binder = rsp.getResponseAsBinder();
			DataResultSet results = binder.getResultSet("SearchResults");
			if (results == null) {
				break;
			}
			List<DataObject> rows = results.getRows();
			if ((rows == null) || rows.isEmpty()) {
				break;
			}
			final boolean lastPage = (rows.size() < pageSize);
			currentRow.addAndGet(rows.size());
			if (lastPage) {
				break;
			}
		}
	}

	@Test
	public void FLD_FOLDER_SEARCH() throws Exception {
		callService("FLD_FOLDER_SEARCH", (binder) -> {
			binder.putLocal("QueryText", "<NOT>(fParentGUID <matches> `FLD_ROOT`)");
			binder.putLocal("ResultCount", "1000");
			binder.putLocal("StartRow", "1");
		});
	}
}