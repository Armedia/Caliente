package com.armedia.caliente.engine.transform;

import java.util.HashSet;
import java.util.Set;

import com.armedia.caliente.store.CmfType;

public class TestObjectFacade extends TransformableObject {

	private String objectId = null;
	private final String historyId = null;
	private boolean historyCurrent = false;
	private CmfType type = null;
	private String label = null;
	private String originalSubtype = null;
	private Set<String> originalDecorators = new HashSet<>();
	private Set<String> decorators = new HashSet<>();
	private int dependencyTier = 0;
	private String originalName = null;
	private String productName = null;
	private String productVersion = null;

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
	public CmfType getType() {
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

	public TestObjectFacade setType(CmfType type) {
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
	public Set<String> getOriginalDecorators() {
		return this.originalDecorators;
	}

	public TestObjectFacade setOriginalDecorators(Set<String> originalDecorators) {
		this.originalDecorators = originalDecorators;
		return this;
	}

	@Override
	public Set<String> getDecorators() {
		return this.decorators;
	}

	public TestObjectFacade setDecorators(Set<String> decorators) {
		this.decorators = decorators;
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
	public String getProductName() {
		return this.productName;
	}

	public TestObjectFacade setProductName(String productName) {
		this.productName = productName;
		return this;
	}

	@Override
	public String getProductVersion() {
		return this.productVersion;
	}

	public TestObjectFacade setProductVersion(String productVersion) {
		this.productVersion = productVersion;
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