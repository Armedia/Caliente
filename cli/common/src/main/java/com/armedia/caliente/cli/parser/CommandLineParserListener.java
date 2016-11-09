package com.armedia.caliente.cli.parser;

import java.util.Collection;

public interface CommandLineParserListener {

	public void setParameter(Parameter p);

	public void setParameter(Parameter p, Collection<String> values);

	public void addRemainingParameters(Collection<String> remaining);
}