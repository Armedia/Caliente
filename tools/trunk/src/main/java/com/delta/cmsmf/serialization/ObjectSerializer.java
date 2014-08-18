package com.delta.cmsmf.serialization;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.delta.cmsmf.cmsobjects.DctmObjectTypesEnum;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfSession;

public class ObjectSerializer {

	private Map<DctmObjectTypesEnum, Set<String>> index;

	ObjectSerializer() {
		Map<DctmObjectTypesEnum, Set<String>> index = new EnumMap<DctmObjectTypesEnum, Set<String>>(
			DctmObjectTypesEnum.class);
		for (DctmObjectTypesEnum e : DctmObjectTypesEnum.values()) {
			index.put(e, new HashSet<String>());
		}
		this.index = Collections.unmodifiableMap(index);
	}

	public Set<String> getIndex(DctmObjectTypesEnum type, String objectId) {
		return this.index.get(type);
	}

	public void serialize(IDfSession session, DctmObjectTypesEnum type, String objectId) throws IOException,
		CMSMFException {
	}
}