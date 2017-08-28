package com.armedia.caliente.cli.launcher;

import java.util.Collection;

import com.armedia.caliente.cli.DuplicateParameterException;
import com.armedia.caliente.cli.InvalidParameterException;
import com.armedia.caliente.cli.Parameter;
import com.armedia.caliente.cli.ParameterScheme;

class ExtensibleParameterScheme {

	private ParameterScheme scheme;
	private boolean modified = false;

	ExtensibleParameterScheme(ParameterScheme scheme) {
		this.scheme = new ParameterScheme(scheme);
	}

	boolean isModified() {
		return this.modified;
	}

	public String getName() {
		return this.scheme.getName();
	}

	public boolean isSupportsArguments() {
		return this.scheme.isSupportsArguments();
	}

	public int getMinArgs() {
		return this.scheme.getMinArgs();
	}

	public int getMaxArgs() {
		return this.scheme.getMaxArgs();
	}

	public ExtensibleParameterScheme addParameter(Parameter parameter)
		throws InvalidParameterException, DuplicateParameterException {
		this.scheme.addParameter(parameter);
		this.modified = true;
		return this;
	}

	public Collection<Parameter> getParameters() {
		return this.scheme.getParameters();
	}

	public Collection<Parameter> getRequiredParameters() {
		return this.scheme.getRequiredParameters();
	}

	public int getRequiredParameterCount() {
		return this.scheme.getRequiredParameterCount();
	}

	public int getParameterCount() {
		return this.scheme.getParameterCount();
	}

	public boolean hasParameter(Character shortOpt) {
		return this.scheme.hasParameter(shortOpt);
	}

	public boolean hasParameter(String longOpt) {
		return this.scheme.hasParameter(longOpt);
	}

	public int countCollisions(Parameter parameter) {
		return this.scheme.countCollisions(parameter);
	}

	public Parameter getParameter(String longOpt) {
		return this.scheme.getParameter(longOpt);
	}

	public Parameter getParameter(Character shortOpt) {
		return this.scheme.getParameter(shortOpt);
	}
}