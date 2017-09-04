package com.armedia.caliente.cli;

public interface PositionalValueSupport {

	public static final int UNLIMITED = -1;

	public String getArgumentName();

	public int getMinArguments();

	public int getMaxArguments();

}