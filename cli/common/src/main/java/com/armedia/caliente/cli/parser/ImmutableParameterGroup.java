package com.armedia.caliente.cli.parser;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.armedia.commons.utilities.Tools;

public final class ImmutableParameterGroup implements ParameterGroup {

	private final String name;
	private final String description;
	private final Collection<Parameter> parameters;

	ImmutableParameterGroup(ParameterGroup other) {
		this.name = other.getName();
		this.description = other.getDescription();
		List<Parameter> parameters = new LinkedList<>();
		Collection<Parameter> p = other.getParameters();
		if (p != null) {
			parameters.addAll(p);
		}
		this.parameters = Tools.freezeList(parameters);
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public Collection<Parameter> getParameters() {
		return this.parameters;
	}
}