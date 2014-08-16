/**
 *
 */

package com.delta.cmsmf.io;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.delta.cmsmf.cmsobjects.DctmObject;
import com.delta.cmsmf.cmsobjects.DctmObjectTypesEnum;
import com.documentum.fc.client.IDfSession;

/**
 * 
 * @author diego.rivera@armedia.com
 * 
 */
public class DctmObjectGraph {

	private final DctmObject root;

	private final Map<DctmObjectTypesEnum, Map<String, DctmObject>> dependencies;

	public DctmObjectGraph(DctmObject root) {
		if (root == null) { throw new IllegalArgumentException("Must provide a valid root object"); }
		this.root = root;
		this.dependencies = new EnumMap<DctmObjectTypesEnum, Map<String, DctmObject>>(DctmObjectTypesEnum.class);
		for (DctmObjectTypesEnum type : DctmObjectTypesEnum.values()) {
			this.dependencies.put(type, new HashMap<String, DctmObject>());
		}
	}

	public void resolveDependencies(IDfSession session) {
	}

	public void addIdentity(DctmObjectTypesEnum type, String identity) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (identity == null) { throw new IllegalArgumentException("Must provide an object ID"); }
	}
}