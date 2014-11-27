package com.armedia.cmf.engine.sharepoint;

import com.armedia.cmf.storage.StoredObjectType;
import com.independentsoft.share.Group;
import com.independentsoft.share.Service;

public class ShptGroup extends ShptSecurityObject<Group> {

	public ShptGroup(Service service, Group group) {
		super(service, group, StoredObjectType.GROUP);
	}

	@Override
	public int getNumericId() {
		return this.wrapped.getId();
	}

	@Override
	public String getName() {
		return this.wrapped.getLoginName();
	}
}