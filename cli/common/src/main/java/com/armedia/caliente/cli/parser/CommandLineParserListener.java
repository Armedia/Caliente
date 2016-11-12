package com.armedia.caliente.cli.parser;

import java.util.Collection;

public interface CommandLineParserListener {

	public void setParameter(CommandLineParameter p);

	public void setParameter(CommandLineParameter p, Collection<String> values);

	public void addRemainingParameters(Collection<String> remaining);
}