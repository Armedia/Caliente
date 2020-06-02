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
package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.common.SessionWrapper;
import com.armedia.caliente.engine.ucm.BaseTest;
import com.armedia.caliente.engine.ucm.UcmSession;

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
			UcmSession session = w.get();

			UcmModel m = new UcmModel();
			UcmFolder f = session.getFolder("/");
			m.iterateFolderContentsRecursive(session, f.getURI(), true, (s, p, u, o) -> {
				String type = (o.getType() == UcmObjectType.FILE ? "FILE" : "FLDR");
				if (o.isShortcut()) {
					type = String.format(">%s", type);
				} else {
					type = String.format(" %s", type);
				}
				String shortcut = o.isShortcut() ? String.format(" | target=%s ", o.getTargetGUID()) : "";
				String data = String.format("{ uri=%s | guid=%s%s }", o.getURI(),
					o.getString(o.getType() == UcmObjectType.FILE ? UcmAtt.dDocName : UcmAtt.fFolderGUID), shortcut);
				System.out.printf("%s %s : %s%n", type, o.getPath(), data);
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
				Assertions.fail(String.format("Did not fail seeking non-existent path [%s]", path));
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
			UcmSession session = w.get();
			UcmModel model = new UcmModel();
			UcmFolder root = model.getRootFolder(session);
			model.iterateFolderContentsRecursive(session, root, false, (s, p, u, o) -> {
				UcmAtt guidAtt = (o.getType() == UcmObjectType.FILE ? UcmAtt.dDocName : UcmAtt.fFolderGUID);
				System.out.printf("[%s] -> [%s] (GUID:%s)%n", o.getPath(), u, o.getString(guidAtt));
				try {
					UcmFolder parent = o.getParentFolder(s);
					if (parent == null) {
						System.out.printf("\tno parent%n");
					} else {
						System.out.printf("\tparent = [%s] -> [%s]%n", parent.getPath(), parent.getURI());
						if (o.isShortcut()) {
							System.out.printf("\t---> [%s]%n", o.getTargetGUID());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
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
				Assertions.fail("Did not fail seeking a folder");
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
				Assertions.fail("Did not fail seeking a file");
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
				Assertions.fail("Did not fail seeking a folder");
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
			UcmSession session = w.get();
			UcmModel model = new UcmModel();

			model.iterateDocumentSearchResults(session, query, 10000,
				(s, p, u, o) -> System.out.printf("Got file # %02d: [%s](%s)%n", p, o.getPath(), o.getUniqueURI()));
		} finally {
			w.close();
		}
	}
}