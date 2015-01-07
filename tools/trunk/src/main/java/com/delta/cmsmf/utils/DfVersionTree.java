/**
 *
 */

package com.delta.cmsmf.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cms.CmsAttributes;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DfVersionTree {

	protected final Logger log = Logger.getLogger(getClass());

	public final IDfId chronicle;
	public final Map<String, DfVersionNumber> indexById;
	public final Map<DfVersionNumber, String> indexByVersionNumber;
	public final Set<DfVersionNumber> missingAntecedent;
	public final Set<DfVersionNumber> patches;

	public DfVersionTree(IDfSession session, IDfId chronicle) throws CMSMFException, DfException {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to analyze the chronicle"); }
		if (chronicle == null) { throw new IllegalArgumentException("Must provide a chronicle to analyze"); }
		if (chronicle.isNull()) { throw new IllegalArgumentException("Must provide a valid (non-NULLID) chronicle"); }
		// First, create an index by object ID...
		final String chronicleId = chronicle.getId();
		Map<String, DfVersionNumber> indexById = new HashMap<String, DfVersionNumber>();
		Map<DfVersionNumber, IDfSysObject> sysObjectsByVersionNumber = new TreeMap<DfVersionNumber, IDfSysObject>();
		Map<DfVersionNumber, String> indexByVersionNumber = new TreeMap<DfVersionNumber, String>();

		IDfCollection results = null;
		final List<IDfId> all;
		try {
			final String dql = String.format(
				"select r_object_id from dm_sysobject (ALL) where i_chronicle_id = '%s' order by r_object_id",
				chronicle.getId());
			results = DfUtils.executeQuery(session, dql, IDfQuery.DF_EXECREAD_QUERY);
			all = new ArrayList<IDfId>();
			while (results.next()) {
				all.add(results.getId(CmsAttributes.R_OBJECT_ID));
			}
		} finally {
			DfUtils.closeQuietly(results);
		}

		for (final IDfId sysObjectId : all) {
			final String sysObjectIdStr = sysObjectId.getId();
			final IDfSysObject sysObject = IDfSysObject.class.cast(session.getObject(sysObjectId));
			final DfVersionNumber versionNumber = new DfVersionNumber(sysObject.getImplicitVersionLabel());
			final IDfSysObject duplicate = sysObjectsByVersionNumber.put(versionNumber, sysObject);
			if (duplicate != null) { throw new CMSMFException(String.format(
				"Duplicate version number [%s] between documents [%s] and [%s]", versionNumber, sysObjectIdStr,
				duplicate.getObjectId().getId())); }
			indexByVersionNumber.put(versionNumber, sysObjectIdStr);

			indexById.put(sysObjectIdStr, versionNumber);
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
		IDfSysObject rootNode = null;
		for (DfVersionNumber versionNumber : sysObjectsByVersionNumber.keySet()) {
			final IDfSysObject sysObject = sysObjectsByVersionNumber.get(versionNumber);
			final IDfId sysObjectId = sysObject.getObjectId();
			final IDfId antecedentId = sysObject.getAntecedentId();
			final DfVersionNumber antecedentVersion = indexById.get(antecedentId);
			if (antecedentVersion != null) {
				continue;
			}

			if (antecedentId.isNull()) {
				if (rootNode != null) {
					// If we already have a root node, then this is an error condition with invalid
					// data, and we can't continue
					throw new CMSMFException(String.format(
						"Found a second object [%s] in chronicle [%s] that has a null antecedent id",
						sysObjectId.getId(), chronicleId));
				}

				// If there is no antecedent, and the antecedent ID is the null ID
				// (0000000000000000), then this MUST be the chronicle root...verify it!
				if (!Tools.equals(chronicleId, sysObject.getObjectId().getId())) {
					// If this is not the chronicle root, this is an error condition from invalid
					// data, and we can't continue
					throw new CMSMFException(
						String
							.format(
								"Object with ID [%s] returned the null ID for its antecedent, but it's not the chronicle root for [%s]",
								sysObjectId.getId(), chronicleId));
				}
				continue;
			}

			missingAntecedent.add(versionNumber);
		}

		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("Locating missing antecedents for versions: %s", missingAntecedent));
		}

		// Ok...so now we walk through the items in missingAntecedent and determine where they need
		// to be grafted onto the tree
		Set<DfVersionNumber> patches = new TreeSet<DfVersionNumber>();
		for (DfVersionNumber versionNumber : missingAntecedent) {
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("Repairing version [%s]", versionNumber));
			}
			// First, see if we can find any of its required antecedents...
			Set<DfVersionNumber> antecedents = new TreeSet<DfVersionNumber>(DfVersionNumber.REVERSE_ORDER);
			antecedents.addAll(versionNumber.getAllAntecedents(true));
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("Searching for the required antecedents: %s", antecedents));
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

		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("Total of %d items lack antecedents in chronicle [%s]: %s",
				missingAntecedent.size(), chronicleId, missingAntecedent));
			this.log.trace(String.format("Total of %d corrections required for the full tree for chronicle [%s]: %s",
				patches.size(), chronicleId, patches));
		}
		this.chronicle = chronicle;
		this.indexById = Tools.freezeMap(indexById);
		this.indexByVersionNumber = Tools.freezeMap(indexByVersionNumber);
		this.missingAntecedent = Tools.freezeSet(missingAntecedent);
		this.patches = Tools.freezeSet(patches);
	}
}