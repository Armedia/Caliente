package com.armedia.caliente.engine.ucm.model;

import java.util.Collection;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;

public class UcmCache {
	private static CacheAccess<String, UcmFSObject> OBJECTS = JCS.getInstance("FSObject");
	private static CacheAccess<String, Collection<UcmFSObject>> CHILDREN = JCS.getInstance("FSObjectChildren");;

	public UcmCache() {
		// TODO Auto-generated constructor stub
	}
}