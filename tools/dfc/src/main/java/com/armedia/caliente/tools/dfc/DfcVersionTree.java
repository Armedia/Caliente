/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
/**
 *
 */

package com.armedia.caliente.tools.dfc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.DfObjectNotFoundException;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

/**
 *
 *
 */
public class DfcVersionTree {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * The i_chronicle_id this tree represents
	 */
	public final IDfId chronicle;

	/**
	 * Maps the r_object_id to each version, without order
	 */
	public final Map<String, DfcVersionNumber> indexById;

	/**
	 * Maps the version numbers to the r_object_id for each version, ordered by version.
	 */
	public final Map<DfcVersionNumber, String> indexByVersionNumber;

	/**
	 * Includes all the versions for which the antecedent version is missing (i.e. i_antecedent_id
	 * pointed to a non-existent object), in order. If this set is empty, it means the tree is
	 * stable as-is and needs no repairs.
	 */
	public final Set<DfcVersionNumber> missingAntecedent;

	/**
	 * Includes all the required versions that need to be added in order to create a consistent
	 * version tree, in order. If this set is empty, it means the tree is stable as-is and needs no
	 * repairs.
	 */
	public final Set<DfcVersionNumber> totalPatches;

	/**
	 * Includes all the version numbers required to obtain a stable, consistent tree, including
	 * existing versions <b><i>and</i></b> required patches, in order.
	 */
	public final List<DfcVersionNumber> allVersions;

	/**
	 * Includes all the versions for which the antecedent version is missing (i.e. i_antecedent_id
	 * pointed to a non-existent object), in order. The value is the version number for the closest
	 * antecedent from which this version can be obtained, applying the necessary patches.
	 */
	public final Map<DfcVersionNumber, DfcVersionNumber> alternateAntecedent;

	/**
	 * Flag to indicate if the tree contains branches or not.
	 */
	private final boolean branched;

	/**
	 *
	 * @param session
	 *            the session through which to perform the analysis
	 * @param chronicle
	 *            the chronicle for which to construct the version tree
	 * @throws DctmException
	 *             an unrecoverable processing error ocurred
	 * @throws DfException
	 *             raised by the underlying DFC
	 * @throws DfObjectNotFoundException
	 *             if the given chronicle id refers to a missing chronicle and no objects were found
	 * @throws IllegalArgumentException
	 *             raised if the session is {@code null}, the chronicle is {@code null}, or
	 *             {@code chronicle.isNull()} returns {@code true}.
	 */
	public DfcVersionTree(IDfSession session, IDfId chronicle) throws DctmException, DfException {
		if (session == null) {
			throw new IllegalArgumentException("Must provide a session through which to analyze the chronicle");
		}
		if (chronicle == null) { throw new IllegalArgumentException("Must provide a chronicle to analyze"); }
		if (chronicle.isNull()) { throw new IllegalArgumentException("Must provide a valid (non-NULLID) chronicle"); }
		// First, create an index by object ID...
		final String chronicleId = chronicle.getId();
		Map<String, DfcVersionNumber> indexById = new HashMap<>();
		Map<DfcVersionNumber, IDfSysObject> sysObjectsByVersionNumber = new TreeMap<>();
		Map<DfcVersionNumber, String> indexByVersionNumber = new TreeMap<>();
		Set<DfcVersionNumber> allVersions = new TreeSet<>();
		Map<DfcVersionNumber, DfcVersionNumber> alternateAntecedents = new TreeMap<>();
		Set<DfcVersionNumber> trunkVersions = new TreeSet<>(DfcVersionNumber.DESCENDING);

		final String dql = String.format(
			"select distinct r_object_id from dm_sysobject (ALL) where i_chronicle_id = '%s' order by r_object_id",
			chronicle.getId());
		final List<IDfId> all = new ArrayList<>();
		DfcQuery.run(session, dql, DfcQuery.Type.DF_EXECREAD_QUERY, (o) -> all.add(o.getValueAt(0).asId()));
		// This can only happen if there is nothing in the chronicle
		if (all.isEmpty()) { throw new DfIdNotFoundException(chronicle); }

		boolean branched = false;
		for (final IDfId sysObjectId : all) {
			final String sysObjectIdStr = sysObjectId.getId();
			final IDfSysObject sysObject = IDfSysObject.class.cast(session.getObject(sysObjectId));
			final DfcVersionNumber versionNumber = new DfcVersionNumber(sysObject.getImplicitVersionLabel());
			final IDfSysObject duplicate = sysObjectsByVersionNumber.put(versionNumber, sysObject);
			if (duplicate != null) {
				throw new DctmException(String.format("Duplicate version number [%s] between documents [%s] and [%s]",
					versionNumber, sysObjectIdStr, duplicate.getObjectId().getId()));
			}
			if (versionNumber.getComponentCount() == 2) {
				trunkVersions.add(versionNumber);
			}
			// If the version number contains more than two components, it's definitely
			// a branch...
			branched |= (versionNumber.getComponentCount() > 2);
			indexByVersionNumber.put(versionNumber, sysObjectIdStr);
			allVersions.add(versionNumber);

			indexById.put(sysObjectIdStr, versionNumber);
		}

		this.branched = branched;

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

		Set<DfcVersionNumber> missingAntecedent = new TreeSet<>();
		IDfSysObject rootNode = null;
		for (DfcVersionNumber versionNumber : sysObjectsByVersionNumber.keySet()) {
			final IDfSysObject sysObject = sysObjectsByVersionNumber.get(versionNumber);
			final IDfId sysObjectId = sysObject.getObjectId();
			final IDfId antecedentId = sysObject.getAntecedentId();

			if (antecedentId.isNull()) {
				if (rootNode != null) {
					// If we already have a root node, then this is an error condition with invalid
					// data, and we can't continue
					throw new DctmException(
						String.format("Found a second object [%s] in chronicle [%s] that has a null antecedent id",
							sysObjectId.getId(), chronicleId));
				}

				// If there is no antecedent, and the antecedent ID is the null ID
				// (0000000000000000), then this MUST be the chronicle root...verify it!
				if (!Tools.equals(chronicleId, sysObject.getObjectId().getId())) {
					// If this is not the chronicle root, this is an error condition from invalid
					// data, and we can't continue
					throw new DctmException(String.format(
						"Object with ID [%s] returned the null ID for its antecedent, but it's not the chronicle root for [%s]",
						sysObjectId.getId(), chronicleId));
				}

				// This is the root node, so mark it as such
				rootNode = sysObject;
				continue;
			}

			final DfcVersionNumber antecedentVersion = versionNumber.getAntecedent(false);
			if ((versionNumber.getComponentCount() == 2) || indexByVersionNumber.containsKey(antecedentVersion)) {
				// If this is a "root" version, or the antecedent version exists, then don't list it
				// as a missing antecedent
				continue;
			}

			missingAntecedent.add(versionNumber);
		}

		if (this.log.isTraceEnabled()) {
			this.log.trace("Locating missing antecedents for versions: {}", missingAntecedent);
		}

		// Ok...so now we walk through the items in missingAntecedent and determine where they need
		// to be grafted onto the tree
		Set<DfcVersionNumber> patches = new TreeSet<>();
		for (DfcVersionNumber versionNumber : missingAntecedent) {
			if (this.log.isTraceEnabled()) {
				this.log.trace("Repairing version [{}]", versionNumber);
			}
			// First, see if we can find any of its required antecedents...
			Set<DfcVersionNumber> antecedents = new TreeSet<>(DfcVersionNumber.DESCENDING);
			antecedents.addAll(versionNumber.getAllAntecedents(true));
			if (this.log.isTraceEnabled()) {
				this.log.trace("Searching for the required antecedents: {}", antecedents);
			}

			// The antecedent list is organized in inverse order, so we have to do the least amount
			// of work in order to finalize the tree structure
			boolean alternateFound = false;
			DfcVersionNumber trunkPatch = null;
			alternateSearch: for (DfcVersionNumber antecedent : antecedents) {
				if (indexByVersionNumber.containsKey(antecedent) || patches.contains(antecedent)) {
					if (!antecedent.isSibling(versionNumber)) {
						alternateAntecedents.put(versionNumber, antecedent);
						alternateFound = true;
						break alternateSearch;
					}
					continue alternateSearch;
				}
				patches.add(antecedent);
				allVersions.add(antecedent);
				if (antecedent.getComponentCount() == 2) {
					trunkPatch = antecedent;
				}
			}

			if (!alternateFound && (trunkPatch != null)) {
				// The alternate must be the "latest trunk version"
				trunkSearch: for (DfcVersionNumber trunk : trunkVersions) {
					if (trunkPatch.isSuccessorOf(trunk)) {
						alternateAntecedents.put(versionNumber, trunk);
						break trunkSearch;
					}
				}
			}
		}

		for (DfcVersionNumber versionNumber : trunkVersions) {
			// Find the highest trunk - existing or from a patch - that should be used
			// as the antecedent
			DfcVersionNumber highestPatch = null;
			for (DfcVersionNumber p : patches) {
				if (p.getComponentCount() > 2) {
					continue;
				}
				if (p.compareTo(versionNumber) >= 0) {
					break;
				}
				highestPatch = p;
			}
			DfcVersionNumber highestTrunk = null;
			for (DfcVersionNumber p : trunkVersions) {
				if (p.compareTo(versionNumber) < 0) {
					highestTrunk = p;
					break;
				}
			}

			DfcVersionNumber bestAntecedent = Tools.max(highestPatch, highestTrunk);
			if (bestAntecedent == null) {
				continue;
			}

			if (trunkVersions.contains(bestAntecedent)) {
				// If the best antecedent is already part of the versions that
				// exist, then we need not store an alternative
				continue;
			}

			// The best antecedent doesn't already exist, so we mark it for future
			// refrence
			alternateAntecedents.put(versionNumber, bestAntecedent);
		}

		if (this.log.isTraceEnabled()) {
			this.log.trace("Total of {} items lack antecedents in chronicle [{}]: {}", missingAntecedent.size(),
				chronicleId, missingAntecedent);
			this.log.trace("Total of %d corrections required for the full tree for chronicle [{}]: {}", patches.size(),
				chronicleId, patches);
		}
		this.chronicle = chronicle;
		this.indexById = Tools.freezeMap(indexById);
		this.indexByVersionNumber = Tools.freezeMap(indexByVersionNumber);
		this.missingAntecedent = Tools.freezeSet(missingAntecedent);
		this.totalPatches = Tools.freezeSet(patches);
		this.allVersions = Tools.freezeList(new ArrayList<>(allVersions));
		this.alternateAntecedent = Tools.freezeMap(alternateAntecedents);
	}

	/**
	 *
	 * @param current
	 * @return the versions that precede the given object ID in the version tree (non inclusive), or
	 *         {@code null} if the ID isn't part of this version tree
	 */
	public List<IDfId> getVersionsPre(IDfId current) {
		if (current == null) { throw new IllegalArgumentException("Must provide an ID to split the tree"); }
		if (this.indexById.containsKey(current.getId())) { return null; }
		List<IDfId> l = new ArrayList<>();
		for (DfcVersionNumber vn : this.indexByVersionNumber.keySet()) {
			final String id = this.indexByVersionNumber.get(vn);
			if (id.equals(current.getId())) {
				break;
			}
			l.add(new DfId(id));
		}
		return l;
	}

	/**
	 *
	 * @param current
	 * @return the versions that follow the given object ID in the version tree (non inclusive), or
	 *         {@code null} if the ID isn't part of this version tree
	 */
	public List<IDfId> getVersionsPost(IDfId current) {
		if (current == null) { throw new IllegalArgumentException("Must provide an ID to split the tree"); }
		if (this.indexById.containsKey(current.getId())) { return null; }
		List<IDfId> l = new ArrayList<>();
		boolean add = false;
		for (DfcVersionNumber vn : this.indexByVersionNumber.keySet()) {
			final String id = this.indexByVersionNumber.get(vn);
			if (id.equals(current.getId())) {
				add = true;
				continue;
			}
			if (add) {
				l.add(new DfId(id));
			}
		}
		return l;
	}

	public boolean isBranched() {
		return this.branched;
	}
}