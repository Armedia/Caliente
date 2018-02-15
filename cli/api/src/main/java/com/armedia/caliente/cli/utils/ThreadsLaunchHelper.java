package com.armedia.caliente.cli.utils;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.Options;
import com.armedia.commons.utilities.Tools;

public final class ThreadsLaunchHelper extends Options {

	public static final int DEFAULT_MIN_THREADS = 1;
	public static final int DEFAULT_DEF_THREADS = (Runtime.getRuntime().availableProcessors() * 2);
	public static final int DEFAULT_MAX_THREADS = (Runtime.getRuntime().availableProcessors() * 4);

	public static final Option THREADS = new OptionImpl() //
		.setShortOpt('t') //
		.setLongOpt("threads") //
		.setMinArguments(1) //
		.setMaxArguments(1) //
		.setDefault(String.valueOf(ThreadsLaunchHelper.DEFAULT_DEF_THREADS)) //
		.setArgumentName("threads") //
		.setDescription("The number of threads to use") //
	;

	private final int min;
	private final Integer def;
	private final int max;

	private final OptionGroupImpl group;

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
		this.group = new OptionGroupImpl("Threading") //
			.add(ThreadsLaunchHelper.THREADS) //
		;
	}

	public ThreadsLaunchHelper(int min, int def, int max) {
		min = Math.max(1, min);
		if (max < min) { throw new IllegalArgumentException(
			String.format("Maximum value %d is lower than minmum value %d", max, min)); }
		this.min = min;
		this.max = max;
		this.def = Tools.ensureBetween(min, def, max);
		this.group = new OptionGroupImpl("Threading") //
			.add(ThreadsLaunchHelper.THREADS) //
		;
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

	public boolean hasThreads(OptionValues cli) {
		return cli.isPresent(ThreadsLaunchHelper.THREADS);
	}

	public Integer getThreads(OptionValues cli) {
		Integer t = cli.getInteger(ThreadsLaunchHelper.THREADS);
		if (t == null) { return this.def; }
		return Tools.ensureBetween(this.min, t.intValue(), this.max);
	}

	public int getThreads(OptionValues cli, int def) {
		return Tools.ensureBetween(this.min, cli.getInteger(ThreadsLaunchHelper.THREADS, def), this.max);
	}

	@Override
	public OptionGroup asGroup(String name) {
		return this.group.getCopy(name);
	}
}