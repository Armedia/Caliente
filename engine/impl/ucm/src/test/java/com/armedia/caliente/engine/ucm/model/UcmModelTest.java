package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionFactory;
import com.armedia.caliente.engine.ucm.UcmSessionSetting;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;

@SuppressWarnings("unused")
public class UcmModelTest {

	private static final String NULL = "<null>";

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
				v = UcmModelTest.NULL;
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
	public void testResolvePath() throws Exception {
		Map<String, String> settingsMap = new TreeMap<>();

		settingsMap.put(UcmSessionSetting.USER.getLabel(), "weblogic");
		settingsMap.put(UcmSessionSetting.PASSWORD.getLabel(), "system01");
		settingsMap.put(UcmSessionSetting.HOST.getLabel(), "armdec6aapp06.dev.armedia.com");

		UcmSessionFactory sf = new UcmSessionFactory(new CfgTools(settingsMap), new CmfCrypt());
		UcmModel model = new UcmModel();

		String[] paths = {
			"/Enterprise Libraries", "/Shortcut To Test Folder", "/Test Folder", "/Users",
			"/Caliente 3.0 Concept Document v4.0.docx", "/non-existent-path"
		};

		SessionWrapper<UcmSession> w = sf.acquireSession();
		UcmSession s = w.getWrapped();

		for (String p : paths) {
			try {
				URI uri = model.resolvePath(s, p);
				System.out.printf("[%s] -> [%s]%n", p, uri);
				if (model.isFileURI(uri)) {
					UcmFile f = model.getFile(s, p);

					System.out.printf("\tRevisions:%n");
					System.out.printf("\t%s%n", StringUtils.repeat('-', 40));
					for (UcmRevision r : model.getFileHistory(s, f)) {
						System.out.printf("\t\t[%d] = %s%n", r.getRevisionId(), r.getRevLabel());
						UcmFile R = model.getFileRevision(s, r);
						System.out.printf("\t\t\tGUID = %s%n", R.getObjectGUID());
						System.out.printf("\t\t\tNAME = %s%n", R.getName());
						System.out.printf("\t\t\tSIZE = %d%n", R.getSize());
						try (InputStream in = R.getInputStream(s)) {
							System.out.printf("\t\t\tSUM  = %s%n", new String(Hex.encodeHex(DigestUtils.sha256(in))));
						} catch (Exception e) {
							e.printStackTrace(System.err);
						}
					}
				} else {
					UcmFolder f = model.getFolder(s, p);
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}

		for (String p : paths) {
			try {
				URI uri = model.resolvePath(s, p);
				System.out.printf("[%s] -> [%s]%n", p, uri);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}

}