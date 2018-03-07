package com.armedia.caliente.cli.caliente.newlauncher;

public interface CommandModule extends AutoCloseable {

	public String getName();

	public int run() throws Exception;

}