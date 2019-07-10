/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.validator;

import java.io.Closeable;
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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoDataType;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoSchema;
import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoType;
import com.armedia.caliente.engine.alfresco.bi.importer.model.SchemaAttribute;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.codec.BinaryEncoding;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;

public class Validator extends BaseShareableLockable {

	private static final Logger LOG = LoggerFactory.getLogger(Validator.class);

	private static final Pattern VALID_FILENAME = Pattern
		.compile("^.*\\.metadata\\.properties\\.xml(?:\\.v\\d+(?:\\.\\d+)?)?$", Pattern.CASE_INSENSITIVE);

	private static final int BUFFER_SIZE = (int) FileUtils.ONE_MB;

	private static final String METADATA_MARKER = ".metadata.properties.xml";
	private static final String METADATA_MARKER_PATTERN = String.format("\\Q%s\\E", Validator.METADATA_MARKER);
	private static final Pattern CHECKSUM_PARSER = Pattern.compile("^([^:]+):(\\d+):((?:[a-f0-9]{2})+)$",
		Pattern.CASE_INSENSITIVE);

	private static final String CONTENT_TYPE = "cm:content";

	private static final String PROP_TYPE = "type";
	private static final String PROP_ASPECTS = "aspects";
	private static final String PROP_CHECKSUM = "streamChecksum";

	private static final Map<String, String> REFERENCE_TYPE_MAPPING;
	static {
		Map<String, String> m = new TreeMap<>();
		m.put("app:folderlink", "arm:reference");
		m.put("app:filelink", "arm:reference");
		REFERENCE_TYPE_MAPPING = Tools.freezeMap(new LinkedHashMap<>(m));
	}

	private static final Map<String, Set<String>> ALLOWED_ENFORCED_MISSES;
	static {
		Map<String, Set<String>> m = new TreeMap<>();
		Set<String> s = new TreeSet<>();
		s.add("cm:created");
		s.add("cm:creator");
		s.add("cm:modified");
		s.add("cm:modifier");
		s = Tools.freezeSet(new LinkedHashSet<>(s));
		m.put("arm:reference", s);
		m.put("arm:rendition", s);
		ALLOWED_ENFORCED_MISSES = Tools.freezeMap(new LinkedHashMap<>(m));
	}

	private static enum ValidationErrorType {
		//
		EXCEPTION, // exception raised while examining
		FILE_MISSING, // source exists, but not target
		TYPE_MISMATCH, // type or aspects fail to meet requirements
		ASPECT_MISSING, // type or aspects fail to meet requirements
		ATTRIBUTE_VALUE, // attribute values are different
		MANDATORY_ATTRIBUTE_MISSING, // mandatory attribute not set
		CONTENT_MISMATCH, // content size or checksum mismatch
		//
		;
	}

	private static enum ValueComparator {
		//
		OBJECT(), // Default mode
		STRING(AlfrescoDataType.MLTEXT, AlfrescoDataType.TEXT, AlfrescoDataType.QNAME) { // Compare
																							// strings
			@Override
			public String compareValues(String source, String candidate) {
				if (source == candidate) { return null; }
				if (source == null) { return "SOURCE VALUE IS NULL"; }
				if (candidate == null) { return "CANDIDATE VALUE IS NULL"; }
				if (source.length() != candidate.length()) {
					return String.format("LENGTHS ARE DIFFERENT (%d vs %d)", source.length(), candidate.length());
				}
				if (source.equals(candidate)) { return null; }
				if (source.equalsIgnoreCase(candidate)) { return "VALUES DIFFER IN CASE"; }
				// TODO: Calculate and highlight the differences?
				return "VALUES ARE DIFFERENT";
			}
		},
		DATE(AlfrescoDataType.DATE, AlfrescoDataType.DATETIME) { // Compare date values
			// ISO8601 with timezone...
			final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

			@Override
			public String compareValues(String source, String candidate) {
				// Dates require parsing using ISO8601 datetime
				String remarks = null;
				Date sourceValue = null;
				try {
					sourceValue = DateUtils.parseDate(source, this.dateFormat);
				} catch (ParseException e) {
					if (Validator.LOG.isDebugEnabled()) {
						Validator.LOG.error("Failed to parse the source date value [{}] with format [{}]", source,
							this.dateFormat, e);
					}
					sourceValue = null;
					remarks = String.format("Failed to parse the source date value with the format [%s]: %s",
						this.dateFormat, e.getMessage());
				}
				Date candidateValue = null;
				try {
					candidateValue = DateUtils.parseDate(candidate, this.dateFormat);
				} catch (ParseException e) {
					if (Validator.LOG.isDebugEnabled()) {
						Validator.LOG.error("Failed to parse the candidate date value [{}] with format [{}]", source,
							this.dateFormat, e);
					}
					candidateValue = null;
					String split = "F";
					if (remarks == null) {
						remarks = "";
					} else {
						split = ", f";
					}
					remarks = String.format("%s%sailed to parse the candidate date value with the format [%s]: %s",
						remarks, split, this.dateFormat, e.getMessage());
				}
				if (remarks != null) { return remarks; }

				if (!Tools.equals(sourceValue, candidateValue)) {
					long diff = sourceValue.getTime() - candidateValue.getTime();
					String sign = "";
					if (diff < 0) {
						sign = "-";
						diff *= -1;
					}
					long h = TimeUnit.MILLISECONDS.toHours(diff);
					diff -= TimeUnit.HOURS.toMillis(h);
					long m = TimeUnit.MILLISECONDS.toMinutes(diff);
					diff -= TimeUnit.MINUTES.toMillis(m);
					long s = TimeUnit.MILLISECONDS.toSeconds(diff);
					diff -= TimeUnit.SECONDS.toMillis(s);
					long l = TimeUnit.MILLISECONDS.toMillis(diff);
					remarks = String.format("SRC - DST == %s%d:%02d:%02d.%03d", sign, h, m, s, l);
				}
				return remarks;
			}
		},
		//
		;

		private final Set<AlfrescoDataType> supported;

		ValueComparator(AlfrescoDataType... supported) {
			Set<AlfrescoDataType> s = EnumSet.noneOf(AlfrescoDataType.class);
			for (AlfrescoDataType t : supported) {
				if (t == null) {
					continue;
				}
				s.add(t);
			}
			this.supported = Tools.freezeSet(s);
		}

		public String compareValues(String source, String candidate) {
			if (Tools.equals(source, candidate)) { return null; }
			return "VALUES ARE DIFFERENT";
		}

		private static Map<AlfrescoDataType, ValueComparator> MAP = null;
		static {
			Map<AlfrescoDataType, ValueComparator> m = new EnumMap<>(AlfrescoDataType.class);
			for (ValueComparator c : ValueComparator.values()) {
				for (AlfrescoDataType t : c.supported) {
					ValueComparator C = m.put(t, c);
					if (C != null) {
						throw new IllegalStateException(String.format(
							"Comparators %s and %s both support type %s - this is not permitted, only one should support it",
							c.name(), C.name(), t.name()));
					}
				}
			}
			ValueComparator.MAP = Tools.freezeMap(m);
		}

		public static ValueComparator getComparator(AlfrescoDataType type) {
			return Tools.coalesce(ValueComparator.MAP.get(type), ValueComparator.OBJECT);
		}
	}

	private static abstract class ValidationFault implements Comparable<ValidationFault> {
		protected final ValidationErrorType type;
		protected final Path path;

		private ValidationFault(ValidationErrorType type, Path path) {
			this.type = type;
			this.path = path;
		}

		private CSVPrinter createOutputFile(File baseDir, String marker) throws IOException {
			// Open the file
			File targetFile = new File(baseDir,
				String.format("caliente-validator-faults.%s.%s.csv", marker, this.type.name().toLowerCase()));

			// Clear out garbage
			if (targetFile.exists() && targetFile.isFile()) {
				FileUtils.forceDelete(targetFile);
			}

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
			p.print(this.path.toString());
		}

		protected final void writeRecord(CSVPrinter p) throws IOException {
			writeColumns(p);
			p.println();
			p.flush();
		}

		protected void writeHeaders(CSVPrinter p) throws IOException {
			p.print("FAULT TYPE");
			p.print("RELATIVE PATH");
		}

		@Override
		public int compareTo(ValidationFault o) {
			if (o == null) { return 1; }
			int r = Tools.compare(this.type, o.type);
			if (r != 0) { return r; }
			r = Tools.compare(this.path, o.path);
			if (r != 0) { return r; }
			return 0;
		}
	}

	public static class ExceptionFault extends ValidationFault {
		private final String message;
		private final Boolean examiningSource;
		private final Throwable exception;

		private ExceptionFault(Path path, String message, Throwable exception, Boolean examiningSource) {
			super(ValidationErrorType.EXCEPTION, path);
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

	public static class FileMissingFault extends ValidationFault {
		private final String sourceMessage;
		private final String candidateMessage;

		private FileMissingFault(Path path, String sourceMessage, String candidateMessage) {
			super(ValidationErrorType.FILE_MISSING, path);
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

		private TypeMismatchFault(Path path, String sourceType, String candidateType) {
			super(ValidationErrorType.TYPE_MISMATCH, path);
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

		private AspectMissingFault(Path path, String sourceAspect, Set<String> candidateAspects) {
			super(ValidationErrorType.ASPECT_MISSING, path);
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
		private final String remarks;

		private AttributeFault(ValidationErrorType type, Path path, SchemaAttribute attribute, Object sourceValue,
			Object candidateValue, String remarks) {
			super(type, path);
			this.attribute = attribute;
			this.sourceValue = sourceValue;
			this.candidateValue = candidateValue;
			this.remarks = remarks;
		}

		private AttributeFault(Path path, SchemaAttribute attribute, Object sourceValue, Object candidateValue,
			String remarks) {
			this(ValidationErrorType.ATTRIBUTE_VALUE, path, attribute, sourceValue, candidateValue, remarks);
		}

		@Override
		protected void writeColumns(CSVPrinter p) throws IOException {
			super.writeColumns(p);
			p.print(this.attribute.name);
			p.print(this.attribute.type.nameString);
			p.print(this.sourceValue);
			p.print(this.candidateValue);
			p.print(this.remarks);
		}

		@Override
		protected void writeHeaders(CSVPrinter p) throws IOException {
			super.writeHeaders(p);
			p.print("ATTRIBUTE NAME");
			p.print("ATTRIBUTE TYPE");
			p.print("SOURCE VALUE");
			p.print("CANDIDATE VALUE");
			p.print("REMARKS");
		}
	}

	public static class MandatoryAttributeMissingFault extends AttributeFault {
		private MandatoryAttributeMissingFault(Path path, SchemaAttribute attribute, Object sourceValue,
			Object candidateValue, String remarks) {
			super(ValidationErrorType.MANDATORY_ATTRIBUTE_MISSING, path, attribute, sourceValue, candidateValue,
				remarks);
		}
	}

	public static class ContentMismatchFault extends ValidationFault {
		private final long sourceSize;
		private final String sourceSum;
		private final long candidateSize;
		private final String candidateSum;

		private ContentMismatchFault(Path path, long sourceSize, String sourceSum, long candidateSize,
			String candidateSum) {
			super(ValidationErrorType.CONTENT_MISMATCH, path);
			this.sourceSize = sourceSize;
			this.sourceSum = sourceSum;
			this.candidateSize = candidateSize;
			this.candidateSum = candidateSum;
		}

		@Override
		protected void writeColumns(CSVPrinter p) throws IOException {
			super.writeColumns(p);
			p.print(this.sourceSize);
			p.print(this.sourceSum);
			p.print(this.candidateSize);
			p.print(this.candidateSum);
		}

		@Override
		protected void writeHeaders(CSVPrinter p) throws IOException {
			super.writeHeaders(p);
			p.print("SOURCE SIZE");
			p.print("SOURCE CHECKSUM");
			p.print("CANDIDATE SIZE");
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
					Validator.LOG.error("Failed to submit the file [{}] for processing - workers no longer working",
						file.toRealPath().toString(), e);
					return FileVisitResult.TERMINATE;
				} catch (Exception e) {
					Validator.LOG.error("Failed to submit the file [{}] for processing - unexpected exception caught",
						file.toRealPath().toString(), e);
				}
			}
			return FileVisitResult.CONTINUE;
		}

		protected abstract void processFile(Path file) throws Exception;

		@Override
		public final FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
			Validator.this.log.warn("Failed to visit the file at [{}]", file.toRealPath(), exception);
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

	private final File reportDir;
	private final String reportMarker;
	private final Map<ValidationErrorType, AtomicLong> faultCounters;
	private final Map<ValidationErrorType, CSVPrinter> errors;
	private final AtomicLong failureCount = new AtomicLong(0);
	private final AtomicLong successCount = new AtomicLong(0);
	private final AtomicLong faultCount = new AtomicLong(0);
	private final Path sourceRoot;
	private final Path candidateRoot;

	private final AlfrescoSchema schema;

	private final AtomicBoolean closed = new AtomicBoolean(false);

	public Validator(final Path reportDir, final Path sourceRoot, final Path candidateRoot,
		Collection<String> contentModel, String reportMarker) throws Exception {
		this.reportDir = reportDir.toFile();
		this.reportMarker = reportMarker;
		this.sourceRoot = sourceRoot;
		this.candidateRoot = candidateRoot;

		if ((contentModel == null) || contentModel.isEmpty()) {
			throw new Exception("Must provide a valid, non-emtpy content model");
		}

		Collection<URI> modelUris = new ArrayList<>(contentModel.size());
		for (String m : contentModel) {
			modelUris.add(new File(m).toURI());
		}
		this.schema = new AlfrescoSchema(modelUris);

		this.errors = new EnumMap<>(ValidationErrorType.class);
		this.faultCounters = new EnumMap<>(ValidationErrorType.class);
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

	public final String getReportMarker() {
		return this.reportMarker;
	}

	private void reportFault(ValidationFault fault) {
		if (fault == null) { return; }

		CSVPrinter p = this.errors.get(fault.type);
		AtomicLong c = this.faultCounters.get(fault.type);
		if (p == null) {
			synchronized (fault.type) {
				p = this.errors.get(fault.type);
				if (p == null) {
					try {
						p = fault.createOutputFile(this.reportDir, this.reportMarker);
					} catch (IOException e) {
						String msg = String.format("Failed to create the fault report for %s", fault.type.name());
						Validator.LOG.error(msg, e);
						throw new RuntimeException(msg, e);
					}
					this.errors.put(fault.type, p);
				}
				c = this.faultCounters.get(fault.type);
				if (c == null) {
					c = new AtomicLong(0);
					this.faultCounters.put(fault.type, c);
				}
			}
		}

		try {
			synchronized (p) {
				fault.writeRecord(p);
			}
		} catch (IOException e) {
			String msg = String.format("Failed to write out a fault record for %s", fault.type.name());
			Validator.LOG.error(msg, e);
			throw new RuntimeException(msg, e);
		}
		c.incrementAndGet();
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
		try (InputStream in = new FileInputStream(file)) {
			return XmlProperties.loadFromXML(in);
		} catch (XMLStreamException e) {
			this.log.warn("Failed to load the properties at [{}] as XML, falling back to default properties", path, e);
		}
		try (InputStream in = new FileInputStream(file)) {
			final Properties properties = new Properties();
			properties.load(in);
			return properties;
		}
	}

	private boolean checkFiles(final Path relativePath) {
		final Path sourcePath = this.sourceRoot.resolve(relativePath);
		final Path candidatePath = this.candidateRoot.resolve(relativePath);

		String sourceMsg = validateFile(sourcePath);
		if (sourceMsg != null) {
			sourceMsg = String.format(sourceMsg, "source");
		}

		// Test 1: If the candidate doesn't exist, then we have a problem
		String candidateMsg = validateFile(candidatePath);
		if (candidateMsg != null) {
			candidateMsg = String.format(candidateMsg, "candidate");
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
		reportFault(new FileMissingFault(relativePath, sourceMsg, candidateMsg));
		return false;
	}

	// TODO: Change this into an interface that can be used "pluggably" (i.e. we can employ
	// custom-written validation checks)
	private boolean checkTypes(final Path relativePath, Properties sourceData, Properties candidateData) {
		String sourceType = sourceData.getProperty(Validator.PROP_TYPE);
		String candidateType = candidateData.getProperty(Validator.PROP_TYPE);
		if (Tools.equals(sourceType, candidateType)
			|| Tools.equals(Validator.REFERENCE_TYPE_MAPPING.get(candidateType), sourceType)) {
			return true;
		}
		reportFault(new TypeMismatchFault(relativePath, sourceType, candidateType));
		return false;
	}

	private Set<String> loadAspects(Properties properties) {
		String aspectsStr = properties.getProperty(Validator.PROP_ASPECTS, "");
		Set<String> aspects = new TreeSet<>();
		for (String aspect : aspectsStr.split(",")) {
			if (StringUtils.isBlank(aspect)) {
				continue;
			}
			aspects.add(aspect);
		}
		return aspects;
	}

	private boolean checkAspects(final Path relativePath, Properties sourceData, Properties candidateData) {
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
			reportFault(new AspectMissingFault(relativePath, aspect, candidateAspects));
		}
		return false;
	}

	private boolean checkAttributes(final Path relativePath, Properties sourceData, Properties candidateData) {
		AlfrescoType alfrescoType = this.schema.buildType(sourceData.getProperty(Validator.PROP_TYPE),
			loadAspects(sourceData));

		boolean faultReported = false;
		for (final String attributeName : alfrescoType.getAttributeNames()) {
			final SchemaAttribute attribute = alfrescoType.getAttribute(attributeName);

			// This is an attribute that should match between the two...
			final String sourceValueStr = sourceData.getProperty(attributeName);
			final String candidateValueStr = candidateData.getProperty(attributeName);

			// If the attribute isn't present in the source, we don't check against the candidate,
			// nor do we report a fault...unless it's a mandatory attribute
			if (sourceValueStr == null) {
				if (!attribute.name.startsWith("sys:")) {
					// This isn't a system attribute, so we log it
					switch (attribute.mandatory) {
						case ENFORCED:
							Set<String> misses = Validator.ALLOWED_ENFORCED_MISSES.get(alfrescoType.getName());
							if (misses == null) {
								misses = Collections.emptySet();
							}
							if (!misses.contains(attribute.name)) {
								reportFault(new MandatoryAttributeMissingFault(relativePath, attribute, sourceValueStr,
									candidateValueStr,
									String.format("%s ATTRIBUTE MISSING", attribute.mandatory.name())));
								faultReported = true;
							}
							break;
						case RELAXED:
							break;
						case OPTIONAL:
							break;
					}
				}
				continue;
			}

			// If the value is present in the source, but is absent in the candidate, we report
			// a fault because we have a value mismatch
			if (candidateValueStr == null) {
				reportFault(new AttributeFault(relativePath, attribute, sourceValueStr, candidateValueStr,
					"NULL CANDIDATE VALUE"));
				faultReported = true;
				continue;
			}

			// Check to see if the type requires special handling
			final ValueComparator comparator = ValueComparator.getComparator(attribute.type);
			String remarks = comparator.compareValues(sourceValueStr, candidateValueStr);
			if (remarks != null) {
				reportFault(new AttributeFault(relativePath, attribute, sourceValueStr, candidateValueStr, remarks));
				faultReported = true;
			}
		}

		return !faultReported;
	}

	private byte[] getChecksum(MessageDigest digest, File source) throws IOException {
		final byte[] buffer = new byte[Validator.BUFFER_SIZE];
		try (InputStream in = new FileInputStream(source)) {
			for (;;) {
				int read = in.read(buffer);
				if (read < 0) {
					break;
				}
				digest.update(buffer, 0, read);
			}
		}

		return digest.digest();
	}

	private boolean checkContents(Path relativePath, Properties sourceData, Properties candidateData) {
		AlfrescoType alfrescoType = this.schema.buildType(sourceData.getProperty(Validator.PROP_TYPE),
			loadAspects(sourceData));

		final String candidateCheckSumStr = candidateData.getProperty(Validator.PROP_CHECKSUM);
		final boolean sourceContentRequired = alfrescoType.isDescendedOf(Validator.CONTENT_TYPE);

		if (candidateCheckSumStr == null) {
			// If this type doesn't require a content stream, then there's no checksum required,
			// and therefore it's OK to short-circuit the validation. Also, if the type is a
			// reference, we assume that there won't be any content to begin with...
			if (!sourceContentRequired
				|| Validator.REFERENCE_TYPE_MAPPING.containsKey(candidateData.getProperty(Validator.PROP_TYPE))) {
				return true;
			}

			// If there is a content stream required, then we issue a checksum violation
			reportFault(new ContentMismatchFault(relativePath, -1, "(expected, but not checked)", -1,
				"NO CANDIDATE DATA PROVIDED"));
			return false;
		}

		if (!sourceContentRequired) {
			// The source file shouldn't have any content...so this is a mismatch
			reportFault(new ContentMismatchFault(relativePath, -1, "SOURCE OBJECT PROVIDES NO CONTENT", -1,
				candidateCheckSumStr));
			return false;
		}

		// Ok we have a checksum, let's parse it out
		Matcher m = Validator.CHECKSUM_PARSER.matcher(candidateCheckSumStr);
		if (!m.matches()) {
			// If we have a badly formatted content stream, we must report it as a validation
			// failure, and we can't continue b/c we won't know what to check
			reportFault(new ContentMismatchFault(relativePath, -1, "(not checked)", -1,
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
			reportFault(new ContentMismatchFault(relativePath, -1, "(not checked)", -1,
				String.format("BAD HASH TYPE (%s): %s", checksumType, candidateCheckSumStr)));
			return false;
		}

		final long candidateSize;
		try {
			candidateSize = Long.valueOf(m.group(2));
		} catch (NumberFormatException e) {
			reportFault(new ContentMismatchFault(relativePath, -1, "(not checked)", -1,
				String.format("BAD SIZE COMPONENT: %s", candidateCheckSumStr)));
			return false;
		}

		final String candidateChecksumValue = m.group(3).toLowerCase();

		// Ok so we know the candidate says we should have a content stream...let's try to find it
		// First, identify the content file for this one
		final File sourceContentFile = new File(
			this.sourceRoot.resolve(relativePath).toString().replaceAll(Validator.METADATA_MARKER_PATTERN, ""));
		final Path sourceContentPath = sourceContentFile.toPath();

		if (!sourceContentFile.exists()) {
			reportFault(new ContentMismatchFault(relativePath, -1,
				String.format("SOURCE FILE [%s] MISSING", sourceContentPath.toString()), candidateSize,
				String.format("%s:%s", checksumType, candidateChecksumValue)));
			return false;
		}

		if (!sourceContentFile.isFile()) {
			reportFault(new ContentMismatchFault(relativePath, -1,
				String.format("SOURCE FILE [%s] IS NOT A REGULAR FILE", sourceContentPath.toString()), candidateSize,
				String.format("%s:%s", checksumType, candidateChecksumValue)));
			return false;
		}

		if (!sourceContentFile.canRead()) {
			reportFault(new ContentMismatchFault(relativePath, -1,
				String.format("SOURCE FILE [%s] IS NOT READABLE", sourceContentPath.toString()), candidateSize,
				String.format("%s:%s", checksumType, candidateChecksumValue)));
			return false;
		}

		final long sourceSize = sourceContentFile.length();

		if (sourceSize != candidateSize) {
			reportFault(new ContentMismatchFault(relativePath, sourceSize, "(not checked)", candidateSize,
				String.format("%s:%s", checksumType, candidateChecksumValue)));
			return false;
		}

		final String sourceChecksumValue;
		try {
			sourceChecksumValue = BinaryEncoding.HEX.encode(getChecksum(checksumDigest, sourceContentFile));
		} catch (IOException e) {
			reportFault(new ExceptionFault(relativePath,
				String.format("Failed to calculate the %s checksum for the source file [%s]", checksumType,
					sourceContentPath.toString()),
				e, null));
			return false;
		}

		if (!Tools.equals(sourceChecksumValue, candidateChecksumValue)) {
			reportFault(new ContentMismatchFault(relativePath, sourceSize,
				String.format("%s:%s", checksumType, sourceChecksumValue), candidateSize,
				String.format("%s:%s", checksumType, candidateChecksumValue)));
			return false;
		}

		return true;
	}

	public void validate(final Path sourcePath) {
		try (SharedAutoLock lock = autoSharedLock()) {
			if (this.closed.get()) { throw new IllegalStateException("This validator has been closed, but not reset"); }
			// Validate the source file against the target...
			final Path relativePath = this.sourceRoot.relativize(sourcePath);
			final Path candidatePath = this.candidateRoot.resolve(relativePath);

			this.log.info("Examining the object at [{}]...", relativePath.toString());

			boolean validated = false;
			try {
				if (!checkFiles(relativePath)) { return; }

				// Test 2: Load both properties files, and begin property comparisons
				Properties sourceProp = null;
				try {
					sourceProp = loadProperties(sourcePath);
				} catch (IOException e) {
					reportFault(new ExceptionFault(relativePath, "Loading source properties", e, true));
					sourceProp = null;
				}
				Properties candidateProp = null;
				try {
					candidateProp = loadProperties(candidatePath);
				} catch (IOException e) {
					reportFault(new ExceptionFault(relativePath, "Loading candidate properties", e, false));
					candidateProp = null;
				}

				// If we were unable to load the properties for either of them, we can no longer
				// proceed
				if ((sourceProp == null) || (candidateProp == null)) { return; }

				// Test 3: they must be of the same object type
				if (!checkTypes(relativePath, sourceProp, candidateProp)) { return; }

				// Test 4: the aspects specified by candidate must be a superset of the aspects
				// specified by the source
				if (!checkAspects(relativePath, sourceProp, candidateProp)) { return; }

				// Test 5: every property specified in source must match its corresponding
				// property on target (apply special typing rules for date, int, double, etc.)
				// We check the attributes and the content at this point...
				final boolean attsOk = checkAttributes(relativePath, sourceProp, candidateProp);

				// Test 6: perform the size + checksum check (folders will simply pass this test
				// quietly)
				final boolean contentOk = checkContents(relativePath, sourceProp, candidateProp);

				if (!attsOk || !contentOk) { return; }

				// We only reach here if all tests were successful.
				validated = true;
			} catch (Throwable t) {
				this.log.error("Unexpected Exception caught while processing [{}]", relativePath.toString(), t);
			} finally {
				(validated ? this.successCount : this.failureCount).incrementAndGet();
				this.log.info("Validation for [{}] {}", relativePath.toString(), validated ? "PASSED" : "FAILED");
			}
		}
	}

	private void resetState() {
		this.errors.clear();
		this.faultCount.set(0);
		this.faultCounters.clear();
	}

	public void writeAndClear() throws IOException {
		try (MutexAutoLock lock = autoMutexLock()) {
			try {
				for (ValidationErrorType t : this.errors.keySet()) {
					closeQuietly(this.errors.get(t));
					this.log.info("Detected {} {} faults", this.faultCounters.get(t).get(), t.name());
				}
				this.log.info("Detected {} individual faults total", this.faultCount.get());
				this.log.info("{} objects total PASSED", this.successCount.get());
				this.log.info("{} objects total FAILED", this.failureCount.get());
			} finally {
				resetState();
				this.closed.set(true);
			}
		}
	}

	private void closeQuietly(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException e) {
			}
		}
	}
}
