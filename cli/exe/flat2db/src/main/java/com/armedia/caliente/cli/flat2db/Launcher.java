package com.armedia.caliente.cli.flat2db;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;

public class Launcher extends AbstractLauncher {
	private static final int MIN_THREADS = 1;
	private static final int DEFAULT_THREADS = Math.min(16, Runtime.getRuntime().availableProcessors() * 2);
	private static final int MAX_THREADS = (Runtime.getRuntime().availableProcessors() * 3);

	private static final String REPORT_MARKER_FORMAT = "yyyyMMdd-HHmmss";

	private final ThreadsLaunchHelper threadsLaunchHelper = new ThreadsLaunchHelper(Launcher.MIN_THREADS,
		Launcher.DEFAULT_THREADS, Launcher.MAX_THREADS);

	@Override
	protected String getProgramName() {
		return "caliente-flat2db";
	}

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	@Override
	protected int run(OptionValues cli, String command, OptionValues commandValues, Collection<String> positionals)
		throws Exception {
		final String reportMarker = DateFormatUtils.format(new Date(), Launcher.REPORT_MARKER_FORMAT);
		System.setProperty("logName", String.format("%s-%s", getProgramName(), reportMarker));
		return 0;
	}

	@Override
	protected OptionScheme getOptionScheme() {
		return new OptionScheme(getProgramName()) //
			.addGroup( //
				this.threadsLaunchHelper.asGroup() //
			) //
			.addFrom( //
				Option.unwrap(CLIParam.values()) //
			) //
		;
	}
}