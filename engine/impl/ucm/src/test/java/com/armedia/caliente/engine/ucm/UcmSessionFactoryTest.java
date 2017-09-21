package com.armedia.caliente.engine.ucm;

import java.net.URI;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import com.armedia.caliente.engine.ucm.UcmSession.RequestPreparation;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
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
		callService("FLD_BROWSE", new RequestPreparation() {
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
	}

	@Test
	public void DOC_INFO_BY_NAME() throws Exception {
		callService("DOC_INFO_BY_NAME", new RequestPreparation() {
			@Override
			public void prepareRequest(DataBinder binder) {
				binder.putLocal("dDocName", "ARMDEC6AAP9055000001");
				binder.putLocal("includeFileRenditionsInfo", "1");
			}
		});
	}

	@Test
	public void FLD_INFO() throws Exception {
		callService("FLD_INFO", new RequestPreparation() {
			@Override
			public void prepareRequest(DataBinder binder) {
				binder.putLocal("path", "/");
			}
		});
	}

	@Test
	public void REV_HISTORY() throws Exception {
		callService("REV_HISTORY", new RequestPreparation() {
			@Override
			public void prepareRequest(DataBinder binder) {
				binder.putLocal("dID", "5");
				binder.putLocal("includeFileRenditionsInfo", "1");
			}
		});
	}

	@Test
	public void DOC_INFO() throws Exception {
		String[] ids = {
			"1", "2", "3", "5"
		};
		for (final String id : ids) {
			callService("DOC_INFO", new RequestPreparation() {
				@Override
				public void prepareRequest(DataBinder binder) {
					binder.putLocal("dID", id);
					binder.putLocal("includeFileRenditionsInfo", "1");
				}
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
		ServiceResponse response = callService("GET_FILE", new RequestPreparation() {
			@Override
			public void prepareRequest(DataBinder binder) {
				binder.putLocal("dID", "5");
				binder.putLocal("Rendition", "primary");
			}
		});
		if (response.getResponseType() == ResponseType.STREAM) {
			byte[] sha = DigestUtils.sha256(response.getResponseStream());
			System.out.printf("SIZE   = %d%n", Integer.parseInt(response.getHeader("Content-Length")));
			System.out.printf("SHA256 = %s%n", Hex.encodeHexString(sha));
		}
	}

	@Test
	public void QUERY_GROUP() throws Exception {
		callService("QUERY_GROUP", new RequestPreparation() {
			@Override
			public void prepareRequest(DataBinder binder) {
				binder.putLocal("dGroupName", "Public");
			}
		});
	}
}