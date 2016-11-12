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

	public static final int DEFAULT_MIN_THREADS = 1;
	public static final int DEFAULT_DEF_THREADS = (Runtime.getRuntime().availableProcessors() * 2);
	public static final int DEFAULT_MAX_THREADS = (Runtime.getRuntime().availableProcessors() * 4);

	private final int min;
	private final Integer def;
	private final int max;

	public ThreadsParameterSet() {
		this(ThreadsParameterSet.DEFAULT_MIN_THREADS, ThreadsParameterSet.DEFAULT_MAX_THREADS);
	}

	public ThreadsParameterSet(int max) {
		this(ThreadsParameterSet.DEFAULT_MIN_THREADS, max);
	}

	public ThreadsParameterSet(int min, int max) {
		min = Math.max(1, min);
		if (max < min) { throw new IllegalArgumentException(
			String.format("Maximum value %d is lower than minmum value %d", max, min)); }
		this.min = min;
		this.max = max;
		this.def = null;
	}

	public ThreadsParameterSet(int min, int def, int max) {
		min = Math.max(1, min);
		if (max < min) { throw new IllegalArgumentException(
			String.format("Maximum value %d is lower than minmum value %d", max, min)); }
		this.min = min;
		this.max = max;
		this.def = Tools.ensureBetween(min, def, max);
	}

	public int getMin() {
		return this.min;
	}

	public int getMax() {
		return this.max;
	}

	public boolean hasDefault() {
		return (this.def != null);
	}

	public Integer getDefault() {
		return this.def;
	}

	@Override
	public Collection<? extends ParameterDefinition> getParameterDefinitions(CommandLineValues commandLine) {
		return Collections.singleton(ThreadsParameterSet.THREADS);
	}

	public boolean hasThreads(CommandLineValues cli) {
		return cli.isPresent(ThreadsParameterSet.THREADS);
	}

	public Integer getThreads(CommandLineValues cli) {
		Integer t = cli.getInteger(ThreadsParameterSet.THREADS);
		if (t == null) { return this.def; }
		return Tools.ensureBetween(this.min, t.intValue(), this.max);
	}

	public int getThreads(CommandLineValues cli, int def) {
		return Tools.ensureBetween(this.min, cli.getInteger(ThreadsParameterSet.THREADS, def), this.max);
	}
}