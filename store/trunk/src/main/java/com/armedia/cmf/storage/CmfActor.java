package com.armedia.cmf.storage;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import com.armedia.commons.utilities.Tools;

public final class CmfActor implements Serializable, Comparable<CmfActor> {
	private static final long serialVersionUID = 1L;

	public static enum Type {
		//
		USER,
		GROUP,
		ROLE,
		//
		;
	}

	private final String name;
	private final Type actorType;

	private final Set<String> actions;

	public CmfActor(String name, Type actorType, String... actions) {
		this(name, actorType, (actions != null ? Arrays.asList(actions) : null));
	}

	public CmfActor(String name, Type actorType, Collection<String> actions) {
		this.name = name;
		this.actorType = actorType;
		this.actions = new TreeSet<String>();

		if (actions != null) {
			for (String p : actions) {
				this.actions.add(p);
			}
		}
	}

	public String getName() {
		return this.name;
	}

	public Type getType() {
		return this.actorType;
	}

	public int getActionCount() {
		return this.actions.size();
	}

	public boolean hasActions() {
		return !this.actions.isEmpty();
	}

	public Set<String> getActions() {
		return new TreeSet<String>(this.actions);
	}

	public boolean addAction(String permission) {
		if (permission == null) { throw new IllegalArgumentException("Must provide a non-null permission"); }
		return this.actions.add(permission);
	}

	public boolean removeAction(String permission) {
		if (permission == null) { throw new IllegalArgumentException("Must provide a non-null permission"); }
		return this.actions.remove(permission);
	}

	public boolean hasAction(String permission) {
		if (permission == null) { throw new IllegalArgumentException("Must provide a non-null permission"); }
		return this.actions.contains(permission);
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.actorType, this.name);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		CmfActor other = CmfActor.class.cast(obj);
		if (this.actorType != other.actorType) { return false; }
		if (!Tools.equals(this.name, other.name)) { return false; }
		return true;
	}

	@Override
	public int compareTo(CmfActor o) {
		if (o == null) { return 1; }
		int i = 0;
		i = Tools.compare(this.actorType, o.actorType);
		if (i != 0) { return i; }
		i = Tools.compare(this.name, o.name);
		if (i != 0) { return i; }
		return 0;
	}

	@Override
	public String toString() {
		return String.format("CmfActor [name=%s, actorType=%s, actions=%s]", this.name, this.actorType, this.actions);
	}

}