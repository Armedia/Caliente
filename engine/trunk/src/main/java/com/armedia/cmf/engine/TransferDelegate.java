package com.armedia.cmf.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TransferDelegate<T, S, V, C extends TransferContext<S, V>, F extends TransferDelegateFactory<S, V, C, E>, E extends TransferEngine<S, V, C, ?, ?, ?>> {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final F factory;
	protected final Class<T> objectClass;

	protected TransferDelegate(F factory, Class<T> objectClass) throws Exception {
		if (factory == null) { throw new IllegalArgumentException("Must provide a factory to process with"); }
		if (objectClass == null) { throw new IllegalArgumentException("Must provide an object class to work with"); }
		this.factory = factory;
		this.objectClass = objectClass;
	}

	protected final T castObject(Object o) {
		// This should NEVER fail...but if it does, it's well-deserved and we make no effort to
		// catch it or soften the blow
		return this.objectClass.cast(o);
	}

	public final Class<T> getObjectClass() {
		return this.objectClass;
	}

	protected abstract String calculateLabel(T object) throws Exception;
}