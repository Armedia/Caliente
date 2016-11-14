package com.armedia.caliente.cli.parser;

import java.util.ArrayList;
import java.util.Collection;

public final class MutableParameterGroup implements ParameterGroup, Cloneable {

	private String name = null;
	private String description = null;
	private final Collection<Parameter> parameters = new ArrayList<>();

	public MutableParameterGroup() {
	}

	MutableParameterGroup(ParameterGroup other) {
		this.name = other.getName();
		this.description = other.getDescription();
		this.parameters.clear();
		Collection<Parameter> p = other.getParameters();
		if (p != null) {
			this.parameters.addAll(p);
		}
	}

	@Override
	public MutableParameterGroup clone() {
		return new MutableParameterGroup(this);
	}

	@Override
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ParameterGroup freezeCopy() {
		return new ImmutableParameterGroup(this);
	}

	@Override
	public Collection<Parameter> getParameters() {
		return this.parameters;
	}
}