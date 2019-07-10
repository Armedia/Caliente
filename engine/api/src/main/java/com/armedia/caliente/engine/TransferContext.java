/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
/**
 *
 */

package com.armedia.caliente.engine;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.commons.utilities.CfgTools;

/**
 *
 *
 */
public abstract class TransferContext< //
	SESSION, //
	VALUE, //
	CONTEXT_FACTORY extends TransferContextFactory<SESSION, VALUE, ?, ?> //
> implements WarningTracker {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final String id;
	private final CONTEXT_FACTORY factory;
	private final String rootId;
	private final CmfObject.Archetype rootType;
	private final SESSION session;
	private final Map<String, VALUE> values = new HashMap<>();
	private final Map<String, Object> objects = new HashMap<>();
	private final CfgTools settings;
	private final Logger output;
	private final String productName;
	private final String productVersion;
	private final WarningTracker warningTracker;

	protected TransferContext(CONTEXT_FACTORY factory, CfgTools settings, String rootId, CmfObject.Archetype rootType,
		SESSION session, Logger output, WarningTracker warningTracker) {
		this.factory = factory;
		this.settings = settings;
		this.rootId = rootId;
		this.rootType = rootType;
		this.session = session;
		this.output = output;
		this.productName = factory.getProductName();
		this.productVersion = factory.getProductVersion();
		this.warningTracker = warningTracker;
		this.id = factory.getNextContextId();
	}

	public final String getId() {
		return this.id;
	}

	protected CONTEXT_FACTORY getFactory() {
		return this.factory;
	}

	public final CfgTools getSettings() {
		return this.settings;
	}

	public final String getRootObjectId() {
		return this.rootId;
	}

	public final CmfObject.Archetype getRootObjectType() {
		return this.rootType;
	}

	public final SESSION getSession() {
		return this.session;
	}

	private void assertValidName(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a value name"); }
	}

	public final VALUE getValue(String name) {
		assertValidName(name);
		return this.values.get(name);
	}

	public final VALUE setValue(String name, VALUE value) {
		assertValidName(name);
		if (value == null) { return clearValue(name); }
		return this.values.put(name, value);
	}

	public final VALUE clearValue(String name) {
		assertValidName(name);
		return this.values.remove(name);
	}

	public final boolean hasValue(String name) {
		assertValidName(name);
		return this.values.containsKey(name);
	}

	public final <T> T getObject(String name) {
		assertValidName(name);
		@SuppressWarnings("unchecked")
		T t = (T) this.objects.get(name);
		return t;
	}

	public final Object setObject(String name, Object value) {
		assertValidName(name);
		if (value == null) { return clearObject(name); }
		return this.objects.put(name, value);
	}

	public final Object clearObject(String name) {
		assertValidName(name);
		return this.objects.remove(name);
	}

	public final boolean hasObject(String name) {
		assertValidName(name);
		return this.objects.containsKey(name);
	}

	public final void printf(String format, Object... args) {
		if (this.output != null) {
			this.output.info(String.format(format, args));
		}
	}

	@Override
	public final void trackWarning(CmfObjectRef ref, String format, Object... args) {
		if (this.warningTracker != null) {
			this.warningTracker.trackWarning(ref, format, args);
		}
	}

	public final boolean isSupported(CmfObject.Archetype type) {
		return this.factory.isSupported(type);
	}

	public final String getProductName() {
		return this.productName;
	}

	public final String getProductVersion() {
		return this.productVersion;
	}

	public void close() {

	}
}