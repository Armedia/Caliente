package com.delta.cmsmf.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cms.AbstractTest;
import com.delta.cmsmf.cms.CmsAttributes;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class CmsBranchFixerTest extends AbstractTest {
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
			document.setObjectName("Broken History.txt");
			document.setContentType("text");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write("ROOT VERSION (1.0)".getBytes());
			document.setContent(out);
			IDfFolder parent = session.getFolderByPath("/Temp");
			document.link(parent.getObjectId().getId());
			document.save();
			this.log.info(String.format("Created version: %s",
				document.getAllRepeatingStrings(CmsAttributes.R_VERSION_LABEL, ",")));
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
				base.getAllRepeatingStrings(CmsAttributes.R_VERSION_LABEL, ","), baseLabel));
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

	private List<IDfDocument> createMinorRevisions(IDfDocument base, int count) throws IOException, DfException {
		return createMinorRevisions(base, count, 1);
	}

	private List<IDfDocument> createMinorRevisions(IDfDocument base, int count, int gap) throws IOException,
	DfException {
		final IDfSession session = base.getSession();
		session.beginTrans();
		boolean ok = false;
		List<IDfDocument> ret = new ArrayList<IDfDocument>(count);
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
					child.getAllRepeatingStrings(CmsAttributes.R_VERSION_LABEL, ","), baseLabel));
				baseLabel = child.getImplicitVersionLabel();
				if (!Tools.equals(baseId, child.getAntecedentId().getId())) {
					this.log.warn(String.format("ANTECEDENT ID MISMATCH: Expected %s, but got %s", baseId, child
						.getAntecedentId().getId()));
				}
				base = child;
				tok = new StrTokenizer(baseLabel, '.');
				l = tok.getTokenList();
				minor = Integer.valueOf(l.get(l.size() - 1));
				ret.add(child);
			}
			ok = true;
			return ret;
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
			this.log.info(String.format("Branched version: %s",
				branch.getAllRepeatingStrings(CmsAttributes.R_VERSION_LABEL, ",")));
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

	@Test
	public void testBranching() throws Throwable {
		IDfSession session = acquireTargetSession();
		IDfDocument document = null;
		List<IDfDocument> minors = null;
		List<IDfDocument> allRevisions = new ArrayList<IDfDocument>();
		final IDfId chronicle;
		try {
			document = createDocument(session);
			chronicle = document.getChronicleId();
			allRevisions.add(document);

			minors = createMinorRevisions(document, 3);
			document = minors.get(minors.size() - 1); // Get the last revision
			allRevisions.addAll(minors);

			document = createMajorRevision(document);
			allRevisions.add(document);
			minors = createMinorRevisions(document, 3);
			document = minors.get(minors.size() - 1); // Get the last revision
			allRevisions.addAll(minors);

			document = createMajorRevision(document);
			allRevisions.add(document);
			IDfDocument branch1 = createBranch(document);
			allRevisions.add(branch1);
			IDfDocument branch2 = createBranch(document);
			allRevisions.add(branch2);
			IDfDocument branch3 = createBranch(document);
			allRevisions.add(branch3);

			minors = createMinorRevisions(document, 3);
			document = minors.get(minors.size() - 1);
			allRevisions.addAll(minors);
			minors = createMinorRevisions(branch1, 3);
			branch1 = minors.get(minors.size() - 1);
			allRevisions.addAll(minors);
			minors = createMinorRevisions(branch2, 3);
			branch2 = minors.get(minors.size() - 1);
			allRevisions.addAll(minors);
			minors = createMinorRevisions(branch3, 3);
			branch3 = minors.get(minors.size() - 1);
			allRevisions.addAll(minors);

			document = createMajorRevision(document, 3);
			allRevisions.add(document);
			minors = createMinorRevisions(document, 3, 3);
			document = minors.get(minors.size() - 1);
			allRevisions.addAll(minors);

			branch1 = createBranch(branch1);
			allRevisions.add(branch1);
			branch2 = createBranch(branch2);
			allRevisions.add(branch2);
			branch3 = createBranch(branch3);
			allRevisions.add(branch3);

			minors = createMinorRevisions(document, 3);
			document = minors.get(minors.size() - 1);
			allRevisions.addAll(minors);
			minors = createMinorRevisions(branch1, 3);
			branch1 = minors.get(minors.size() - 1);
			allRevisions.addAll(minors);
			minors = createMinorRevisions(branch2, 3);
			branch2 = minors.get(minors.size() - 1);
			allRevisions.addAll(minors);
			minors = createMinorRevisions(branch3, 3);
			branch3 = minors.get(minors.size() - 1);
			allRevisions.addAll(minors);

			document = createMajorRevision(document, 3);
			allRevisions.add(document);
			minors = createMinorRevisions(document, 3, 3);
			document = minors.get(minors.size() - 1);
			allRevisions.addAll(minors);

			// Ok...so now we have a "complex" version tree... now we start to trim out
			// specific versions to clip it and start deducing where things go
			Map<String, IDfDocument> index = new HashMap<String, IDfDocument>();
			Set<String> removed = new HashSet<String>();
			for (IDfDocument d : allRevisions) {
				index.put(d.getObjectId().getId(), d);
			}

			// Verify continuity
			for (IDfDocument d : allRevisions) {
				IDfId a = d.getAntecedentId();
				if (a.isNull()) {
					Assert.assertEquals(d.getChronicleId().getId(), d.getObjectId().getId());
					continue;
				}
				Assert.assertTrue(index.containsKey(a.getId()));
			}

			// Chop it up
			for (int i = 1; i <= allRevisions.size(); i++) {
				// Remove every 3rd revision
				IDfDocument doc = allRevisions.get(i - 1);
				if ((i % 3) == 0) {
					// you're toast...
					boolean ok = false;
					session.beginTrans();
					final String oid = doc.getObjectId().getId();
					final String version = doc.getImplicitVersionLabel();
					try {
						doc.destroy();
						index.remove(oid);
						removed.add(oid);
						ok = true;
					} finally {
						if (ok) {
							this.log.info(String.format("DESTROYED VERSION %s", version));
							session.commitTrans();
						} else {
							this.log.warn(String.format("FAILED TO DESTROY VERSION %s", version));
							session.abortTrans();
						}
					}
				}
			}

			// Verify continuity breaks
			for (final String oid : index.keySet()) {
				final IDfDocument d = index.get(oid);
				final IDfId a = d.getAntecedentId();
				final String aid = a.getId();
				if (a.isNull()) {
					Assert.assertEquals(d.getChronicleId().getId(), d.getObjectId().getId());
					continue;
				}
				if (removed.contains(aid)) {
					Assert.assertFalse(index.containsKey(aid));
					try {
						IDfDocument antecedent = IDfDocument.class.cast(session.getObject(a));
						Assert.fail(String.format(
							"Deleted antecedent [%s] (v %s) is still available in the repository", aid,
							antecedent.getImplicitVersionLabel()));
					} catch (DfIdNotFoundException e) {
						// We're good...it was properly removed
					}
				}
			}

			// Now, we try to repair the broken tree
			new DfVersionTree(session, chronicle);

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