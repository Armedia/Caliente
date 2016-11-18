package com.armedia.caliente.cli.validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;

import com.armedia.caliente.cli.launcher.AbstractLauncher;
import com.armedia.caliente.cli.launcher.LaunchParameterSet;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.utils.ThreadsParameterSet;
import com.armedia.commons.utilities.PooledWorkers;

public class Launcher extends AbstractLauncher implements LaunchParameterSet {
	private static final int MIN_THREADS = 1;
	private static final int DEFAULT_THREADS = Math.min(16, Runtime.getRuntime().availableProcessors() * 2);
	private static final int MAX_THREADS = (Runtime.getRuntime().availableProcessors() * 3);

	private static final String REPORT_MARKER_FORMAT = "yyyyMMdd-HHmmss";

	private static boolean verifyPath(Logger log, File f, String label) throws IOException {
		if (!f.exists()) {
			log.error("The {} folder [{}] doesn't exist", label, f.getAbsolutePath());
			return false;
		}
		if (!f.isDirectory()) {
			log.error("The location at [{}] is not a valid {} folder", f.getAbsolutePath(), label);
			return false;
		}
		if (!f.canRead()) {
			log.error("The {} folder [{}] is not readable", label, f.getAbsolutePath());
			return false;
		}
		return true;
	}

	private final ThreadsParameterSet threadsParameter = new ThreadsParameterSet(Launcher.MIN_THREADS,
		Launcher.DEFAULT_THREADS, Launcher.MAX_THREADS);

	@Override
	public Collection<CLIParam> getParameters(CommandLineValues commandLine) {
		return Arrays.asList(CLIParam.values());
	}

	@Override
	protected Collection<? extends LaunchParameterSet> getLaunchParameterSets(CommandLineValues cli, int pass) {
		if (pass > 0) { return null; }
		return Arrays.asList(this, this.threadsParameter);
	}

	@Override
	protected String getProgramName(int pass) {
		return "Caliente Validator";
	}

	public static final void main(String... args) {
		System.exit(new Launcher().launch(args));
	}

	@Override
	protected int run(CommandLineValues cli) throws Exception {
		final String reportMarker = DateFormatUtils.format(new Date(), Launcher.REPORT_MARKER_FORMAT);
		System.setProperty("logName", String.format("caliente-validator-%s", reportMarker));

		final File biFile = new File(cli.getString(CLIParam.bulk_import)).getCanonicalFile();
		if (!Launcher.verifyPath(this.log, biFile, "bulk import")) { return 1; }
		final File beFile = new File(cli.getString(CLIParam.bulk_export)).getCanonicalFile();
		if (!Launcher.verifyPath(this.log, beFile, "bulk export")) { return 1; }

		final String reportDirStr = cli.getString(CLIParam.report_dir, System.getProperty("user.dir"));
		File reportDir = new File(reportDirStr);
		try {
			reportDir = reportDir.getCanonicalFile();
		} catch (IOException e) {
			// do nothing...
		}
		reportDir = reportDir.getAbsoluteFile();

		try {
			FileUtils.forceMkdir(reportDir);
		} catch (IOException e) {
			this.log.error("Failed to ensure that the target directory [{}] exists", reportDir.getAbsolutePath());
			return 1;
		}

		final int threads = this.threadsParameter.getThreads(cli);

		this.log.info("Starting validation with {} thread{}", threads, threads > 1 ? "s" : "");
		Runtime runtime = Runtime.getRuntime();
		this.log.info(String.format("Current heap size: %d MB", runtime.totalMemory() / 1024 / 1024));
		this.log.info(String.format("Maximum heap size: %d MB", runtime.maxMemory() / 1024 / 1024));
		this.log.info("Bulk Import path: [{}]", biFile.getAbsolutePath());
		this.log.info("Bulk Export path: [{}]", beFile.getAbsolutePath());
		this.log.info("Report directory: [{}]", reportDir.getAbsolutePath());
		this.log.info("Content models  : {}", cli.getAllStrings(CLIParam.model));
		this.log.info("Report marker   : [{}]", reportMarker);

		final Validator validator = new Validator(reportDir.toPath(), biFile.toPath(), beFile.toPath(),
			cli.getAllStrings(CLIParam.model), reportMarker);
		final long start = System.currentTimeMillis();
		try {
			final PooledWorkers<Object, Path> workers = new PooledWorkers<Object, Path>() {
				@Override
				protected Object prepare() throws Exception {
					return null;
				}

				@Override
				protected void process(Object state, Path source) throws Exception {
					validator.validate(source);
				}

				@Override
				protected void cleanup(Object state) {
					// Do nothing
				}
			};

			final Path endPath = Paths.get("");
			workers.start(threads, endPath, true);

			try {
				Files.walkFileTree(validator.getSourceRoot(), validator.new FileVisitor() {
					@Override
					protected void processFile(Path file) throws Exception {
						workers.addWorkItem(file);
					}
				});
			} finally {
				workers.waitForCompletion();
			}
		} finally {
			long duration = System.currentTimeMillis() - start;
			final long hours = TimeUnit.MILLISECONDS.toHours(duration);
			duration -= TimeUnit.HOURS.toMillis(hours);
			final long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
			duration -= TimeUnit.MINUTES.toMillis(minutes);
			final long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
			duration -= TimeUnit.SECONDS.toMillis(seconds);

			try {
				validator.writeAndClear();
			} finally {
				this.log.info(String.format("Total duration: %d:%02d:%02d.%03d", hours, minutes, seconds, duration));
			}
		}

		return 0;
	}
}