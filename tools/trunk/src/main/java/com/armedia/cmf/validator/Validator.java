package com.armedia.cmf.validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.PooledWorkers;

public class Validator {

	private static final Logger LOG = LoggerFactory.getLogger(Validator.class);

	private static final Pattern VALID_FILENAME = Pattern
		.compile("^.*\\.metadata\\.properties\\.xml(?:\\.v\\d+(?:\\.\\d+)?)?$", Pattern.CASE_INSENSITIVE);

	private static abstract class MetadataFile {
		private final Path file;

		private MetadataFile(Path file) {
			this.file = file;
		}

		protected abstract void process();
	}

	private static abstract class FileScanner implements FileVisitor<Path> {
		private final PooledWorkers<?, MetadataFile> workers;

		private FileScanner(PooledWorkers<?, MetadataFile> workers) {
			this.workers = workers;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			// If the file is a metadata properties file, we submit it for loading.
			Matcher m = Validator.VALID_FILENAME.matcher(file.getFileName().toString());
			if (m.matches()) {
				try {
					this.workers.addWorkItem(wrapPath(file));
				} catch (InterruptedException e) {
					// Log the error...
					Validator.LOG.error(
						String.format("Failed to submit the file [%s] for processing - workers no longer working",
							file.toAbsolutePath().toString()),
						e);
					return FileVisitResult.TERMINATE;
				}
			}
			return FileVisitResult.CONTINUE;
		}

		protected abstract MetadataFile wrapPath(Path file);

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			Validator.LOG.warn(String.format("Failed to visit the file at [%s]", file.toAbsolutePath().toString()),
				exc);
			// We continue b/c we need to keep trying...
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}
	}

	static int run() throws Exception {
		Path p = new File("test").toPath();

		final PooledWorkers<Object, MetadataFile> workers = new PooledWorkers<Object, MetadataFile>(100000) {
			@Override
			protected Object prepare() throws Exception {
				return null;
			}

			@Override
			protected void process(Object state, MetadataFile item) throws Exception {
				item.process();
			}

			@Override
			protected void cleanup(Object state) {
				// Do nothing
			}
		};

		int threads = 16; // softcode this
		workers.start(threads, new MetadataFile(null) {
			@Override
			protected void process() {
				// Do nothing...
			}
		}, false);
		Files.walkFileTree(p, new FileScanner(workers) {

			@Override
			protected MetadataFile wrapPath(Path file) {
				return new MetadataFile(file) {
					@Override
					protected void process() {
						// Add it to the source map...
					}
				};
			}
		});
		return 0;
	}
}