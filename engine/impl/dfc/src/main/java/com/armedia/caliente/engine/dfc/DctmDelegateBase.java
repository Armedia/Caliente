package com.armedia.caliente.engine.dfc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.store.CmfObject;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

public class DctmDelegateBase<T extends IDfPersistentObject, E extends TransferEngine<?, ?, ?, IDfSession, IDfValue, ?, ?, ?, ?>> {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Class<T> dfClass;
	private final DctmObjectType type;
	private final E engine;

	protected DctmDelegateBase(E engine, CmfObject.Archetype type) {
		this(engine, DctmObjectType.decodeType(type));
	}

	protected DctmDelegateBase(E engine, DctmObjectType type) {
		if (engine == null) {
			throw new IllegalArgumentException("Must provide the engine that will interact with this delegate");
		}
		if (type == null) {
			throw new IllegalArgumentException("Must provide the object type for which this delegate will operate");
		}
		this.engine = engine;
		this.type = type;
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>) type.getDfClass();
		this.dfClass = c;
	}

	protected final Class<T> getDfClass() {
		return this.dfClass;
	}

	protected final E getEngine() {
		return this.engine;
	}

	protected final DctmObjectType getDctmType() {
		return this.type;
	}

	protected final T castObject(IDfPersistentObject object) throws DfException {
		if (object == null) { return null; }
		if (!this.dfClass.isInstance(object)) {
			throw new DfException(String.format("Expected an object of class %s, but got one of class %s",
				this.dfClass.getCanonicalName(), object.getClass().getCanonicalName()));
		}
		return this.dfClass.cast(object);
	}
}