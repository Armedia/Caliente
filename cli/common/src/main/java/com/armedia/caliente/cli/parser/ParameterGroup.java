package com.armedia.caliente.cli.parser;

import java.util.Collection;

public interface ParameterGroup {

	public String getName();

	public String getDescription();

	public Collection<Parameter> getParameters();

}