package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.net.URI;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.ucm.BaseTest;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.model.UcmModel.ObjectHandler;

public class UcmModelTest extends BaseTest {

	@Test
	public void testIterator() throws Throwable {
		SessionWrapper<UcmSession> w = BaseTest.factory.acquireSession();
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
	public void testRecursiveIterator() throws Throwable {
		SessionWrapper<UcmSession> w = BaseTest.factory.acquireSession();
		try {
			UcmSession s = w.getWrapped();

			FolderTreeIterator.Config cfg = new FolderTreeIterator.Config();
			cfg.setPageSize(100);
			cfg.setRecurseShortcuts(true);
			FolderTreeIterator it = new FolderTreeIterator(s, "/", cfg);
			while (it.hasNext()) {
				UcmAttributes att = it.next();
				UcmAtt nameAtt = UcmAtt.fFileName;
				String type = "FILE";
				if (!att.hasAttribute(nameAtt)) {
					type = "FLDR";
					nameAtt = UcmAtt.fFolderName;
				}
				if (UcmModel.isShortcut(att)) {
					type = String.format(">%s", type);
				} else {
					type = String.format(" %s", type);
				}
				String path = att.getString(UcmAtt.$ucmParentPath);
				String name = att.getString(nameAtt);
				if (path == null) {
					path = "";
					name = "";
				} else if (path.equals("/")) {
					path = "";
				}
				name = String.format("%s/%s", path, name);
				System.out.printf("Item [%03d] (depth %d): %s %s%n", it.getCurrentPos(), it.getCurrentDept(), type,
					name);
				// System.out.printf("%s%n", StringUtils.repeat('-', 40));
				// dumpObject(1, att);
			}
		} catch (UcmServiceException e) {
			handleException(e.getCause());
		} finally {
			w.close();
		}
	}

	@Test
	public void testModelRecursiveIteration() throws Throwable {
		SessionWrapper<UcmSession> w = BaseTest.factory.acquireSession();
		try {
			UcmSession s = w.getWrapped();

			UcmModel m = new UcmModel();
			UcmFolder f = s.getFolder("/");
			m.iterateFolderContentsRecursive(s, f.getURI(), true, new ObjectHandler() {
				@Override
				public void handleObject(UcmSession session, int pos, URI objectUri, UcmFSObject object) {
					System.out.printf("Item [%03d] = [%s]%n", pos, object.getPath());
				}
			});
		} catch (UcmServiceException e) {
			handleException(e.getCause());
		} finally {
			w.close();
		}
	}

	@Test
	public void testResolvePath() throws Exception {
		String[] paths = {
			"/Enterprise Libraries", "/Shortcut To Test Folder", "/Test Folder", "/Users",
			"/Caliente 3.0 Concept Document v4.0.docx", "/non-existent-path"
		};

		SessionWrapper<UcmSession> w = BaseTest.factory.acquireSession();
		try {
			UcmSession s = w.getWrapped();
			UcmModel model = new UcmModel();

			processPaths(model, s, paths);
			// This call is to verify that the caching is being done...
			processPaths(model, s, paths);
		} finally {
			w.close();
		}
	}

	private void processPaths(UcmModel model, UcmSession s, String... paths) throws Exception {
		for (String p : paths) {
			try {
				URI uri = model.resolvePath(s, p);
				System.out.printf("[%s] -> [%s]%n", p, uri);
				if (UcmModel.isFileURI(uri)) {
					UcmFile f = model.getFile(s, p);

					System.out.printf("\tRevisions:%n");
					System.out.printf("\t%s%n", StringUtils.repeat('-', 40));
					for (UcmRevision r : model.getFileHistory(s, f)) {
						System.out.printf("\t\t[%d] = %s (%s)%n", r.getRevisionId(), r.getRevLabel(), r.getId());
						UcmFile R = model.getFileRevision(s, r);
						System.out.printf("\t\t\tACT  = [dID=%s, dRevLabel=%s, dDocName=%s]%n", R.getRevisionId(),
							R.getRevisionLabel(), R.getContentId());
						System.out.printf("\t\t\tGUID = %s%n", R.getUniqueURI());
						System.out.printf("\t\t\tCNAME= %s%n", R.getName());
						System.out.printf("\t\t\tRNAME= %s%n", R.getRevisionName());
						System.out.printf("\t\t\tSIZE = %d%n", R.getSize());
						try (InputStream in = R.getInputStream(s)) {
							System.out.printf("\t\t\tSUM  = %s%n", new String(Hex.encodeHex(DigestUtils.sha256(in))));
						} catch (Exception e) {
							e.printStackTrace(System.err);
						}
					}
				} else {
					model.getFolder(s, p);
				}
			} catch (Exception e) {
				handleException(e);
			}
		}
	}

	@Test
	public void testFsObject() throws Exception {
		String[] paths = {
			"/", "/Enterprise Libraries", "/Shortcut To Test Folder", "/Test Folder", "/Users",
			"/Caliente 3.0 Concept Document v4.0.docx"
		};

		SessionWrapper<UcmSession> w = BaseTest.factory.acquireSession();
		try {
			UcmSession s = w.getWrapped();
			UcmModel model = new UcmModel();
			for (String p : paths) {
				try {
					URI uri = model.resolvePath(s, p);
					System.out.printf("[%s] -> [%s]%n", p, uri);
					UcmFSObject o = null;
					if (UcmModel.isFileURI(uri)) {
						o = model.getFile(s, p);
					} else {
						o = model.getFolder(s, p);
					}
					try {
						UcmFolder parent = o.getParentFolder(s);
						System.out.printf("\tparent = [%s] -> [%s]%n", parent.getPath(), parent.getURI());
					} catch (UcmObjectNotFoundException e) {
						// There is no parent!!
						System.out.printf("\tno parent%n");
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
					System.err.flush();
				}
			}
		} finally {
			w.close();
		}

	}
}