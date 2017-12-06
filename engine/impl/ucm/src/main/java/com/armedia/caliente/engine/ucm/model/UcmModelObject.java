package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public abstract class UcmModelObject {

	private final UcmModel model;
	protected final URI uri;

	protected final AtomicLong revision = new AtomicLong(0);

	UcmModelObject(UcmModel model, URI uri) {
		Objects.requireNonNull(model, "Must provide a model to associate this object with");
		Objects.requireNonNull(uri, "Must provide a URI to identify this object with");
		this.model = model;
		this.uri = uri;
	}

	public final UcmModel getModel() {
		return this.model;
	}

	public final URI getURI() {
		return this.uri;
	}
}