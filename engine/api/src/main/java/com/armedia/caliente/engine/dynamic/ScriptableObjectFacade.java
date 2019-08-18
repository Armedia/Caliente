package com.armedia.caliente.engine.dynamic;

import java.util.Map;
import java.util.Set;

import com.armedia.caliente.store.CmfObject;

public interface ScriptableObjectFacade<P extends ScriptablePropertyFacade> {

	public String getObjectId();

	public String getHistoryId();

	public boolean isHistoryCurrent();

	public CmfObject.Archetype getType();

	public String getLabel();

	public String getOriginalSubtype();

	public String getSubtype();

	public String getName();

	public Set<String> getOriginalSecondarySubtypes();

	public Set<String> getSecondarySubtypes();

	public int getDependencyTier();

	public String getOriginalName();

	public Map<String, P> getAtt();

	public Map<String, P> getPriv();

	public default boolean isMutable() {
		return false;
	}
}