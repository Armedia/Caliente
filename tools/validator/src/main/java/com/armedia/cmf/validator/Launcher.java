package com.armedia.cmf.validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.Tools;

public class Launcher {
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

	public static final void main(String... args) {
		System.exit(Launcher.runMain(args));
	}

	private static int runMain(String... args) {
		if (!CLIParam.parse(args)) {
			// If the parameters didn't parse, we fail.
			return 1;
		}
		final String reportMarker = DateFormatUtils.format(new Date(), Launcher.REPORT_MARKER_FORMAT);
		System.setProperty("logName", String.format("caliente-validator-%s", reportMarker));
		final Logger log = LoggerFactory.getLogger(Launcher.class);

		try {
			final File biFile = new File(CLIParam.bulk_import.getString()).getCanonicalFile();
			if (!Launcher.verifyPath(log, biFile, "bulk import")) { return 1; }
			final File beFile = new File(CLIParam.bulk_export.getString()).getCanonicalFile();
			if (!Launcher.verifyPath(log, beFile, "bulk export")) { return 1; }

			final String reportDirStr = CLIParam.report_dir.getString(System.getProperty("user.dir"));
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
				log.error("Failed to ensure that the target directory [{}] exists", reportDir.getAbsolutePath());
				return 1;
			}

			final int threads = Tools.ensureBetween(1, CLIParam.threads.getInteger(Launcher.DEFAULT_THREADS),
				Launcher.MAX_THREADS);

			log.info("Starting validation with {} thread{}", threads, threads > 1 ? "s" : "");
			Runtime runtime = Runtime.getRuntime();
			log.info(String.format("Current heap size: %d MB", runtime.totalMemory() / 1024 / 1024));
			log.info(String.format("Maximum heap size: %d MB", runtime.maxMemory() / 1024 / 1024));
			log.info("Bulk Import path: [{}]", biFile.getAbsolutePath());
			log.info("Bulk Export path: [{}]", beFile.getAbsolutePath());
			log.info("Report directory: [{}]", reportDir.getAbsolutePath());
			log.info("Content models  : {}", CLIParam.model.getAllString());
			log.info("Report marker   : [{}]", reportMarker);

			final Validator validator = new Validator(reportDir.toPath(), biFile.toPath(), beFile.toPath(),
				CLIParam.model.getAllString(), reportMarker);
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
					log.info(String.format("Total duration: %d:%02d:%02d.%03d", hours, minutes, seconds, duration));
				}
			}

			return 0;
		} catch (Exception e) {
			log.error("Launcher error", e);
			return 1;
		}
	}
}
