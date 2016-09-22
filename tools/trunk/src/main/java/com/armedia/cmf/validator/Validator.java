package com.armedia.cmf.validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoSchema;
import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.cmf.engine.alfresco.bulk.importer.model.SchemaAttribute;
import com.armedia.commons.utilities.BinaryEncoding;
import com.armedia.commons.utilities.Tools;

public class Validator {

	private static final Logger LOG = LoggerFactory.getLogger(Validator.class);

	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

	private static final Pattern VALID_FILENAME = Pattern
		.compile("^.*\\.metadata\\.properties\\.xml(?:\\.v\\d+(?:\\.\\d+)?)?$", Pattern.CASE_INSENSITIVE);

	private static final int BUFFER_SIZE = (int) FileUtils.ONE_MB;

	private static final String METADATA_MARKER = ".metadata.properties.xml";
	private static final String METADATA_MARKER_PATTERN = String.format("\\Q%s\\E", Validator.METADATA_MARKER);
	private static final Pattern CHECKSUM_PARSER = Pattern.compile("^([^:]+):((?:[a-f0-9]{2})+)$",
		Pattern.CASE_INSENSITIVE);

	private static final String CONTENT_TYPE = "cm:content";

	private static final String PROP_TYPE = "type";
	private static final String PROP_ASPECTS = "aspects";
	private static final String PROP_CHECKSUM = "streamChecksum";
	// private static final String PROP_CONTENT_SIZE = "streamLength";

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

	private static abstract class ValidationFault implements Comparable<ValidationFault> {
		protected final ValidationErrorType type;
		protected final Path source;
		protected final Path candidate;

		private ValidationFault(ValidationErrorType type, Path source, Path candidate) {
			this.type = type;
			this.source = source;
			this.candidate = candidate;
		}

		private CSVPrinter createOutputFile(File baseDir) throws IOException {
			// Open the file
			File targetFile = new File(baseDir, String.format("faults.%s.csv", this.type.name().toLowerCase()));

			// Create the CSVPrinter
			CSVPrinter p = new CSVPrinter(new FileWriter(targetFile), CSVFormat.DEFAULT);

			// Print out the headers
			writeHeaders(p);
			p.println();
			p.flush();

			// Return the printer
			return p;
		}

		protected void writeColumns(CSVPrinter p) throws IOException {
			p.print(this.type.name());
			p.print(this.source.toString());
			p.print(this.candidate.toString());
		}

		protected final void writeRecord(CSVPrinter p) throws IOException {
			writeColumns(p);
			p.println();
			p.flush();
		}

		protected void writeHeaders(CSVPrinter p) throws IOException {
			p.print("FAULT TYPE");
			p.print("SOURCE PATH");
			p.print("CANDIDATE PATH");
		}

		@Override
		public int compareTo(ValidationFault o) {
			if (o == null) { return 1; }
			int r = Tools.compare(this.type, o.type);
			if (r != 0) { return r; }
			r = Tools.compare(this.source, o.source);
			if (r != 0) { return r; }
			r = Tools.compare(this.candidate, o.candidate);
			if (r != 0) { return r; }
			return 0;
		}
	}

	public static class ExceptionFault extends ValidationFault {
		private final String message;
		private final Boolean examiningSource;
		private final Throwable exception;

		private ExceptionFault(Path source, Path candidate, String message, Throwable exception,
			Boolean examiningSource) {
			super(ValidationErrorType.EXCEPTION, source, candidate);
			this.message = message;
			this.exception = exception;
			this.examiningSource = examiningSource;
		}

		@Override
		protected void writeColumns(CSVPrinter p) throws IOException {
			super.writeColumns(p);
			String stage = "(unknown stage)";
			if (this.examiningSource != null) {
				stage = String.format("While examining the %s file", this.examiningSource ? "source" : "candidate");
			}
			p.print(stage);
			p.print(this.message);
			p.print(Tools.dumpStackTrace(this.exception));
		}

		@Override
		protected void writeHeaders(CSVPrinter p) throws IOException {
			super.writeHeaders(p);
			p.print("STAGE");
			p.print("MESSAGE");
			p.print("EXCEPTION DUMP");
		}
	}

	public static class ObjectMissingFault extends ValidationFault {
		private final String sourceMessage;
		private final String candidateMessage;

		private ObjectMissingFault(Path source, String sourceMessage, Path candidate, String candidateMessage) {
			super(ValidationErrorType.CANDIDATE_MISSING, source, candidate);
			this.sourceMessage = sourceMessage;
			this.candidateMessage = candidateMessage;
		}

		@Override
		protected void writeColumns(CSVPrinter p) throws IOException {
			super.writeColumns(p);
			p.print(this.sourceMessage);
			p.print(this.candidateMessage);
		}

		@Override
		protected void writeHeaders(CSVPrinter p) throws IOException {
			super.writeHeaders(p);
			p.print("SOURCE ERROR");
			p.print("CANDIDATE ERROR");
		}
	}

	public static class TypeMismatchFault extends ValidationFault {
		private final String sourceType;
		private final String candidateType;

		private TypeMismatchFault(Path source, String sourceType, Path candidate, String candidateType) {
			super(ValidationErrorType.TYPE_MISMATCH, source, candidate);
			this.sourceType = sourceType;
			this.candidateType = candidateType;
		}

		@Override
		protected void writeColumns(CSVPrinter p) throws IOException {
			super.writeColumns(p);
			p.print(this.sourceType);
			p.print(this.candidateType);
		}

		@Override
		protected void writeHeaders(CSVPrinter p) throws IOException {
			super.writeHeaders(p);
			p.print("SOURCE TYPE");
			p.print("CANDIDATE TYPE");
		}
	}

	public static class AspectMissingFault extends ValidationFault {
		private final String sourceAspect;
		private final Set<String> candidateAspects;

		private AspectMissingFault(Path source, Path candidate, String sourceAspect, Set<String> candidateAspects) {
			super(ValidationErrorType.ASPECT_MISSING, source, candidate);
			this.sourceAspect = sourceAspect;
			this.candidateAspects = Tools.freezeCopy(candidateAspects, true);
		}

		@Override
		protected void writeColumns(CSVPrinter p) throws IOException {
			super.writeColumns(p);
			p.print(this.sourceAspect);
			p.print(StringUtils.join(this.candidateAspects, ','));
		}

		@Override
		protected void writeHeaders(CSVPrinter p) throws IOException {
			super.writeHeaders(p);
			p.print("MISSING ASPECT");
			p.print("CANDIDATE ASPECTS");
		}
	}

	public static class AttributeFault extends ValidationFault {
		private final SchemaAttribute attribute;
		private final Object sourceValue;
		private final Object candidateValue;

		private AttributeFault(Path source, Path candidate, SchemaAttribute attribute, Object sourceValue,
			Object candidateValue) {
			super(ValidationErrorType.ATTRIBUTE_VALUE, source, candidate);
			this.attribute = attribute;
			this.sourceValue = sourceValue;
			this.candidateValue = candidateValue;
		}

		@Override
		protected void writeColumns(CSVPrinter p) throws IOException {
			super.writeColumns(p);
			p.print(this.attribute.name);
			p.print(this.attribute.type.nameString);
			p.print(this.sourceValue);
			p.print(this.candidateValue);
		}

		@Override
		protected void writeHeaders(CSVPrinter p) throws IOException {
			super.writeHeaders(p);
			p.print("ATTRIBUTE NAME");
			p.print("ATTRIBUTE TYPE");
			p.print("SOURCE VALUE");
			p.print("CANDIDATE TYPE");
		}
	}

	public static class ContentSizeFault extends ValidationFault {
		private final long sourceSize;
		private final long candidateSize;

		private ContentSizeFault(Path source, Path candidate, long sourceSize, long candidateSize) {
			super(ValidationErrorType.CONTENT_SIZE, source, candidate);
			this.sourceSize = sourceSize;
			this.candidateSize = candidateSize;
		}

		@Override
		protected void writeColumns(CSVPrinter p) throws IOException {
			super.writeColumns(p);
			p.print(this.sourceSize);
			p.print(this.candidateSize);
		}

		@Override
		protected void writeHeaders(CSVPrinter p) throws IOException {
			super.writeHeaders(p);
			p.print("SOURCE SIZE");
			p.print("CANDIDATE SIZE");
		}
	}

	public static class ContentSumFault extends ValidationFault {
		private final String sourceSum;
		private final String candidateSum;

		private ContentSumFault(Path source, Path candidate, String sourceSum, String candidateSum) {
			super(ValidationErrorType.CONTENT_SUM, source, candidate);
			this.sourceSum = sourceSum;
			this.candidateSum = candidateSum;
		}

		@Override
		protected void writeColumns(CSVPrinter p) throws IOException {
			super.writeColumns(p);
			p.print(this.sourceSum);
			p.print(this.candidateSum);
		}

		@Override
		protected void writeHeaders(CSVPrinter p) throws IOException {
			super.writeHeaders(p);
			p.print("SOURCE CHECKSUM");
			p.print("CANDIDATE CHECKSUM");
		}
	}

	public abstract class FileVisitor extends SimpleFileVisitor<Path> {

		@Override
		public final FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			// If the file is a metadata properties file, we submit it for loading.
			Matcher m = Validator.VALID_FILENAME.matcher(file.getFileName().toString());
			if (m.matches()) {
				try {
					processFile(file);
				} catch (InterruptedException e) {
					// Log the error...
					Validator.LOG.error(
						String.format("Failed to submit the file [%s] for processing - workers no longer working",
							file.toAbsolutePath().toString()),
						e);
					return FileVisitResult.TERMINATE;
				} catch (Exception e) {
					Validator.LOG.error(
						String.format("Failed to submit the file [%s] for processing - unexpected exception caught",
							file.toAbsolutePath().toString()),
						e);
				}
			}
			return FileVisitResult.CONTINUE;
		}

		protected abstract void processFile(Path file) throws Exception;

		@Override
		public final FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
			Validator.this.log.warn(String.format("Failed to visit the file at [%s]", file.toAbsolutePath().toString()),
				exception);
			// We continue b/c we need to keep trying...
			return FileVisitResult.CONTINUE;
		}

		@Override
		public final FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			return super.preVisitDirectory(dir, attrs);
		}

		@Override
		public final FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return super.postVisitDirectory(dir, exc);
		}
	};

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Map<ValidationErrorType, List<ValidationFault>> errors;
	private final AtomicLong faultCount = new AtomicLong(0);
	private final Path sourceRoot;
	private final Path candidateRoot;

	private final AlfrescoSchema schema;

	public Validator(final Path sourceRoot, final Path candidateRoot, Collection<String> contentModel)
		throws Exception {
		this.sourceRoot = sourceRoot;
		this.candidateRoot = candidateRoot;

		if ((contentModel == null)
			|| contentModel.isEmpty()) { throw new Exception("Must provide a valid, non-emtpy content model"); }

		Collection<URI> modelUris = new ArrayList<URI>(contentModel.size());
		for (String m : contentModel) {
			modelUris.add(new File(m).toURI());
		}
		this.schema = new AlfrescoSchema(modelUris);

		Map<ValidationErrorType, List<ValidationFault>> errors = new EnumMap<ValidationErrorType, List<ValidationFault>>(
			ValidationErrorType.class);
		for (ValidationErrorType t : ValidationErrorType.values()) {
			errors.put(t, Collections.synchronizedList(new LinkedList<ValidationFault>()));
		}
		this.errors = Tools.freezeMap(errors);
	}

	public final long getFaultCount() {
		return this.faultCount.get();
	}

	public final Path getSourceRoot() {
		return this.sourceRoot;
	}

	public final Path getCandidateRoot() {
		return this.candidateRoot;
	}

	private void addFault(ValidationFault fault) {
		if (fault == null) { return; }
		this.errors.get(fault.type).add(fault);
		this.faultCount.incrementAndGet();
	}

	private String validateFile(Path p) {
		return validateFile(p.toFile());
	}

	private String validateFile(File f) {
		if (!f.exists()) { return "The %s file doesn't exist"; }
		if (!f.isFile()) { return "The %s file is not a regular file"; }
		if (!f.canRead()) { return "The %s file is not readable"; }
		return null;
	}

	private Properties loadProperties(Path path) throws IOException {
		final File file = path.toFile();
		InputStream in = new FileInputStream(file);
		final Properties properties = new Properties();
		try {
			properties.loadFromXML(in);
		} catch (InvalidPropertiesFormatException x) {
			IOUtils.closeQuietly(in);
			properties.clear();
			in = new FileInputStream(file);
			properties.load(in);
		} finally {
			IOUtils.closeQuietly(in);
		}
		return properties;
	}

	private boolean checkFiles(final Path sourcePath, final Path candidatePath) {
		// Test 0: We assume that this will pass, but check it anyway
		String sourceMsg = validateFile(sourcePath);
		if (sourceMsg != null) {
			sourceMsg = String.format(sourceMsg, "source");
		}

		// Test 1: If the candidate doesn't exist, then we have a problem
		String candidateMsg = validateFile(candidatePath);
		if (candidateMsg != null) {
			candidateMsg = String.format(candidateMsg, "source");
		}

		// No faults? check out
		if ((sourceMsg == null) && (candidateMsg == null)) { return true; }

		// Report the faults detected
		if (sourceMsg != null) {
			this.log.error("Failed to access the file at [{}]: {}", sourcePath.toString(), sourceMsg);
		}
		if (candidateMsg != null) {
			this.log.error("Failed to access the file at [{}]: {}", candidatePath.toString(), candidateMsg);
		}

		// Can't do anything else...so... we barf out
		addFault(new ObjectMissingFault(sourcePath, sourceMsg, candidatePath, candidateMsg));
		return false;
	}

	// TODO: Change this into an interface that can be used "pluggably" (i.e. we can employ
	// custom-written validation checks)
	private boolean checkTypes(Path sourcePath, Properties sourceData, Path candidatePath, Properties candidateData) {
		String sourceType = sourceData.getProperty(Validator.PROP_TYPE);
		String candidateType = candidateData.getProperty(Validator.PROP_TYPE);
		if (Tools.equals(sourceType, candidateType)) { return true; }
		addFault(new TypeMismatchFault(sourcePath, sourceType, candidatePath, candidateType));
		return false;
	}

	private Set<String> loadAspects(Properties properties) {
		String aspectsStr = properties.getProperty(Validator.PROP_ASPECTS, "");
		Set<String> aspects = new TreeSet<String>();
		for (String aspect : aspectsStr.split(",")) {
			if (StringUtils.isBlank(aspect)) {
				continue;
			}
			aspects.add(aspect);
		}
		return aspects;
	}

	private boolean checkAspects(Path sourcePath, Properties sourceData, Path candidatePath, Properties candidateData) {
		Set<String> sourceAspects = loadAspects(sourceData);
		Set<String> candidateAspects = loadAspects(candidateData);

		// Remove all aspects in the source that exist in the candidates
		sourceAspects.removeAll(candidateAspects);

		// If there are no source aspects remaining, it means they're all covered in the candidate
		// and thus the aspects match...
		if (sourceAspects.isEmpty()) { return true; }

		// If there are remaining source aspects, it means they weren't included in the
		// candidate, and thus should be reported as a fault
		for (String aspect : sourceAspects) {
			addFault(new AspectMissingFault(sourcePath, candidatePath, aspect, candidateAspects));
		}
		return false;
	}

	private Date parseDate(String dateStr) throws ParseException {
		return DateUtils.parseDate(dateStr, Validator.DATE_FORMAT);

	}

	private boolean checkAttributes(Path sourcePath, Properties sourceData, Path candidatePath,
		Properties candidateData) {

		AlfrescoType alfrescoType = this.schema.buildType(sourceData.getProperty(Validator.PROP_TYPE),
			loadAspects(sourceData));

		boolean faultReported = false;
		for (final String attributeName : alfrescoType.getAttributeNames()) {
			final SchemaAttribute attribute = alfrescoType.getAttribute(attributeName);

			// This is an attribute that should match between the two...
			final String sourceValueStr = sourceData.getProperty(attributeName);
			final String candidateValueStr = candidateData.getProperty(attributeName);

			// If the attribute isn't present in the source, we don't check against the candidate,
			// nor do we report a fault
			if (sourceValueStr == null) {
				continue;
			}

			// If the value is present in the source, but is absent in the candidate, we report
			// a fault because we have a value mismatch
			if (candidateValueStr == null) {
				addFault(new AttributeFault(sourcePath, candidatePath, attribute, sourceValueStr, candidateValueStr));
				faultReported = true;
				continue;
			}

			// So we have both values... for now, assume they're strings
			Object sourceValue = sourceValueStr;
			Object candidateValue = candidateValueStr;

			// Check to see if the type requires special handling
			switch (attribute.type) {
				case DATETIME:
				case DATE:
					// Dates require parsing using ISO8601 datetime
					try {
						sourceValue = parseDate(sourceValueStr);
					} catch (ParseException e) {
						sourceValue = null;
						addFault(new ExceptionFault(sourcePath, candidatePath,
							String.format("Parsing source field [%s] as %s with value [%s]", attributeName,
								attribute.type.name(), sourceValueStr),
							e, true));
						faultReported = true;
					}
					try {
						candidateValue = parseDate(candidateValueStr);
					} catch (ParseException e) {
						candidateValue = null;
						addFault(new ExceptionFault(sourcePath, candidatePath,
							String.format("Parsing candidate field [%s] as %s with value [%s]", attributeName,
								attribute.type.name(), candidateValueStr),
							e, true));
						faultReported = true;
					}
					if ((sourceValue == null) || (candidateValue == null)) {
						// The faults have been reported, move on to the next attribute
						continue;
					}
					break;

				default:
					break;
			}

			// Do the actual comparison... the values support equality testing by now, or remain
			// strings (which also supports equality)
			if (!Tools.equals(sourceValue, candidateValue)) {
				addFault(new AttributeFault(sourcePath, candidatePath, attribute, sourceValueStr, candidateValueStr));
				faultReported = true;
			}
		}
		return !faultReported;
	}

	private byte[] getChecksum(MessageDigest digest, File source) throws IOException {
		final byte[] buffer = new byte[Validator.BUFFER_SIZE];
		InputStream in = new FileInputStream(source);
		try {
			for (;;) {
				int read = in.read(buffer);
				if (read < 0) {
					break;
				}
				digest.update(buffer, 0, read);
			}
		} finally {
			IOUtils.closeQuietly(in);
		}

		return digest.digest();
	}

	private boolean checkContents(Path sourcePath, Properties sourceData, Path candidatePath,
		Properties candidateData) {

		AlfrescoType alfrescoType = this.schema.buildType(sourceData.getProperty(Validator.PROP_TYPE),
			loadAspects(sourceData));

		final String candidateCheckSumStr = candidateData.getProperty(Validator.PROP_CHECKSUM);
		final boolean sourceContentRequired = alfrescoType.isDescendedOf(Validator.CONTENT_TYPE);

		if (candidateCheckSumStr == null) {
			// If this type doesn't require a content stream, then there's no checksum required,
			// and therefore it's OK to short-circuit the validation
			if (!sourceContentRequired) { return true; }

			// If there is a content stream required, then we issue a checksum violation
			addFault(new ContentSumFault(sourcePath, candidatePath, "(not checked)", "(not provided)"));
			return false;
		}

		if (!sourceContentRequired) {
			// The source file shouldn't have any content...so this is a mismatch
			addFault(new ContentSumFault(sourcePath, candidatePath, "(not supported)", candidateCheckSumStr));
			return false;
		}

		// Ok we have a checksum, let's parse it out
		Matcher m = Validator.CHECKSUM_PARSER.matcher(candidateCheckSumStr);
		if (!m.matches()) {
			// If we have a badly formatted content stream, we must report it as a validation
			// failure, and we can't continue b/c we won't know what to check
			addFault(new ContentSumFault(sourcePath, candidatePath, "(not checked)",
				String.format("BAD FORMAT: %s", candidateCheckSumStr)));
			return false;
		}

		// Our candidate checksum is properly formatted, parse out its contents
		final String checksumType = m.group(1);
		final MessageDigest checksumDigest;
		try {
			checksumDigest = MessageDigest.getInstance(checksumType.toUpperCase());
		} catch (NoSuchAlgorithmException e) {
			// Illegal checksum scheme...this is a validation error
			addFault(new ContentSumFault(sourcePath, candidatePath, "(not checked)",
				String.format("BAD HASH TYPE (%s): %s", checksumType, candidateCheckSumStr)));
			return false;
		}

		// Ok so we know the candidate says we should have a content stream...let's try to find it
		// First, identify the content file for this one
		final File sourceContentFile = new File(
			sourcePath.toString().replaceAll(Validator.METADATA_MARKER_PATTERN, ""));
		final Path sourceContentPath = sourceContentFile.toPath();

		if (!sourceContentFile.exists()) {
			addFault(new ContentSumFault(sourceContentPath, candidatePath,
				String.format("SOURCE FILE [%s] MISSING", sourceContentPath.toString()), candidateCheckSumStr));
			return false;
		}

		if (!sourceContentFile.isFile()) {
			addFault(new ContentSumFault(sourceContentPath, candidatePath,
				String.format("SOURCE FILE [%s] IS NOT A REGULAR FILE", sourceContentPath.toString()),
				candidateCheckSumStr));
			return false;
		}

		if (!sourceContentFile.canRead()) {
			addFault(new ContentSumFault(sourceContentPath, candidatePath,
				String.format("SOURCE FILE [%s] IS NOT READABLE", sourceContentPath.toString()), candidateCheckSumStr));
			return false;
		}

		final long sourceSize = sourceContentFile.length();
		final long candidateSize = sourceContentFile.length(); // TODO: pull this from....???

		if (sourceSize != candidateSize) {
			addFault(new ContentSizeFault(sourceContentPath, candidatePath, sourceSize, candidateSize));
			return false;
		}

		final String candidateChecksumValue = m.group(2).toLowerCase();
		final byte[] sourceChecksumBytes;
		try {
			sourceChecksumBytes = getChecksum(checksumDigest, sourceContentFile);
		} catch (IOException e) {
			addFault(new ExceptionFault(sourceContentPath, candidatePath,
				String.format("Failed to calculate the %s checksum for the source file [%s]", checksumType,
					sourceContentPath.toString()),
				e, null));
			return false;
		}

		final String sourceChecksumValue = BinaryEncoding.HEX.encode(sourceChecksumBytes).toLowerCase();
		if (!Tools.equals(sourceChecksumValue, candidateChecksumValue)) {
			addFault(new ContentSumFault(sourceContentPath, candidatePath, sourceChecksumValue, candidateCheckSumStr));
			return false;
		}

		return true;
	}

	public void validate(final Path sourcePath) {
		// Validate the source file against the target...
		final Path relativePath = this.sourceRoot.relativize(sourcePath);
		final Path candidatePath = this.candidateRoot.resolve(relativePath);

		this.log.info("Examining the object at [{}]...", relativePath.toString());

		boolean validated = false;
		try {
			if (!checkFiles(sourcePath, candidatePath)) { return; }

			// Test 2: Load both properties files, and begin property comparisons
			Properties sourceProp = null;
			try {
				sourceProp = loadProperties(sourcePath);
			} catch (IOException e) {
				addFault(new ExceptionFault(sourcePath, candidatePath, "Loading source properties", e, true));
				sourceProp = null;
			}
			Properties candidateProp = null;
			try {
				candidateProp = loadProperties(candidatePath);
			} catch (IOException e) {
				addFault(new ExceptionFault(sourcePath, candidatePath, "Loading candidate properties", e, false));
				candidateProp = null;
			}

			// If we were unable to load the properties for either of them, we can no longer proceed
			if ((sourceProp == null) || (candidateProp == null)) { return; }

			// Test 3: they must be of the same object type
			if (!checkTypes(sourcePath, sourceProp, candidatePath, candidateProp)) { return; }

			// Test 4: the aspects specified by candidate must be a superset of the aspects
			// specified by the source
			if (!checkAspects(sourcePath, sourceProp, candidatePath, candidateProp)) { return; }

			// Test 5: every property specified in source must match its corresponding
			// property on target (apply special typing rules for date, int, double, etc.)
			// We check the attributes and the content at this point...
			final boolean attsOk = checkAttributes(sourcePath, sourceProp, candidatePath, candidateProp);

			// Test 6: perform the size + checksum check (folders will simply pass this test
			// quietly)
			final boolean contentOk = checkContents(sourcePath, sourceProp, candidatePath, candidateProp);

			if (!attsOk || !contentOk) { return; }

			// We only reach here if all tests were successful.
			validated = true;
		} catch (Throwable t) {
			addFault(new ExceptionFault(sourcePath, candidatePath, "Unexpected exception caught", t, null));
		} finally {
			this.log.info("Validation for the object at [{}] {}", relativePath.toString(),
				validated ? "passed" : "FAILED");
		}
	}
}