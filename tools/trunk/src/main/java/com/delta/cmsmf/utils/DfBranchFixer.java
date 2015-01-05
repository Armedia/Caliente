/**
 *
 */

package com.delta.cmsmf.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DfBranchFixer {

	private static final Logger LOG = Logger.getLogger(DfBranchFixer.class);

	public static void fixTree(IDfId chronicle, Collection<IDfDocument> allVersions) throws CMSMFException, DfException {
		// First, create an index by object ID...
		final String chronicleId = chronicle.getId();
		Map<String, IDfDocument> indexById = new HashMap<String, IDfDocument>(allVersions.size());
		Map<DfVersionNumber, IDfDocument> indexByVersionNumber = new TreeMap<DfVersionNumber, IDfDocument>();
		Map<Integer, Set<DfVersionNumber>> indexByVersionComponents = new TreeMap<Integer, Set<DfVersionNumber>>();
		// Make sure the trunk is always covered
		indexByVersionComponents.put(2, new TreeSet<DfVersionNumber>());
		for (IDfDocument d : allVersions) {
			if (d == null) {
				continue;
			}
			if (!Tools.equals(chronicleId, d.getChronicleId().getId())) { throw new CMSMFException(String.format(
				"Bad chronicle ID for document [%s] - expected [%s] but got [%s]", d.getObjectId().getId(),
				chronicleId, d.getChronicleId().getId())); }
			indexById.put(d.getObjectId().getId(), d);
			DfVersionNumber versionNumber = new DfVersionNumber(d.getImplicitVersionLabel());
			IDfDocument dupe = indexByVersionNumber.put(versionNumber, d);
			if (dupe != null) { throw new CMSMFException(String.format(
				"Duplicate version number [%s] in document [%s] and [%s]", versionNumber.toString(), d.getObjectId()
				.getId(), dupe.getObjectId().getId())); }
			Set<DfVersionNumber> s = indexByVersionComponents.get(versionNumber.getComponentCount());
			if (s == null) {
				s = new TreeSet<DfVersionNumber>();
				indexByVersionComponents.put(versionNumber.getComponentCount(), s);
			}
			s.add(versionNumber);
		}

		// So...at this point, if we were to walk by the indexByVersion, we would be able to
		// construct the tree in correct version order, since we know for a fact that the versions
		// are correctly ordered relative to each other, and thus antecedents will necessarily
		// be listed before their successors or their branches. So now we have to find the gaps.

		// Start by identifying the versions for which no antecedent is present
		Map<DfVersionNumber, Set<DfVersionNumber>> missingAntecedent = new TreeMap<DfVersionNumber, Set<DfVersionNumber>>();
		Map<String, DfDocumentVersionNode> documentVersionIndexById = new HashMap<String, DfDocumentVersionNode>();
		Map<DfVersionNumber, DfDocumentVersionNode> documentVersionIndexByNumber = new TreeMap<DfVersionNumber, DfDocumentVersionNode>();
		DfDocumentVersionNode rootNode = null;
		for (DfVersionNumber vn : indexByVersionNumber.keySet()) {
			final IDfDocument d = indexByVersionNumber.get(vn);
			final IDfId objectId = d.getObjectId();
			final IDfId antecedentId = d.getAntecedentId();
			DfDocumentVersionNode antecedent = documentVersionIndexById.get(antecedentId.getId());
			if (antecedent == null) {
				if (!antecedentId.isNull()) {
					// The antecedent will not be there, period - so mark this item for repair, and
					// list the version history it needs leading up to the trunk
					Set<DfVersionNumber> s = new TreeSet<DfVersionNumber>(DfVersionNumber.REVERSE_ORDER);
					s.addAll(vn.getAllAntecedents(true));
					missingAntecedent.put(vn, s);
					continue;
				}

				if (rootNode != null) {
					// If we already have a root node, then this is an error condition with invalid
					// data, and we can't continue
					throw new CMSMFException(String.format(
						"Found a second object [%s] in chronicle [%s] that has a null antecedent id", objectId.getId(),
						chronicleId));
				}

				// If there is no antecedent, and the antecedent ID is the null ID
				// (0000000000000000), then this MUST be the chronicle root...verify it!
				if (!Tools.equals(chronicleId, d.getObjectId().getId())) {
					// If this is not the chronicle root, this is an error condition from invalid
					// data, and we can't continue
					throw new CMSMFException(
						String
						.format(
							"Object with ID [%s] returned the null ID for its antecedent, but it's not the chronicle root for [%s]",
							objectId.getId(), chronicleId));
				}
			}

			// Ok...so...create the tree node
			DfDocumentVersionNode node = new DfDocumentVersionNode(d, antecedent);
			if (antecedentId.isNull()) {
				rootNode = node;
			}
			documentVersionIndexById.put(objectId.getId(), node);
			documentVersionIndexByNumber.put(node.getVersion(), node);
		}

		if (DfBranchFixer.LOG.isDebugEnabled()) {
			DfBranchFixer.LOG.debug(String.format("Locating missing antecedents for versions: %s",
				missingAntecedent.keySet()));
		}

		// Ok...so now we walk through the items in missingAntecedent and determine where they need
		// to be grafted onto the tree
		for (DfVersionNumber vn : missingAntecedent.keySet()) {
			if (DfBranchFixer.LOG.isDebugEnabled()) {
				DfBranchFixer.LOG.debug(String.format("Repairing version [%s]", vn));
			}
			// First, see if we can find any of its required antecedents...
			DfVersionNumber last = null;
			Set<DfVersionNumber> antecedents = missingAntecedent.get(vn);
			if (DfBranchFixer.LOG.isDebugEnabled()) {
				DfBranchFixer.LOG.debug(String.format("Searching for the required antecedents: %s", antecedents));
			}
			// The antecedent list is organized in inverse order, so we have to do the least amount
			// of work in order to finalize the graft
			for (DfVersionNumber antecedent : antecedents) {
				if (indexByVersionNumber.containsKey(antecedent)) {
					last = antecedent;
					break;
				}
			}
			if (last == null) {
				if (vn.getComponentCount() > 2) {
					last = vn.getSubset(2);
					DfBranchFixer.LOG.debug("No antecedent found, will graft starting at the root of the tree");
					// Find the closest, non-superior version in the trunk to graft into
					// use that as the antecedent for the base branch, then add the rest
					// now, add the one we need
					DfDocumentVersionNode baseNode = null;
					for (DfVersionNumber a : documentVersionIndexByNumber.keySet()) {
						if (a.compareTo(last) <= 0) {
							baseNode = documentVersionIndexByNumber.get(a);
						} else {
							break;
						}
					}

					if (baseNode == null) {
						// There are no trunk versions, so add 1.0...
						baseNode = new DfDocumentVersionNode(new DfVersionNumber("1.0"), IDfDocument.class.cast(null));
						documentVersionIndexByNumber.put(baseNode.getVersion(), baseNode);
					}

					// Ok...so now we start to graft based on baseNode

					for (DfVersionNumber antecedent : antecedents) {
						DfDocumentVersionNode node = new DfDocumentVersionNode(antecedent, baseNode);
						documentVersionIndexByNumber.put(antecedent, node);
						baseNode = node;
					}

				} else {
					DfBranchFixer.LOG.debug("Item is at the root of the tree, no grafting required");
				}
			} else {
				if (DfBranchFixer.LOG.isDebugEnabled()) {
					DfBranchFixer.LOG.debug(String.format("Found candidate version %s as potential graft point", last));
				}
			}
		}
	}
}