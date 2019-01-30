package com.armedia.caliente.engine.transform;

import java.util.HashSet;
import java.util.Set;

import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.store.CmfObject;

public class TestObjectFacade extends DynamicObject {

	private String objectId = null;
	private final String historyId = null;
	private boolean historyCurrent = false;
	private CmfObject.Archetype type = null;
	private String label = null;
	private String originalSubtype = null;
	private Set<String> originalSecondaries = new HashSet<>();
	private Set<String> secondaries = new HashSet<>();
	private int dependencyTier = 0;
	private String originalName = null;

	public TestObjectFacade() {
	}

	@Override
	public String getObjectId() {
		return this.objectId;
	}

	public TestObjectFacade setObjectId(String objectId) {
		this.objectId = objectId;
		return this;
	}

	@Override
	public boolean isHistoryCurrent() {
		return this.historyCurrent;
	}

	public TestObjectFacade setHistoryCurrent(boolean historyCurrent) {
		this.historyCurrent = historyCurrent;
		return this;
	}

	@Override
	public CmfObject.Archetype getType() {
		return this.type;
	}

	@Override
	public String getLabel() {
		return this.label;
	}

	public TestObjectFacade setLabel(String label) {
		this.label = label;
		return this;
	}

	public TestObjectFacade setType(CmfObject.Archetype type) {
		this.type = type;
		return this;
	}

	@Override
	public String getOriginalSubtype() {
		return this.originalSubtype;
	}

	public TestObjectFacade setOriginalSubtype(String originalSubtype) {
		this.originalSubtype = originalSubtype;
		return this;
	}

	@Override
	public TestObjectFacade setSubtype(String subtype) {
		super.setSubtype(subtype);
		return this;
	}

	@Override
	public Set<String> getOriginalSecondarySubtypes() {
		return this.originalSecondaries;
	}

	public TestObjectFacade setOriginalSecondarySubtypes(Set<String> originalSecondaries) {
		this.originalSecondaries = originalSecondaries;
		return this;
	}

	@Override
	public Set<String> getSecondarySubtypes() {
		return this.secondaries;
	}

	public TestObjectFacade setSecondarySubtypes(Set<String> secondaries) {
		this.secondaries = secondaries;
		return this;
	}

	@Override
	public int getDependencyTier() {
		return this.dependencyTier;
	}

	public TestObjectFacade setDependencyTier(int dependencyTier) {
		this.dependencyTier = dependencyTier;
		return this;
	}

	@Override
	public TestObjectFacade setName(String name) {
		super.setName(name);
		return this;
	}

	@Override
	public String getHistoryId() {
		return this.historyId;
	}

	public TestObjectFacade setOriginalName(String originalName) {
		this.originalName = originalName;
		return this;
	}

	@Override
	public String getOriginalName() {
		return this.originalName;
	}
}