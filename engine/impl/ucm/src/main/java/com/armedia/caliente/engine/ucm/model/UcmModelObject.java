package com.armedia.caliente.engine.ucm.model;

import java.util.concurrent.atomic.AtomicLong;

public abstract class UcmModelObject {

	protected final UcmModel model;

	protected final AtomicLong revision = new AtomicLong(0);

	UcmModelObject(UcmModel model) {
		this.model = model;
	}

	public final UcmModel getModel() {
		return this.model;
	}

	public abstract void refresh() throws UcmException;

	public boolean isStale() {
		return this.model.isStale(this);
	}
}