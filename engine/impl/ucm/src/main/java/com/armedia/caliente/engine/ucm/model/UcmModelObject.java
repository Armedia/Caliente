package com.armedia.caliente.engine.ucm.model;

import oracle.stellent.ridc.IdcClientException;

public abstract class UcmModelObject {

	protected final UcmModel model;

	UcmModelObject(UcmModel model) {
		this.model = model;
	}

	public final UcmModel getModel() {
		return this.model;
	}

	public abstract void refresh() throws IdcClientException;

}