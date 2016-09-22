package com.armedia.cmf.validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.PooledWorkers;

public class Launcher {
	private static final Logger LOG = LoggerFactory.getLogger(Launcher.class);

	private static final int DEFAULT_THREADS = Math.min(16, Runtime.getRuntime().availableProcessors() * 2);

	private static boolean verifyPath(File f, String label) throws IOException {
		if (!f.exists()) {
			Launcher.LOG.error("The {} folder [{}] doesn't exist", label, f.getAbsolutePath());
			return false;
		}
		if (!f.isDirectory()) {
			Launcher.LOG.error("The location at [{}] is not a valid {} folder", f.getAbsolutePath(), label);
			return false;
		}
		if (!f.canRead()) {
			Launcher.LOG.error("The {} folder [{}] is not readable", label, f.getAbsolutePath());
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

		try {
			final File biFile = new File(CLIParam.bulk_import.getString()).getCanonicalFile();
			if (!Launcher.verifyPath(biFile, "bulk import")) { return 1; }
			final File beFile = new File(CLIParam.bulk_export.getString()).getCanonicalFile();
			if (!Launcher.verifyPath(beFile, "bulk export")) { return 1; }

			final Validator validator = new Validator(biFile.toPath(), beFile.toPath(), CLIParam.model.getAllString());

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

			final int threads = CLIParam.threads.getInteger(Launcher.DEFAULT_THREADS);
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

			// validator.reportOutcome(Launcher.LOG);

			return 0;
		} catch (Exception e) {
			Launcher.LOG.error("Launcher error", e);
			return 1;
		}
	}
}