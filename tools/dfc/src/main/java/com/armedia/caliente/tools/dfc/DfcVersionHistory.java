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
package com.armedia.caliente.tools.dfc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

public final class DfcVersionHistory<T extends IDfSysObject> implements Iterable<DfcVersion<T>> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final IDfId chronicleId;

	private final DfcVersion<T> rootVersion;

	private final DfcVersion<T> currentVersion;

	private final Map<IDfId, List<DfcVersionNumber>> patches;
	private final Map<IDfId, String> patchAntecedents;
	private final Map<IDfId, Integer> indexes;
	private final List<DfcVersion<T>> history;
	private final Map<IDfId, DfcVersion<T>> mainIndex;

	public DfcVersionHistory(IDfSession session, IDfId chronicleId) throws DfException, DctmException {
		this(session, chronicleId, null);
	}

	public DfcVersionHistory(IDfSession session, IDfId chronicleId, Class<T> objectClass)
		throws DfException, DctmException {
		if (session == null) {
			throw new NullPointerException("Must provide a valid session to read the history with");
		}
		if (chronicleId == null) {
			throw new NullPointerException("Must provide a valid chronicle id to read the history for");
		}

		this.chronicleId = chronicleId;

		// No existing history, we must calculate it
		final DfcVersionTree tree = new DfcVersionTree(session, chronicleId);
		List<DfcVersionNumber> currentPatches = new ArrayList<>();
		List<DfcVersion<T>> history = new LinkedList<>();
		Map<IDfId, List<DfcVersionNumber>> patches = new HashMap<>();
		Map<IDfId, String> patchAntecedents = new HashMap<>();
		Map<IDfId, DfcVersion<T>> mainIndex = new HashMap<>();
		DfcVersion<T> currentVersion = null;
		for (DfcVersionNumber versionNumber : tree.allVersions) {
			if (tree.totalPatches.contains(versionNumber)) {
				currentPatches.add(versionNumber);
				continue;
			}

			final IDfId id = new DfId(tree.indexByVersionNumber.get(versionNumber));
			final IDfSysObject obj = IDfSysObject.class.cast(session.getObject(id));
			final DfcVersion<T> thisVersion;
			if (objectClass != null) {
				thisVersion = new DfcVersion<>(this, versionNumber, objectClass.cast(obj));
			} else {
				thisVersion = new DfcVersion<>(this, versionNumber, obj.getObjectId(), obj.getCreationDate(),
					obj.getAntecedentId());
			}
			mainIndex.put(id, thisVersion);
			if (obj.getHasFolder()) {
				currentVersion = thisVersion;
			}
			history.add(thisVersion);
			if (!currentPatches.isEmpty()) {
				patches.put(id, Tools.freezeList(currentPatches));
				currentPatches = new ArrayList<>();
			}

			DfcVersionNumber alternateAntecedent = tree.alternateAntecedent.get(versionNumber);
			if (alternateAntecedent != null) {
				String antecedentId = tree.indexByVersionNumber.get(alternateAntecedent);
				if (antecedentId == null) {
					antecedentId = alternateAntecedent.toString();
				}
				patchAntecedents.put(id, antecedentId);
			}
		}

		// We need to sort the version history if branches are involved, since all versions must be
		// in proper chronological order. Relative order is not enough...this is only necessary
		// if branches are involved because "cousins" may not be properly ordered chronologically
		// and this needs to be the case in order to support import by other systems. We don't sort
		// otherwise because it implies a performance hit that we don't need to pay
		if (tree.isBranched()) {
			Collections.sort(history);
		}

		Map<IDfId, Integer> indexes = new HashMap<>(history.size());
		int i = 0;
		DfcVersion<T> rootVersion = null;
		final String idxFmt;
		final String max;
		if (this.log.isTraceEnabled()) {
			max = String.valueOf(history.size());
			int width = max.length();
			idxFmt = String.format("%%0%dd", width);
		} else {
			idxFmt = null;
			max = null;
		}

		for (DfcVersion<T> v : history) {
			if (rootVersion == null) {
				rootVersion = v;
			}
			final int index = ++i;
			if (this.log.isTraceEnabled()) {
				this.log.trace("HISTORY_{}[{} of {}] = [{}] ({}{})", chronicleId, String.format(idxFmt, index), max,
					v.getId(), v.getVersionNumber(), (v == currentVersion ? ",CURRENT" : ""));
			}
			indexes.put(v.getId(), index);
		}

		// Creating a new list here makes it easier for storage and traversal
		this.history = Tools.freezeList(new ArrayList<>(history));
		this.indexes = Tools.freezeMap(indexes);
		this.rootVersion = rootVersion;
		this.currentVersion = currentVersion;
		this.patches = Tools.freezeMap(patches);
		this.patchAntecedents = Tools.freezeMap(patchAntecedents);
		this.mainIndex = Tools.freezeMap(mainIndex);
		if (this.log.isTraceEnabled()) {
			this.log.trace("ROOT_{} = [{}] ({})", chronicleId, rootVersion.getId(), rootVersion.getVersionNumber());
			this.log.trace("CURRENT_{} = [{}] ({})", chronicleId, currentVersion.getId(),
				currentVersion.getVersionNumber());
			for (IDfId id : this.patches.keySet()) {
				this.log.trace("PATCHES_{}_{} = {}", chronicleId, id, this.patches.get(id));
			}
			for (IDfId id : this.patchAntecedents.keySet()) {
				this.log.trace("PATCH_ANTECEDENT_{}_{} = {}", chronicleId, id, this.patchAntecedents.get(id));
			}
		}
	}

	public DfcVersion<T> getRootVersion() {
		return this.rootVersion;
	}

	public DfcVersion<T> getCurrentVersion() {
		return this.currentVersion;
	}

	public boolean isEmpty() {
		return this.history.isEmpty();
	}

	public int size() {
		return this.history.size();
	}

	@Override
	public Iterator<DfcVersion<T>> iterator() {
		return this.history.iterator();
	}

	public List<DfcVersion<T>> getVersions() {
		return this.history;
	}

	public IDfId getChronicleId() {
		return this.chronicleId;
	}

	public DfcVersion<T> getVersion(IDfId id) {
		return this.mainIndex.get(id);
	}

	public DfcVersion<T> getAntecedent(DfcVersion<T> version) {
		if (version == null) {
			throw new IllegalArgumentException("Must provide a version whose antecedent to retrieve");
		}
		return getAntecedent(version.getId());
	}

	public DfcVersion<T> getAntecedent(IDfId objectId) {
		DfcVersion<T> v = getVersion(objectId);
		if (v == null) {
			throw new IllegalArgumentException(
				String.format("The object ID [%s] does not exist in this version history", objectId));
		}
		return this.mainIndex.get(v.getAntecedentId());
	}

	public List<DfcVersionNumber> getPatchesFor(IDfId objectId) {
		return this.patches.get(objectId);
	}

	public String getPatchAntecedentFor(IDfId objectId) {
		return this.patchAntecedents.get(objectId);
	}

	public Integer getIndexFor(IDfId objectId) {
		return this.indexes.get(objectId);
	}

	public Map<IDfId, String> getPatchAntecedents() {
		return this.patchAntecedents;
	}

	public Map<IDfId, List<DfcVersionNumber>> getPatches() {
		return this.patches;
	}

	public Map<IDfId, Integer> getIndexes() {
		return this.indexes;
	}

	public Integer getCurrentIndex() {
		if (this.currentVersion == null) { return -1; }
		return this.indexes.get(this.currentVersion.getId());
	}
}