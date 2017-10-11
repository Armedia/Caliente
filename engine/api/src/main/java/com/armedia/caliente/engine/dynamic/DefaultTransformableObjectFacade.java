package com.armedia.caliente.engine.dynamic;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;

public class DefaultTransformableObjectFacade extends DynamicObject {

	private final CmfObject<CmfValue> object;
	private final Set<String> originalSecondaries;
	private final Set<String> secondaries;

	public DefaultTransformableObjectFacade(CmfObject<CmfValue> object) {
		Objects.requireNonNull(object, "Must provide a CmfObject to pattern this instance on");
		this.object = object;
		for (CmfAttribute<CmfValue> att : object.getAttributes()) {
			this.attributes.put(att.getName(), new DynamicValue(att));
		}

		for (CmfProperty<CmfValue> prop : object.getProperties()) {
			this.privateProperties.put(prop.getName(), new DynamicValue(prop));
		}

		// TODO: Calculate the actual secondaries associated with the object...
		this.originalSecondaries = new LinkedHashSet<>();
		this.secondaries = new LinkedHashSet<>();
	}

	@Override
	public String getObjectId() {
		return this.object.getId();
	}

	@Override
	public String getHistoryId() {
		return this.object.getHistoryId();
	}

	@Override
	public boolean isHistoryCurrent() {
		return this.object.isHistoryCurrent();
	}

	@Override
	public CmfType getType() {
		return this.object.getType();
	}

	@Override
	public String getLabel() {
		return this.object.getLabel();
	}

	@Override
	public String getOriginalSubtype() {
		return this.object.getSubtype();
	}

	@Override
	public Set<String> getOriginalSecondarySubtypes() {
		return this.originalSecondaries;
	}

	@Override
	public Set<String> getSecondarySubtypes() {
		return this.secondaries;
	}

	@Override
	public int getDependencyTier() {
		return this.object.getDependencyTier();
	}

	@Override
	public String getOriginalName() {
		return this.object.getName();
	}

	@Override
	public String getProductName() {
		return this.object.getProductName();
	}

	@Override
	public String getProductVersion() {
		return this.object.getProductVersion();
	}

}