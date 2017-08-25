package com.armedia.caliente.cli.utils;

import java.util.Collection;
import java.util.Collections;

import com.armedia.caliente.cli.CommandLineValues;
import com.armedia.caliente.cli.ParameterImpl;
import com.armedia.caliente.cli.Parameter;
import com.armedia.caliente.cli.launcher.LaunchParameterSet;
import com.armedia.commons.utilities.Tools;

public final class ThreadsLaunchHelper implements LaunchParameterSet {

	private static final Parameter THREADS = new ParameterImpl() //
		.setShortOpt('t') //
		.setLongOpt("threads") //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("threads") //
		.setDescription("The number of threads to use");

	public static final int DEFAULT_MIN_THREADS = 1;
	public static final int DEFAULT_DEF_THREADS = (Runtime.getRuntime().availableProcessors() * 2);
	public static final int DEFAULT_MAX_THREADS = (Runtime.getRuntime().availableProcessors() * 4);

	private final int min;
	private final Integer def;
	private final int max;

	public ThreadsLaunchHelper() {
		this(ThreadsLaunchHelper.DEFAULT_MIN_THREADS, ThreadsLaunchHelper.DEFAULT_MAX_THREADS);
	}

	public ThreadsLaunchHelper(int max) {
		this(ThreadsLaunchHelper.DEFAULT_MIN_THREADS, max);
	}

	public ThreadsLaunchHelper(int min, int max) {
		min = Math.max(1, min);
		if (max < min) { throw new IllegalArgumentException(
			String.format("Maximum value %d is lower than minmum value %d", max, min)); }
		this.min = min;
		this.max = max;
		this.def = null;
	}

	public ThreadsLaunchHelper(int min, int def, int max) {
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
	public Collection<? extends Parameter> getParameters(CommandLineValues commandLine) {
		return Collections.singleton(ThreadsLaunchHelper.THREADS);
	}

	public boolean hasThreads(CommandLineValues cli) {
		return cli.isPresent(ThreadsLaunchHelper.THREADS);
	}

	public Integer getThreads(CommandLineValues cli) {
		Integer t = cli.getInteger(ThreadsLaunchHelper.THREADS);
		if (t == null) { return this.def; }
		return Tools.ensureBetween(this.min, t.intValue(), this.max);
	}

	public int getThreads(CommandLineValues cli, int def) {
		return Tools.ensureBetween(this.min, cli.getInteger(ThreadsLaunchHelper.THREADS, def), this.max);
	}
}