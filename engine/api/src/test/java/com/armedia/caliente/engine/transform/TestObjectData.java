package com.armedia.caliente.engine.transform;

import java.util.HashSet;
import java.util.Set;

import com.armedia.caliente.store.CmfType;

public class TestObjectData extends ObjectData {

	private String objectId = null;
	private final String historyId = null;
	private boolean historyCurrent = false;
	private CmfType type = null;
	private String originalSubtype = null;
	private Set<String> originalDecorators = new HashSet<>();
	private Set<String> decorators = new HashSet<>();
	private int dependencyTier = 0;
	private String originalName = null;
	private String productName = null;
	private String productVersion = null;

	public TestObjectData() {
	}

	@Override
	public String getObjectId() {
		return this.objectId;
	}

	public TestObjectData setObjectId(String objectId) {
		this.objectId = objectId;
		return this;
	}

	@Override
	public boolean isHistoryCurrent() {
		return this.historyCurrent;
	}

	public TestObjectData setHistoryCurrent(boolean historyCurrent) {
		this.historyCurrent = historyCurrent;
		return this;
	}

	@Override
	public CmfType getType() {
		return this.type;
	}

	public TestObjectData setType(CmfType type) {
		this.type = type;
		return this;
	}

	@Override
	public String getOriginalSubtype() {
		return this.originalSubtype;
	}

	public TestObjectData setOriginalSubtype(String originalSubtype) {
		this.originalSubtype = originalSubtype;
		return this;
	}

	@Override
	public TestObjectData setSubtype(String subtype) {
		super.setSubtype(subtype);
		return this;
	}

	@Override
	public Set<String> getOriginalDecorators() {
		return this.originalDecorators;
	}

	public TestObjectData setOriginalDecorators(Set<String> originalDecorators) {
		this.originalDecorators = originalDecorators;
		return this;
	}

	@Override
	public Set<String> getDecorators() {
		return this.decorators;
	}

	public TestObjectData setDecorators(Set<String> decorators) {
		this.decorators = decorators;
		return this;
	}

	@Override
	public int getDependencyTier() {
		return this.dependencyTier;
	}

	public TestObjectData setDependencyTier(int dependencyTier) {
		this.dependencyTier = dependencyTier;
		return this;
	}

	@Override
	public TestObjectData setName(String name) {
		super.setName(name);
		return this;
	}

	@Override
	public String getProductName() {
		return this.productName;
	}

	public TestObjectData setProductName(String productName) {
		this.productName = productName;
		return this;
	}

	@Override
	public String getProductVersion() {
		return this.productVersion;
	}

	public TestObjectData setProductVersion(String productVersion) {
		this.productVersion = productVersion;
		return this;
	}

	@Override
	public String getHistoryId() {
		return this.historyId;
	}

	public TestObjectData setOriginalName(String originalName) {
		this.originalName = originalName;
		return this;
	}

	@Override
	public String getOriginalName() {
		return this.originalName;
	}
}