package com.armedia.caliente.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TransferDelegate< //
	ECM_OBJECT, //
	SESSION, //
	VALUE, //
	CONTEXT extends TransferContext<SESSION, VALUE, ?>, //
	DELEGATE_FACTORY extends TransferDelegateFactory<SESSION, VALUE, CONTEXT, ENGINE>, //
	ENGINE extends TransferEngine<?, ?, ?, SESSION, VALUE, CONTEXT, ?, DELEGATE_FACTORY, ?> //
> {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final DELEGATE_FACTORY factory;
	protected final Class<ECM_OBJECT> objectClass;

	protected TransferDelegate(DELEGATE_FACTORY factory, Class<ECM_OBJECT> objectClass) throws Exception {
		if (factory == null) { throw new IllegalArgumentException("Must provide a factory to process with"); }
		if (objectClass == null) { throw new IllegalArgumentException("Must provide an object class to work with"); }
		this.factory = factory;
		this.objectClass = objectClass;
	}

	protected final ECM_OBJECT castObject(Object o) {
		// This should NEVER fail...but if it does, it's well-deserved and we make no effort to
		// catch it or soften the blow
		return this.objectClass.cast(o);
	}

	public final Class<ECM_OBJECT> getObjectClass() {
		return this.objectClass;
	}
}