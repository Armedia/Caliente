package com.armedia.caliente.engine.transform;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.Tools;

public abstract class TransformableObjectFacade {

	protected final Map<String, TypedValue> attributes = new TreeMap<>();
	protected final Map<String, TypedValue> privateProperties = new TreeMap<>();

	private String subtype = null;
	private String name = null;

	public abstract String getObjectId();

	public abstract String getHistoryId();

	public abstract boolean isHistoryCurrent();

	public abstract CmfType getType();

	public abstract String getOriginalSubtype();

	public String getSubtype() {
		return Tools.coalesce(this.subtype, getOriginalSubtype());
	}

	public TransformableObjectFacade setSubtype(String subtype) {
		this.subtype = subtype;
		return this;
	}

	public String getName() {
		return Tools.coalesce(this.name, getOriginalName());
	}

	public TransformableObjectFacade setName(String name) {
		this.name = name;
		return this;
	}

	public abstract Set<String> getOriginalDecorators();

	public abstract Set<String> getDecorators();

	public abstract int getDependencyTier();

	public abstract String getOriginalName();

	public abstract String getProductName();

	public abstract String getProductVersion();

	public Map<String, TypedValue> getAtt() {
		return this.attributes;
	}

	public Map<String, TypedValue> getPriv() {
		return this.privateProperties;
	}
}