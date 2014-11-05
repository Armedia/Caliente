/**
 *
 */

package com.armedia.cmf.engine;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public abstract class TransferContext<S, T, V> {

	private final String rootId;
	private final S session;
	private final Map<String, V> values = new HashMap<String, V>();
	private final Map<String, Object> objects = new HashMap<String, Object>();
	private final Logger output;

	protected TransferContext(String rootId, S session, Logger output) {
		this.rootId = rootId;
		this.session = session;
		this.output = output;
	}

	public final String getRootObjectId() {
		return this.rootId;
	}

	public final S getSession() {
		return this.session;
	}

	private void assertValidName(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a value name"); }
	}

	public final V getValue(String name) {
		assertValidName(name);
		return this.values.get(name);
	}

	public final V setValue(String name, V value) {
		assertValidName(name);
		if (value == null) { return clearValue(name); }
		return this.values.put(name, value);
	}

	public final V clearValue(String name) {
		assertValidName(name);
		return this.values.remove(name);
	}

	public final boolean hasValue(String name) {
		assertValidName(name);
		return this.values.containsKey(name);
	}

	public final Object getObject(String name) {
		assertValidName(name);
		return this.objects.get(name);
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
}