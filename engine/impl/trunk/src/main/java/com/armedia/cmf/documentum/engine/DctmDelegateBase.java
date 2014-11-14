package com.armedia.cmf.documentum.engine;

import org.apache.log4j.Logger;

import com.armedia.cmf.engine.TransferEngine;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

public class DctmDelegateBase<T extends IDfPersistentObject, E extends TransferEngine<IDfSession, IDfPersistentObject, IDfValue, ?>> {
	protected final Logger log = Logger.getLogger(getClass());

	private final Class<T> dfClass;
	private final DctmObjectType type;
	private final E engine;

	protected DctmDelegateBase(E engine, DctmObjectType type) {
		this.engine = engine;
		this.type = type;
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>) type.getDfClass();
		this.dfClass = c;
	}

	protected final E getEngine() {
		return this.engine;
	}

	protected final DctmObjectType getDctmType() {
		return this.type;
	}

	protected final T castObject(IDfPersistentObject object) throws DfException {
		if (object == null) { return null; }
		if (!this.dfClass.isAssignableFrom(object.getClass())) { throw new DfException(String.format(
			"Expected an object of class %s, but got one of class %s", this.dfClass.getCanonicalName(), object
			.getClass().getCanonicalName())); }
		return this.dfClass.cast(object);
	}
}