package com.delta.cmsmf.engine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cms.AbstractTest;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class CmsBrancherTest extends AbstractTest {
	private Logger log = Logger.getLogger(getClass());

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	private IDfDocument createDocument(IDfSession session) throws IOException, DfException {
		session.beginTrans();
		boolean ok = false;
		try {
			IDfDocument document = IDfDocument.class.cast(session.newObject("dm_document"));
			document.setObjectName("testdocument.doc");
			document.setContentType("text");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write("ROOT VERSION (1.0)".getBytes());
			document.setContent(out);
			IDfFolder parent = session.getFolderByPath("/Temp");
			document.link(parent.getObjectId().getId());
			document.save();
			this.log
				.info(String.format("Created version: %s", document.getAllRepeatingStrings("r_version_label", ",")));
			ok = true;
			return document;
		} finally {
			if (ok) {
				session.commitTrans();
			} else {
				session.abortTrans();
			}
		}
	}

	private IDfDocument createMajorRevision(IDfDocument base) throws DfException, IOException {
		return createMajorRevision(base, 1);
	}

	private IDfDocument createMajorRevision(IDfDocument base, int increment) throws DfException, IOException {
		final IDfSession session = base.getSession();
		session.beginTrans();
		boolean ok = false;
		try {
			final String baseId = base.getObjectId().getId();
			final String baseLabel = base.getImplicitVersionLabel();
			// Get the next-to-last number
			StrTokenizer tok = new StrTokenizer(baseLabel, '.');
			List<String> l = tok.getTokenList();
			// Uneven components == illegal version label
			int major = Integer.valueOf(l.get(l.size() - 2)) + Math.max(increment, 1);
			StringBuilder prefix = new StringBuilder();
			for (int i = 0; i < (l.size() - 2); i += 2) {
				prefix.append(l.get(i)).append('.').append(l.get(i + 1)).append('.');
			}
			base.checkout();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			String newVersion = String.format("%s%d.0,CURRENT", prefix, major);
			String content = String.format("major revision (%s)", newVersion);
			out.write(content.getBytes());
			base.setContent(out);
			IDfId newId = base.checkin(false, newVersion);
			base = IDfDocument.class.cast(session.getObject(newId));
			this.log.info(String.format("Committed version: %s from %s",
				base.getAllRepeatingStrings("r_version_label", ","), baseLabel));
			if (!Tools.equals(baseId, base.getAntecedentId().getId())) {
				this.log.warn(String.format("ANTECEDENT ID MISMATCH: Expected %s, but got %s", baseId, base
					.getAntecedentId().getId()));
			}
			ok = true;
			return base;
		} finally {
			if (ok) {
				session.commitTrans();
			} else {
				session.abortTrans();
			}
		}
	}

	private IDfDocument createMinorRevisions(IDfDocument base, int count) throws IOException, DfException {
		return createMinorRevisions(base, count, 1);
	}

	private IDfDocument createMinorRevisions(IDfDocument base, int count, int gap) throws IOException, DfException {
		final IDfSession session = base.getSession();
		session.beginTrans();
		boolean ok = false;
		try {
			String baseLabel = base.getImplicitVersionLabel();
			// Get the next-to-last number
			StrTokenizer tok = new StrTokenizer(baseLabel, '.');
			List<String> l = tok.getTokenList();
			// Uneven components == illegal version label
			int major = Integer.valueOf(l.get(l.size() - 2));
			int minor = Integer.valueOf(l.get(l.size() - 1));
			StringBuilder prefix = new StringBuilder();
			for (int i = 0; i < (l.size() - 2); i += 2) {
				prefix.append(l.get(i)).append('.').append(l.get(i + 1)).append('.');
			}
			gap = Math.max(gap, 1);
			for (int i = 0; i < count; i++) {
				String baseId = base.getObjectId().getId();
				base.checkout();
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				String versionNumber = String.format("%s%d.%d", prefix, major, minor + gap);
				String content = String.format("minor revision (%s)", versionNumber);
				out.write(content.getBytes());
				base.setContent(out);
				IDfId newId = base.checkin(false, gap > 1 ? versionNumber : null);
				IDfDocument child = IDfDocument.class.cast(session.getObject(newId));
				this.log.info(String.format("Committed version: %s from %s",
					child.getAllRepeatingStrings("r_version_label", ","), baseLabel));
				baseLabel = child.getImplicitVersionLabel();
				if (!Tools.equals(baseId, child.getAntecedentId().getId())) {
					this.log.warn(String.format("ANTECEDENT ID MISMATCH: Expected %s, but got %s", baseId, child
						.getAntecedentId().getId()));
				}
				base = child;
				tok = new StrTokenizer(baseLabel, '.');
				l = tok.getTokenList();
				minor = Integer.valueOf(l.get(l.size() - 1));
			}
			ok = true;
			return base;
		} finally {
			if (ok) {
				session.commitTrans();
			} else {
				session.abortTrans();
			}
		}
	}

	private IDfDocument createBranch(IDfDocument base) throws DfException, IOException {
		final IDfSession session = base.getSession();
		session.beginTrans();
		boolean ok = false;
		try {
			String baseId = base.getObjectId().getId();
			IDfId newId = base.branch(base.getImplicitVersionLabel());
			IDfDocument branch = IDfDocument.class.cast(session.getObject(newId));
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			String content = String.format("BRANCH (%s)", branch.getImplicitVersionLabel());
			out.write(content.getBytes());
			branch.setContent(out);
			branch.save();
			this.log.info(String.format("Branched version: %s", branch.getAllRepeatingStrings("r_version_label", ",")));
			if (!Tools.equals(baseId, branch.getAntecedentId().getId())) {
				this.log.warn(String.format("ANTECEDENT ID MISMATCH: Expected %s, but got %s", baseId, branch
					.getAntecedentId().getId()));
			}
			ok = true;
			return branch;
		} finally {
			if (ok) {
				session.commitTrans();
			} else {
				session.abortTrans();
			}
		}
	}

	private void destroyChronicle(IDfDocument base) throws DfException {
		final IDfSession session = base.getSession();
		session.beginTrans();
		boolean ok = false;
		try {
			IDfSysObject.class.cast(session.getObject(base.getChronicleId())).destroyAllVersions();
			ok = true;
		} finally {
			if (ok) {
				session.commitTrans();
			} else {
				session.abortTrans();
			}
		}
	}

	// @Test
	public void testBranching() throws Throwable {
		IDfSession session = acquireTargetSession();
		IDfDocument document = null;
		try {
			document = createDocument(session);
			document = createMinorRevisions(document, 3);

			document = createMajorRevision(document);
			document = createMinorRevisions(document, 3);

			document = createMajorRevision(document);
			IDfDocument branch1 = createBranch(document);
			IDfDocument branch2 = createBranch(document);
			IDfDocument branch3 = createBranch(document);

			document = createMinorRevisions(document, 3);
			branch1 = createMinorRevisions(branch1, 3);
			branch2 = createMinorRevisions(branch2, 3);
			branch3 = createMinorRevisions(branch3, 3);

			document = createMajorRevision(document, 3);
			document = createMinorRevisions(document, 3, 3);
		} finally {
			try {
				if (document != null) {
					destroyChronicle(document);
				}
			} finally {
				releaseTargetSession(session);
			}
		}
	}
}