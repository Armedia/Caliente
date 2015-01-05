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
		this.version = vn;
		this.node = node;
	}

	public DfDocumentVersionNode(DfVersionNumber vn, DfDocumentVersionNode antecedent) throws DfException {
		this.version = vn;
		this.node = null;
		this.antecedent = antecedent;
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

			DfVersionNumber avn = antecedent.version;
			if (!avn.isAncestorOf(this.version) && !avn.isAntecedentOf(this.version)) { throw new IllegalArgumentException(
				String.format(
					"Version [%s] cannot be obtained directly from version [%s] via either branch or checkout",
					this.version, avn)); }

			if (avn.isAntecedentOf(this.version)) {
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

	public DfVersionNumber getVersion() {
		return this.version;
	}

	// Traverse the tree upward towards the root
	public DfDocumentVersionNode getRootVersion() {
		if (this.antecedent == null) { return this; }
		return this.antecedent.getRootVersion();
	}

	public void insertSuccessor(DfDocumentVersionNode node) {
		if (node == null) { throw new IllegalArgumentException("Must provide a node to insert after"); }
		boolean s = node.version.isSuccessorOf(this.version);
		boolean d = node.version.isDescendantOf(this.version);

		if (!s && !d) { throw new IllegalArgumentException(String.format(
			"Version [%s] can't be directly related to [%s]", node.version, this.version)); }

		node.antecedent = this;
		if (s) {
			// Direct successor
			if (this.successor != null) {
				this.successor.antecedent = node;
			}
			this.successor = node;
		} else if (d) {
			// Branch
			this.branches.put(node.version, node);
		}
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
}