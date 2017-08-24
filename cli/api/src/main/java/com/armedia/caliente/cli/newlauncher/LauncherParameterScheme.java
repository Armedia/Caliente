package com.armedia.caliente.cli.newlauncher;

import java.util.Collection;

import com.armedia.caliente.cli.ParameterDefinition;
import com.armedia.caliente.cli.ParameterScheme;

public final class LauncherParameterScheme {

	private ParameterScheme scheme;
	private boolean modified = false;

	LauncherParameterScheme(ParameterScheme scheme) {
		this.scheme = new ParameterScheme(scheme);
	}

	void markUnchanged() {
		this.modified = false;
	}

	boolean isModified() {
		return this.modified;
	}

	Collection<ParameterDefinition> removeParameter(ParameterDefinition parameterDefinition) {
		return this.scheme.removeParameter(parameterDefinition);
	}

	ParameterDefinition removeParameter(String longOpt) {
		return this.scheme.removeParameter(longOpt);
	}

	ParameterDefinition removeParameter(Character shortOpt) {
		return this.scheme.removeParameter(shortOpt);
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

	public void addParameter(ParameterDefinition parameterDefinition) {
		this.scheme.addParameter(parameterDefinition);
		this.modified = true;
	}

	public void addParameters(ParameterDefinition... parameters) {
		this.scheme.addParameters(parameters);
		this.modified = true;
	}

	public Collection<ParameterDefinition> getParameters() {
		return this.scheme.getParameters();
	}

	public Collection<ParameterDefinition> getRequiredParameters() {
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

	public int countCollisions(ParameterDefinition parameterDefinition) {
		return this.scheme.countCollisions(parameterDefinition);
	}

	public ParameterDefinition getParameter(String longOpt) {
		return this.scheme.getParameter(longOpt);
	}

	public ParameterDefinition getParameter(Character shortOpt) {
		return this.scheme.getParameter(shortOpt);
	}
}