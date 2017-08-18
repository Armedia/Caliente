package com.armedia.caliente.cli.newlauncher;

import java.util.Collection;

import com.armedia.caliente.cli.Parameter;
import com.armedia.caliente.cli.ParameterScheme;

public final class CommandLineScheme {

	private ParameterScheme scheme;
	private boolean modified = false;

	public CommandLineScheme(ParameterScheme scheme) {
		this.scheme = scheme;
	}

	void markUnchanged() {
		this.modified = false;
	}

	boolean isModified() {
		return this.modified;
	}

	public ParameterScheme getScheme() {
		return this.scheme;
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

	public void addParameter(Parameter parameter) {
		this.scheme.addParameter(parameter);
		this.modified = true;
	}

	public void addParameters(Parameter... parameters) {
		this.scheme.addParameters(parameters);
		this.modified = true;
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