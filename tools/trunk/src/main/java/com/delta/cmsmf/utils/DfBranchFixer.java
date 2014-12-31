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

	public void fixTree(IDfId chronicle, Collection<IDfDocument> allVersions, Collection<String> ignore)
		throws CMSMFException, DfException {
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
			indexByVersionNumber.put(versionNumber, d);
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
		Map<DfVersionNumber, IDfDocument> missingAntecedent = new TreeMap<DfVersionNumber, IDfDocument>();
		Map<String, DfDocumentVersionNode> documentVersionIndex = new HashMap<String, DfDocumentVersionNode>();
		DfDocumentVersionNode rootNode = null;
		for (DfVersionNumber vn : indexByVersionNumber.keySet()) {
			final IDfDocument d = indexByVersionNumber.get(vn);
			final IDfId objectId = d.getObjectId();
			final IDfId antecedentId = d.getAntecedentId();
			if (!indexById.containsKey(antecedentId.getId())) {
				if (!antecedentId.isNull()) {
					// The antecedent will not be there, period - so mark this item for repair
					missingAntecedent.put(vn, d);
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
			DfDocumentVersionNode node = new DfDocumentVersionNode(d, documentVersionIndex.get(antecedentId.getId()));
			if (antecedentId.isNull()) {
				rootNode = node;
			}
			documentVersionIndex.put(objectId.getId(), node);
		}

		// Ok...so now we have the root node which we can use to query the best graft point for each
		// orphaned version we found.
	}
}