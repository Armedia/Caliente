package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
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
			UcmSession s = w.get();

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
			UcmSession s = w.get();

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
				String path = att.getString(UcmAtt.cmfParentPath);
				String name = att.getString(nameAtt);
				if (path == null) {
					path = "";
					name = "";
				} else if (path.equals("/")) {
					path = "";
				}
				name = String.format("%s/%s", path, name);
				String shortcut = UcmModel.isShortcut(att)
					? String.format(" | target=%s ", att.getString(UcmAtt.fTargetGUID))
					: "";
				URI uri = UcmModel.getURI(att);
				String data = String.format("{ uri=%s | guid=%s%s }", uri,
					att.getString(UcmModel.isFileURI(uri) ? UcmAtt.dDocName : UcmAtt.fFolderGUID), shortcut);
				System.out.printf("%s %s : %s%n", type, name, data);
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
			UcmSession s = w.get();

			UcmModel m = new UcmModel();
			UcmFolder f = s.getFolder("/");
			m.iterateFolderContentsRecursive(s, f.getURI(), true, new ObjectHandler() {
				@Override
				public void handleObject(UcmSession session, long pos, URI objectUri, UcmFSObject object) {
					String type = (object.getType() == UcmObjectType.FILE ? "FILE" : "FLDR");
					if (object.isShortcut()) {
						type = String.format(">%s", type);
					} else {
						type = String.format(" %s", type);
					}
					String shortcut = object.isShortcut() ? String.format(" | target=%s ", object.getTargetGUID()) : "";
					String data = String.format("{ uri=%s | guid=%s%s }", object.getURI(),
						object.getString(object.getType() == UcmObjectType.FILE ? UcmAtt.dDocName : UcmAtt.fFolderGUID),
						shortcut);
					System.out.printf("%s %s : %s%n", type, object.getPath(), data);
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
			"/Caliente 3.0 Concept Document v4.0.docx"
		};

		SessionWrapper<UcmSession> w = BaseTest.factory.acquireSession();
		try {
			UcmSession s = w.get();
			UcmModel model = new UcmModel();

			processPaths(model, s, paths);
			// This call is to verify that the caching is being done...
			processPaths(model, s, paths);

			try {
				String path = String.format("/%s-non-existent-path-%s", UUID.randomUUID().toString(),
					UUID.randomUUID().toString());
				processPaths(model, s, path);
				Assert.fail(String.format("Did not fail seeking non-existent path [%s]", path));
			} catch (UcmObjectNotFoundException e) {
				// all is well
			}
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
			UcmSession s = w.get();
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
					UcmFolder parent = o.getParentFolder(s);
					if (parent == null) {
						System.out.printf("\tno parent%n");
					} else {
						System.out.printf("\tparent = [%s] -> [%s]%n", parent.getPath(), parent.getURI());
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

	@Test
	public void testFullRecursion() throws Exception {
		SessionWrapper<UcmSession> w = BaseTest.factory.acquireSession();
		try {
			UcmSession s = w.get();
			UcmModel model = new UcmModel();
			UcmFolder root = model.getRootFolder(s);
			model.iterateFolderContentsRecursive(s, root, false, new ObjectHandler() {
				@Override
				public void handleObject(UcmSession session, long pos, URI objectUri, UcmFSObject object) {
					UcmAtt guidAtt = (object.getType() == UcmObjectType.FILE ? UcmAtt.dDocName : UcmAtt.fFolderGUID);
					System.out.printf("[%s] -> [%s] (GUID:%s)%n", object.getPath(), objectUri,
						object.getString(guidAtt));
					try {
						UcmFolder parent = object.getParentFolder(session);
						if (parent == null) {
							System.out.printf("\tno parent%n");
						} else {
							System.out.printf("\tparent = [%s] -> [%s]%n", parent.getPath(), parent.getURI());
							if (object.isShortcut()) {
								System.out.printf("\t---> [%s]%n", object.getTargetGUID());
							}
						}
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
			});
		} finally {
			w.close();
		}
	}

	@Test
	public void testFile() throws Exception {
		SessionWrapper<UcmSession> w = BaseTest.factory.acquireSession();
		try {
			UcmSession s = w.get();
			UcmModel model = new UcmModel();

			try {
				model.getFile(s, "/");
				Assert.fail("Did not fail seeking a folder");
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}

			model.getFile(s, "/Caliente 3.0 Concept Document v4.0.docx");
		} finally {
			w.close();
		}

	}

	@Test
	public void testFolder() throws Exception {
		SessionWrapper<UcmSession> w = BaseTest.factory.acquireSession();
		try {
			UcmSession s = w.get();
			UcmModel model = new UcmModel();

			model.getFolder(s, "/");

			try {
				model.getFolder(s, "/Caliente 3.0 Concept Document v4.0.docx");
				Assert.fail("Did not fail seeking a file");
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		} finally {
			w.close();
		}
	}

	@Test
	public void testGetFile() throws Exception {
		SessionWrapper<UcmSession> w = BaseTest.factory.acquireSession();
		try {
			UcmSession s = w.get();
			UcmModel model = new UcmModel();

			model.getFile(s, "/Test Folder/Second Level Folder/Good Idea.jpg");

			try {
				model.getFile(s, "/");
				Assert.fail("Did not fail seeking a folder");
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		} finally {
			w.close();
		}
	}

	@Test
	public void testSearchResults() throws Exception {
		String query = "<not>\n(\ndID\n<matches>\n`-1`\n)\n              {   dID                }         \n    [   3    ,    5     / 2 ]\n\n";
		SessionWrapper<UcmSession> w = BaseTest.factory.acquireSession();
		try {
			UcmSession s = w.get();
			UcmModel model = new UcmModel();

			model.iterateDocumentSearchResults(s, query, 10000, new ObjectHandler() {
				@Override
				public void handleObject(UcmSession session, long pos, URI objectUri, UcmFSObject object) {
					System.out.printf("Got file # %02d: [%s](%s)%n", pos, object.getPath(), object.getUniqueURI());
				}
			});
		} finally {
			w.close();
		}
	}
}