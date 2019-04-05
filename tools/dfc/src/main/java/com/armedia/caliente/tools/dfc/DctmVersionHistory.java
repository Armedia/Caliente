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

public final class DctmVersionHistory<T extends IDfSysObject> implements Iterable<DctmVersion<T>> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final IDfId chronicleId;

	private final DctmVersion<T> rootVersion;

	private final DctmVersion<T> currentVersion;

	private final Map<IDfId, List<DctmVersionNumber>> patches;
	private final Map<IDfId, String> patchAntecedents;
	private final Map<IDfId, Integer> indexes;
	private final List<DctmVersion<T>> history;
	private final Map<IDfId, DctmVersion<T>> mainIndex;

	public DctmVersionHistory(IDfSession session, IDfId chronicleId) throws DfException, DctmException {
		this(session, chronicleId, null);
	}

	public DctmVersionHistory(IDfSession session, IDfId chronicleId, Class<T> objectClass)
		throws DfException, DctmException {
		if (session == null) { throw new NullPointerException(
			"Must provide a valid session to read the history with"); }
		if (chronicleId == null) { throw new NullPointerException(
			"Must provide a valid chronicle id to read the history for"); }

		this.chronicleId = chronicleId;

		// No existing history, we must calculate it
		final DctmVersionTree tree = new DctmVersionTree(session, chronicleId);
		List<DctmVersionNumber> currentPatches = new ArrayList<DctmVersionNumber>();
		List<DctmVersion<T>> history = new LinkedList<DctmVersion<T>>();
		Map<IDfId, List<DctmVersionNumber>> patches = new HashMap<IDfId, List<DctmVersionNumber>>();
		Map<IDfId, String> patchAntecedents = new HashMap<IDfId, String>();
		Map<IDfId, DctmVersion<T>> mainIndex = new HashMap<IDfId, DctmVersion<T>>();
		DctmVersion<T> currentVersion = null;
		for (DctmVersionNumber versionNumber : tree.allVersions) {
			if (tree.totalPatches.contains(versionNumber)) {
				currentPatches.add(versionNumber);
				continue;
			}

			final IDfId id = new DfId(tree.indexByVersionNumber.get(versionNumber));
			final IDfSysObject obj = IDfSysObject.class.cast(session.getObject(id));
			final DctmVersion<T> thisVersion;
			if (objectClass != null) {
				thisVersion = new DctmVersion<T>(this, versionNumber, objectClass.cast(obj));
			} else {
				thisVersion = new DctmVersion<T>(this, versionNumber, obj.getObjectId(), obj.getCreationDate(),
					obj.getAntecedentId());
			}
			mainIndex.put(id, thisVersion);
			if (obj.getHasFolder()) {
				currentVersion = thisVersion;
			}
			history.add(thisVersion);
			if (!currentPatches.isEmpty()) {
				patches.put(id, Tools.freezeList(currentPatches));
				currentPatches = new ArrayList<DctmVersionNumber>();
			}

			DctmVersionNumber alternateAntecedent = tree.alternateAntecedent.get(versionNumber);
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

		Map<IDfId, Integer> indexes = new HashMap<IDfId, Integer>(history.size());
		int i = 0;
		DctmVersion<T> rootVersion = null;
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

		for (DctmVersion<T> v : history) {
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
		this.history = Tools.freezeList(new ArrayList<DctmVersion<T>>(history));
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

	public DctmVersion<T> getRootVersion() {
		return this.rootVersion;
	}

	public DctmVersion<T> getCurrentVersion() {
		return this.currentVersion;
	}

	public boolean isEmpty() {
		return this.history.isEmpty();
	}

	public int size() {
		return this.history.size();
	}

	@Override
	public Iterator<DctmVersion<T>> iterator() {
		return this.history.iterator();
	}

	public List<DctmVersion<T>> getVersions() {
		return this.history;
	}

	public IDfId getChronicleId() {
		return this.chronicleId;
	}

	public DctmVersion<T> getVersion(IDfId id) {
		return this.mainIndex.get(id);
	}

	public DctmVersion<T> getAntecedent(DctmVersion<T> version) {
		if (version == null) { throw new IllegalArgumentException(
			"Must provide a version whose antecedent to retrieve"); }
		return getAntecedent(version.getId());
	}

	public DctmVersion<T> getAntecedent(IDfId objectId) {
		DctmVersion<T> v = getVersion(objectId);
		if (v == null) { throw new IllegalArgumentException(
			String.format("The object ID [%s] does not exist in this version history", objectId)); }
		return this.mainIndex.get(v.getAntecedentId());
	}

	public List<DctmVersionNumber> getPatchesFor(IDfId objectId) {
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

	public Map<IDfId, List<DctmVersionNumber>> getPatches() {
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