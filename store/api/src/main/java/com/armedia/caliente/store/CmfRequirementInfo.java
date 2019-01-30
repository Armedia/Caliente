package com.armedia.caliente.store;

public class CmfRequirementInfo<T extends Enum<T>> extends CmfObjectRef {
	private static final long serialVersionUID = 1L;

	private final T status;
	private final String data;

	public CmfRequirementInfo(CmfObjectRef other, T status, String data) {
		super(other);
		this.status = status;
		this.data = data;
	}

	public CmfRequirementInfo(CmfArchetype type, String id, T status, String data) {
		super(type, id);
		this.status = status;
		this.data = data;
	}

	public T getStatus() {
		return this.status;
	}

	public String getData() {
		return this.data;
	}
}