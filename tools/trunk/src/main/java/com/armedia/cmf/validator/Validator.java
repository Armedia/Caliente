package com.armedia.cmf.validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;

public class Validator {

	private static final Logger LOG = LoggerFactory.getLogger(Validator.class);

	private static final Pattern VALID_FILENAME = Pattern
		.compile("^.*\\.metadata\\.properties\\.xml(?:\\.v\\d+(?:\\.\\d+)?)?$", Pattern.CASE_INSENSITIVE);

	private static enum ValidationErrorType {
		//
		EXCEPTION, // exception raised while examining
		CANDIDATE_MISSING, // source exists, but not target
		TYPE_MISMATCH, // type or aspects fail to meet requirements
		ASPECT_MISSING, // type or aspects fail to meet requirements
		ATTRIBUTE_VALUE, // attribute values are different
		CONTENT_SIZE, // content size mismatch
		CONTENT_SUM, // content sum mismatch
		//
		;
	}

	private static abstract class ValidationFault {
		protected final ValidationErrorType type;
		protected final Path source;
		protected final Path candidate;

		private ValidationFault(ValidationErrorType type, Path source, Path candidate) {
			this.type = type;
			this.source = source;
			this.candidate = candidate;
		}
	}

	private static class ExceptionFault extends ValidationFault {
		private final Throwable exception;
		private final boolean examiningSource;

		private ExceptionFault(Path source, Path candidate, Throwable exception, boolean examiningSource) {
			super(ValidationErrorType.EXCEPTION, source, candidate);
			this.exception = exception;
			this.examiningSource = examiningSource;
		}
	}

	private static class CandidateMissingFault extends ValidationFault {
		private final boolean sourceMissing;
		private final boolean candidateMissing;

		private CandidateMissingFault(Path source, Path candidate, int mode) {
			super(ValidationErrorType.CANDIDATE_MISSING, source, candidate);
			this.sourceMissing = (mode <= 0);
			this.candidateMissing = (mode >= 0);
		}
	}

	private static class TypeMismatchFault extends ValidationFault {
		private final String sourceType;
		private final String candidateType;

		private TypeMismatchFault(Path source, String sourceType, Path candidate, String candidateType) {
			super(ValidationErrorType.TYPE_MISMATCH, source, candidate);
			this.sourceType = sourceType;
			this.candidateType = candidateType;
		}
	}

	private static class AspectMissingFault extends ValidationFault {
		private final String sourceAspect;
		private final Set<String> candidateAspects;

		private AspectMissingFault(Path source, Path candidate, String sourceAspect, Set<String> candidateAspects) {
			super(ValidationErrorType.ASPECT_MISSING, source, candidate);
			this.sourceAspect = sourceAspect;
			this.candidateAspects = Tools.freezeCopy(candidateAspects, true);
		}
	}

	private static class AttributeFault<T extends Comparable<T>> extends ValidationFault {
		private final String name;
		private final T sourceValue;
		private final T candidateValue;

		private AttributeFault(Path source, Path candidate, String name, T sourceValue, T candidateValue) {
			super(ValidationErrorType.ATTRIBUTE_VALUE, source, candidate);
			this.name = name;
			this.sourceValue = sourceValue;
			this.candidateValue = candidateValue;
		}
	}

	private static class ContentSizeFault extends ValidationFault {
		private final int sourceSize;
		private final int candidateSize;

		private ContentSizeFault(Path source, Path candidate, int sourceSize, int candidateSize) {
			super(ValidationErrorType.CONTENT_SIZE, source, candidate);
			this.sourceSize = sourceSize;
			this.candidateSize = candidateSize;
		}
	}

	private static class ContentSumFault extends ValidationFault {
		private final String sourceSum;
		private final String candidateSum;

		private ContentSumFault(Path source, Path candidate, String sourceSum, String candidateSum) {
			super(ValidationErrorType.CONTENT_SUM, source, candidate);
			this.sourceSum = sourceSum;
			this.candidateSum = candidateSum;
		}
	}

	private static boolean verifyPath(File f, String label) throws IOException {
		if (!f.exists()) {
			Validator.LOG.error("The {} folder [{}] doesn't exist", label, f.getAbsolutePath());
			return false;
		}
		if (!f.isDirectory()) {
			Validator.LOG.error("The location at [{}] is not a valid {} folder", f.getAbsolutePath(), label);
			return false;
		}
		if (!f.canRead()) {
			Validator.LOG.error("The {} folder [{}] is not readable", label, f.getAbsolutePath());
			return false;
		}
		return true;
	}

	static abstract class FileVisitor extends SimpleFileVisitor<Path> {

		private final Logger log = LoggerFactory.getLogger(getClass());

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			// If the file is a metadata properties file, we submit it for loading.
			Matcher m = Validator.VALID_FILENAME.matcher(file.getFileName().toString());
			if (m.matches()) {
				try {
					processFile(file);
				} catch (InterruptedException e) {
					// Log the error...
					this.log.error(
						String.format("Failed to submit the file [%s] for processing - workers no longer working",
							file.toAbsolutePath().toString()),
						e);
					return FileVisitResult.TERMINATE;
				} catch (Exception e) {
					this.log.error(
						String.format("Failed to submit the file [%s] for processing - unexpected exception caught",
							file.toAbsolutePath().toString()),
						e);
				}
			}
			return FileVisitResult.CONTINUE;
		}

		protected abstract void processFile(Path file) throws Exception;

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
			this.log.warn(String.format("Failed to visit the file at [%s]", file.toAbsolutePath().toString()),
				exception);
			// We continue b/c we need to keep trying...
			return FileVisitResult.CONTINUE;
		}
	};

	private static int validateFile(File f) {
		return 0;
	}

	static void validate(Path sourceRoot, Path candidateRoot, Path source) {

		// Validate the source file against the target...
		final Path relativePath = source.relativize(sourceRoot);
		final Path candidate = candidateRoot.resolve(relativePath);

		// Test 0: We assume that this will pass, but check it anyway
		final File sourceFile = source.toFile();
		switch (Validator.validateFile(sourceFile)) {
			case 0: // all is well
				break;
			case 1: // Not there
				Validator.LOG.error("The source file at [{}] doesn't exist", source.toString());
				return;
			case 2: // Not file
				Validator.LOG.error("The source file at [{}] is not a regular file", source.toString());
				return;
			case 3: // not readable
				Validator.LOG.error("The source file at [{}] can't be read", source.toString());
				return;
			case 4: // unkonwn
				Validator.LOG.error("Can't access the source file at [{}] (unknown reason)", source.toString());
				return;
		}

		// Test 1: If the candidate doesn't exist, then we have a problem
		final File candidateFile = candidate.toFile();
		switch (Validator.validateFile(candidateFile)) {
			case 0: // all is well
				break;
			case 1: // Not there
				Validator.LOG.error("The candidate file at [{}] doesn't exist", source.toString());
				return;
			case 2: // Not file
				Validator.LOG.error("The candidate file at [{}] is not a regular file", source.toString());
				return;
			case 3: // not readable
				Validator.LOG.error("The candidate file at [{}] can't be read", source.toString());
				return;
			case 4: // unkonwn
				Validator.LOG.error("Can't access the candidate file at [{}] (unknown reason)", source.toString());
				return;
		}

		// Test 2: If the candidate isn't a regular file, then we have a problem
		// Test 3: Load both properties files, and begin property comparisons
		// Test 4: they must be of the same object type
		// Test 5: the aspects specified by candidate must be a superset of the aspects
		// specified by the source
		// Test 6: every property specified in source must match its corresponding
		// property on target (apply special typing rules for date, int, double, etc.)
		// Test 7: if source represents a file object, perform the size + checksum check
	}
}