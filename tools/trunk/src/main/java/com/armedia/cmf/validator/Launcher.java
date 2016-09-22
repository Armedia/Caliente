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

			final Path biPath = biFile.toPath();
			final Path bePath = beFile.toPath();

			final PooledWorkers<Object, Path> workers = new PooledWorkers<Object, Path>() {
				@Override
				protected Object prepare() throws Exception {
					return null;
				}

				@Override
				protected void process(Object state, Path source) throws Exception {
					Validator.validate(biPath, bePath, source);
				}

				@Override
				protected void cleanup(Object state) {
					// Do nothing
				}
			};

			final int threads = CLIParam.threads.getInteger(Launcher.DEFAULT_THREADS);
			workers.start(threads, Paths.get(""), false);

			Files.walkFileTree(biPath, new Validator.FileVisitor() {

				@Override
				protected void processFile(Path file) throws Exception {
					workers.addWorkItem(file);
				}
			});

			return 0;
		} catch (Exception e) {
			Launcher.LOG.error("Exception caught", e);
			return 1;
		}
	}
}