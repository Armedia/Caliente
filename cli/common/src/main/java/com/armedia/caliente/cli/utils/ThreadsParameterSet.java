package com.armedia.caliente.cli.utils;

import java.util.Collection;
import java.util.Collections;

import com.armedia.caliente.cli.launcher.LaunchParameterSet;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.MutableParameterDefinition;
import com.armedia.caliente.cli.parser.ParameterDefinition;
import com.armedia.commons.utilities.Tools;

public final class ThreadsParameterSet implements LaunchParameterSet {

	private static final ParameterDefinition THREADS = new MutableParameterDefinition() //
		.setShortOpt('t') //
		.setValueCount(1) //
		.setValueOptional(false) //
		.setValueName("threads") //
		.setDescription("The number of threads to use") //
		.freezeCopy();

	@Override
	public Collection<? extends ParameterDefinition> getParameterDefinitions(CommandLineValues commandLine) {
		return Collections.singleton(ThreadsParameterSet.THREADS);
	}

	public boolean hasThreads(CommandLineValues cli) {
		return cli.isPresent(ThreadsParameterSet.THREADS);
	}

	public Integer getThreads(CommandLineValues cli) {
		return cli.getInteger(ThreadsParameterSet.THREADS);
	}

	public int getThreads(CommandLineValues cli, int def) {
		return cli.getInteger(ThreadsParameterSet.THREADS, def);
	}

	public int getThreads(CommandLineValues cli, int min, int def, int max) {
		return Tools.ensureBetween(min, cli.getInteger(ThreadsParameterSet.THREADS, def), max);
	}

}