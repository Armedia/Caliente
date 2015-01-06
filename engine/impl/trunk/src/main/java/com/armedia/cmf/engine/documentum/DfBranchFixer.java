/**
 *
 */

package com.armedia.cmf.engine.documentum;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DfBranchFixer {

	private static final Logger LOG = Logger.getLogger(DfBranchFixer.class);

	public static class RepairPlan {
		public final Map<String, IDfDocument> byId;
		public final Map<DfVersionNumber, IDfDocument> byVersionNumber;
		public final Set<DfVersionNumber> missingAntecedent;
		public final Set<DfVersionNumber> expectedPatches;

		/**
		 * @param byId
		 * @param byVersionNumber
		 * @param missingAntecedent
		 * @param expectedPatches
		 */
		protected RepairPlan(Map<String, IDfDocument> byId, Map<DfVersionNumber, IDfDocument> byVersionNumber,
			Set<DfVersionNumber> missingAntecedent, Set<DfVersionNumber> expectedPatches) {
			this.byId = Tools.freezeMap(byId);
			this.byVersionNumber = Tools.freezeMap(byVersionNumber);
			this.missingAntecedent = Tools.freezeSet(missingAntecedent);
			this.expectedPatches = Tools.freezeSet(expectedPatches);
		}
	}

	public static RepairPlan fixTree(IDfId chronicle, Collection<IDfDocument> allVersions) throws Exception,
	DfException {
		// First, create an index by object ID...
		final String chronicleId = chronicle.getId();
		Map<String, IDfDocument> indexById = new HashMap<String, IDfDocument>();
		Map<DfVersionNumber, IDfDocument> indexByVersionNumber = new TreeMap<DfVersionNumber, IDfDocument>();
		for (IDfDocument d : allVersions) {
			if (d == null) {
				continue;
			}
			if (!Tools.equals(chronicleId, d.getChronicleId().getId())) { throw new Exception(String.format(
				"Bad chronicle ID for document [%s] - expected [%s] but got [%s]", d.getObjectId().getId(),
				chronicleId, d.getChronicleId().getId())); }
			DfVersionNumber versionNumber = new DfVersionNumber(d.getImplicitVersionLabel());
			IDfDocument dupe = indexByVersionNumber.put(versionNumber, d);
			if (dupe != null) { throw new Exception(String.format(
				"Duplicate version number [%s] in document [%s] and [%s]", versionNumber.toString(), d.getObjectId()
				.getId(), dupe.getObjectId().getId())); }
			indexById.put(d.getObjectId().getId(), d);
		}

		// So...at this point, if we were to walk by the indexByVersionNumber, we would be able to
		// construct the tree in correct version order, since we know for a fact that the versions
		// are correctly ordered relative to each other, and thus antecedents will necessarily
		// be listed before their successors or their branches. So now we have to find the gaps.

		// The idea is to walk through all the documents in version order, and take note of which
		// ones have a missing antecedent. Then, at the end, we will make a list of all the
		// "expected" versions that the tree should have in order for no antecedents to be missing.
		// What this means is that as we reconstruct the tree later on, we will know which versions
		// exist, and which ones don't...and we'll be able to "fix" the tree

		// Start by identifying the versions for which no antecedent is present

		Set<DfVersionNumber> missingAntecedent = new TreeSet<DfVersionNumber>();
		IDfDocument rootNode = null;
		for (DfVersionNumber vn : indexByVersionNumber.keySet()) {
			final IDfDocument d = indexByVersionNumber.get(vn);
			final IDfId objectId = d.getObjectId();
			final IDfId antecedentId = d.getAntecedentId();
			IDfDocument antecedent = indexById.get(antecedentId.getId());
			if (antecedent != null) {
				continue;
			}

			if (antecedentId.isNull()) {
				if (rootNode != null) {
					// If we already have a root node, then this is an error condition with invalid
					// data, and we can't continue
					throw new Exception(String.format(
						"Found a second object [%s] in chronicle [%s] that has a null antecedent id", objectId.getId(),
						chronicleId));
				}

				// If there is no antecedent, and the antecedent ID is the null ID
				// (0000000000000000), then this MUST be the chronicle root...verify it!
				if (!Tools.equals(chronicleId, d.getObjectId().getId())) {
					// If this is not the chronicle root, this is an error condition from invalid
					// data, and we can't continue
					throw new Exception(
						String
						.format(
							"Object with ID [%s] returned the null ID for its antecedent, but it's not the chronicle root for [%s]",
							objectId.getId(), chronicleId));
				}
				continue;
			}

			missingAntecedent.add(vn);
		}

		if (DfBranchFixer.LOG.isDebugEnabled()) {
			DfBranchFixer.LOG.debug(String.format("Locating missing antecedents for versions: %s", missingAntecedent));
		}

		// Ok...so now we walk through the items in missingAntecedent and determine where they need
		// to be grafted onto the tree
		Set<DfVersionNumber> patches = new TreeSet<DfVersionNumber>();
		for (DfVersionNumber vn : missingAntecedent) {
			if (DfBranchFixer.LOG.isDebugEnabled()) {
				DfBranchFixer.LOG.debug(String.format("Repairing version [%s]", vn));
			}
			// First, see if we can find any of its required antecedents...
			Set<DfVersionNumber> antecedents = new TreeSet<DfVersionNumber>(DfVersionNumber.REVERSE_ORDER);
			antecedents.addAll(vn.getAllAntecedents(true));
			if (DfBranchFixer.LOG.isDebugEnabled()) {
				DfBranchFixer.LOG.debug(String.format("Searching for the required antecedents: %s", antecedents));
			}

			// The antecedent list is organized in inverse order, so we have to do the least amount
			// of work in order to finalize the tree structure
			for (DfVersionNumber antecedent : antecedents) {
				if (indexByVersionNumber.containsKey(antecedent)) {
					break;
				}
				patches.add(antecedent);
			}
		}

		if (DfBranchFixer.LOG.isDebugEnabled()) {
			DfBranchFixer.LOG.debug(String.format("Total of %d items lack antecedents in chronicle [%s]: %s",
				missingAntecedent.size(), chronicleId, missingAntecedent));
			DfBranchFixer.LOG.debug(String.format(
				"Total of %d corrections required for the full tree for chronicle [%s]: %s", patches.size(),
				chronicleId, patches));
		}
		return new RepairPlan(indexById, indexByVersionNumber, missingAntecedent, patches);
	}
}