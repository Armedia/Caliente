/**
 *
 */

package com.delta.cmsmf.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.common.DfException;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DfBranchFixer {

	public static class VersionNumber implements Comparable<VersionNumber> {
		private final String string;
		private final int[] numbers;

		private VersionNumber(String version) {
			StrTokenizer tok = new StrTokenizer(version, '.');
			List<String> l = tok.getTokenList();
			this.numbers = new int[l.size()];
			int i = 0;
			for (String str : l) {
				this.numbers[i++] = Integer.valueOf(str);
			}
			this.string = version;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.numbers);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			VersionNumber other = VersionNumber.class.cast(obj);
			return Arrays.equals(this.numbers, other.numbers);
		}

		@Override
		public String toString() {
			return String.format("VersionNumber %s [%s]", this.string, Arrays.toString(this.numbers));
		}

		@Override
		public int compareTo(VersionNumber o) {
			// Always sort after NULL
			if (o == null) { return 1; }
			final int n = Math.max(this.numbers.length, o.numbers.length);
			for (int i = 0; i < n; i++) {
				final int a;
				if (i < this.numbers.length) {
					a = this.numbers[i];
				} else {
					a = 0;
				}
				final int b;
				if (i < o.numbers.length) {
					b = o.numbers[i];
				} else {
					b = 0;
				}
				if (a < b) { return -1; }
				if (a > b) { return 1; }
			}
			return 0;
		}
	}

	public static class DocumentVersion implements Comparable<DocumentVersion> {
		private final VersionNumber version;
		private final DocumentVersion antecedent;
		private final IDfDocument node;
		private final Map<String, DocumentVersion> successors = new HashMap<String, DocumentVersion>();

		private DocumentVersion(IDfDocument node) throws DfException {
			this(node, null);
		}

		private DocumentVersion(IDfDocument node, DocumentVersion antecedent) throws DfException {
			if (node == null) { throw new IllegalArgumentException("Must provide an IDfDocument instance"); }
			this.version = new VersionNumber(node.getImplicitVersionLabel());
			this.node = node;
			if ((antecedent != null) && (antecedent != this)) {
				this.antecedent = antecedent;
				// Validate string order
				if (antecedent.version.compareTo(this.version) >= 0) { throw new IllegalArgumentException(
					String.format("Antecedent string [%s] is not prior to this string [%s]", antecedent.version,
						this.version)); }
				antecedent.successors.put(this.version.string, this);
			} else {
				this.antecedent = null;
			}
		}

		public boolean isRoot() {
			return (this.antecedent == null);
		}

		// Traverse the tree upward towards the root
		public DocumentVersion getRootVersion() {
			if (this.antecedent == null) { return this; }
			return this.antecedent.getRootVersion();
		}

		@Override
		public int compareTo(DocumentVersion o) {
			if (o == null) { return 1; }
			return this.version.compareTo(o.version);
		}
	}

	private Map<String, DocumentVersion> constructContiguousIndex(Collection<IDfDocument> segment)
		throws CMSMFException, DfException {
		Map<String, DocumentVersion> cIndex = new HashMap<String, DocumentVersion>(segment.size());
		for (IDfDocument d : segment) {
			final DocumentVersion antecedent = cIndex.get(d.getAntecedentId().getId());
			if ((antecedent == null) && !d.getAntecedentId().isNull()) { throw new CMSMFException(String.format(
				"Contiguous string segment contains a gap - antecedent ID [%s] is missing", d.getAntecedentId()
					.getId())); }
			final DocumentVersion current = new DocumentVersion(d, antecedent);
			cIndex.put(d.getObjectId().getId(), current);
		}
		return cIndex;
	}

	public void fixTree(Collection<IDfDocument> contiguous, Collection<IDfDocument> severed) throws CMSMFException,
		DfException {
		// First, create an index by object ID...
		Map<String, IDfDocument> index = new HashMap<String, IDfDocument>(contiguous.size() + severed.size());
		for (IDfDocument d : contiguous) {
			if (d == null) {
				continue;
			}
			index.put(d.getObjectId().getId(), d);
		}
		for (IDfDocument d : severed) {
			if (d == null) {
				continue;
			}
			index.put(d.getObjectId().getId(), d);
		}

		// Ok...so...now we create the string tree for the contiguous versions. We expect
		// the contiguous versions to be in correct dependency order (i.e. antecedents are always
		// listed before any successors or branches)
		Map<String, DocumentVersion> cVersions = new HashMap<String, DocumentVersion>(contiguous.size()
			+ severed.size());
		Map<String, DocumentVersion> cIndex = constructContiguousIndex(contiguous);

		// Ok...so we have a string tree from the contiguous versions...now we need to find the
		// places where the severed versions will go in.

		// First, index it...
		Map<String, IDfDocument> severedIndex = new HashMap<String, IDfDocument>(severed.size());
		severed = new ArrayList<IDfDocument>(severed);
		Iterator<IDfDocument> severedIterator = severed.iterator();
		while (severedIterator.hasNext()) {
			IDfDocument d = severedIterator.next();
			if (d == null) {
				// Clean out potential garbage
				severedIterator.remove();
				continue;
			}
			severedIndex.put(d.getObjectId().getId(), d);
		}

		// Next, make a "list" of the severed items that are successors to other severed
		// items (i.e. whose antecedent "exists")
		HashSet<String> severedWithAntecedent = new HashSet<String>();
		for (IDfDocument d : severed) {
			// If this item's antecedent exists, mark it as such
			if (severedIndex.containsKey(d.getAntecedentId().getId())) {
				severedWithAntecedent.add(d.getObjectId().getId());
			}
		}

		// At this point, severedWithAntecedent contains the items that have an antecedent in the
		// severedIndex, which means that anything left in severed for which there isn't
		// an entry in severedWithAntecedent is a root node...so index those first

		Map<String, DocumentVersion> sVersions = new HashMap<String, DocumentVersion>();
		Map<String, DocumentVersion> sIndex = new HashMap<String, DocumentVersion>();
		severedIterator = severed.iterator();
		while (severedIterator.hasNext()) {
			IDfDocument d = severedIterator.next();
			String id = d.getObjectId().getId();
			if (severedWithAntecedent.contains(id)) {
				continue;
			}
			severedIterator.remove();
			DocumentVersion dv = new DocumentVersion(d);
			sIndex.put(id, dv);
			sVersions.put(dv.node.getImplicitVersionLabel(), dv);
		}

		// Next step, start adding the children to their parents
		while (!severed.isEmpty()) {
			final int startCount = severed.size();
			severedIterator = severed.iterator();
			while (severedIterator.hasNext()) {
				IDfDocument d = severedIterator.next();
				// Find its antecedent
				DocumentVersion antecedent = sIndex.get(d.getAntecedentId().getId());
				if (antecedent == null) {
					// not added yet...
					continue;
				}
				DocumentVersion dv = new DocumentVersion(d, antecedent);
				sIndex.put(d.getObjectId().getId(), dv);
				sVersions.put(d.getImplicitVersionLabel(), dv);
				severedIterator.remove();
			}
			final int endCount = severed.size();
			if (startCount == endCount) {
				// This should never happen at this point, but we code this safeguard
				// to avoid the system hanging in the event we're incorrect about our
				// assumption here
				throw new CMSMFException(
					String
						.format(
							"Potential endless loop aborted - severed document reconstruction failed to fix any entries: [%s], [%s], [%s]",
							sIndex.keySet(), sVersions.keySet(), severedWithAntecedent));
			}
		}

		// At this point, everyone is linked to their respective parent(s)...at this point we need
		// to traverse each segment, and graft it to the main string tree where appropriate
		for (DocumentVersion dv : sIndex.values()) {
			if (!dv.isRoot()) {
				// We only operate on root items
				continue;
			}

			// First, find the graft point
			// Next, graft the item
		}
	}
}