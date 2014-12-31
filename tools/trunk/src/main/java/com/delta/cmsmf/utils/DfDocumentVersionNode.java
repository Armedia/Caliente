package com.delta.cmsmf.utils;

import java.util.Map;
import java.util.TreeMap;

import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.common.DfException;

public final class DfDocumentVersionNode implements Comparable<DfDocumentVersionNode> {
	private final DfVersionNumber version;
	private DfDocumentVersionNode antecedent = null;
	private final IDfDocument node;
	private DfDocumentVersionNode successor = null;
	private final Map<DfVersionNumber, DfDocumentVersionNode> branches = new TreeMap<DfVersionNumber, DfDocumentVersionNode>();

	public DfDocumentVersionNode(DfVersionNumber vn, IDfDocument node) throws DfException {
		this(node, null);
	}

	public DfDocumentVersionNode(IDfDocument node, DfDocumentVersionNode antecedent) throws DfException {
		if (node == null) { throw new IllegalArgumentException("Must provide an IDfDocument instance"); }
		this.version = new DfVersionNumber(node.getImplicitVersionLabel());
		this.node = node;
		if ((antecedent != null) && (antecedent != this)) {
			this.antecedent = antecedent;
			// Validate string order
			if (antecedent.version.compareTo(this.version) >= 0) {
				// Antecedent version is newer than this version?!?!
				throw new IllegalArgumentException(String.format(
					"Antecedent string [%s] is not prior to this string [%s]", antecedent.version, this.version));
			}
			// Validate that it either has the same number of dots and matches up to the
			// next-to-last item, or it has fewer components and matches completely until the
			// shortest length of the two
			int len = this.version.getComponentCount();
			boolean s = false;
			if (antecedent.version.getComponentCount() == len) {
				len--;
				s = true;
			} else if (antecedent.version.getComponentCount() < len) {
				len = antecedent.version.getComponentCount();
				s = false;
			} else {
				// Error - can't have an antecedent with more dots
				throw new IllegalArgumentException(String.format(
					"Antecedent string [%s] is deeper than this string [%s]", antecedent.version, this.version));
			}

			if (!this.version.equals(antecedent.version, len)) { throw new IllegalArgumentException(String.format(
				"Antecedent string [%s] is not an ancestor of child string [%s]", antecedent.version, this.version)); }

			if (s) {
				if (antecedent.successor != null) {
					// A successor is already in place...
					throw new IllegalArgumentException(
						String
						.format(
							"A successor version (%s) is already in place for antecedent [%s] when attempting to connect with [%s]",
							antecedent.successor.version, antecedent.version, this.version));
				}
				antecedent.successor = this;
			} else {
				antecedent.branches.put(this.version, this);
			}
		} else {
			this.antecedent = null;
		}
	}

	public boolean isRoot() {
		return (this.antecedent == null);
	}

	public IDfDocument getDocument() {
		return this.node;
	}

	// Traverse the tree upward towards the root
	public DfDocumentVersionNode getRootVersion() {
		if (this.antecedent == null) { return this; }
		return this.antecedent.getRootVersion();
	}

	public DfDocumentVersionNode setAntecedent(DfDocumentVersionNode antecedent) {
		DfDocumentVersionNode old = this.antecedent;
		this.antecedent = antecedent;
		return old;
	}

	public DfDocumentVersionNode getAntecedent() {
		return this.antecedent;
	}

	@Override
	public int compareTo(DfDocumentVersionNode o) {
		if (o == null) { return 1; }
		return this.version.compareTo(o.version);
	}

	public static DfDocumentVersionNode findBestGraftPoint(DfDocumentVersionNode rootNode, DfVersionNumber number) {

		if (!rootNode.isRoot()) {
			rootNode = rootNode.getRootVersion();
		}

		// Do the actual work...
		while (true) {

		}
	}
}