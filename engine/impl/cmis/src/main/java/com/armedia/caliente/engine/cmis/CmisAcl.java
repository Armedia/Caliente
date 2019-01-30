package com.armedia.caliente.engine.cmis;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.commons.data.Acl;

import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;

public final class CmisAcl {

	private final CmfObject.Archetype sourceType;
	private final String sourceId;
	private final String sourceOwner;
	private final Acl acl;

	public CmisAcl(CmfObject.Archetype sourceType, CmisObject object) {
		this.sourceType = sourceType;
		this.sourceId = object.getId();
		this.sourceOwner = object.getCreatedBy();
		this.acl = object.getAcl();
	}

	public CmfObject.Archetype getSourceType() {
		return this.sourceType;
	}

	public String getSourceId() {
		return this.sourceId;
	}

	public String getSourceOwner() {
		return this.sourceOwner;
	}

	public Acl getAcl() {
		return this.acl;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.sourceType, this.sourceId);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmisAcl other = CmisAcl.class.cast(obj);
		if (this.sourceType != other.sourceType) { return false; }
		if (!Tools.equals(this.sourceId, other.sourceId)) { return false; }
		return true;
	}
}