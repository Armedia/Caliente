package com.armedia.caliente.cli.datagen;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.datagen.data.DataRecord;
import com.armedia.caliente.cli.datagen.data.DataRecordManager;
import com.armedia.caliente.cli.datagen.data.DataRecordSet;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfValidator;
import com.documentum.fc.client.IDfValueAssistance;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.DfValue;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfList;
import com.documentum.fc.common.IDfProperties;
import com.documentum.fc.common.IDfValue;

public class NodeGenerator {

	private static final Random RANDOM = new Random(System.currentTimeMillis());

	private static final IDfValue NULL_ID = new DfValue(DfId.DF_NULLID_STR, IDfValue.DF_ID);

	private static final Pattern ID_PARSER = Pattern.compile("^[0-9a-f]{16}$", Pattern.CASE_INSENSITIVE);

	private static final String[] DATE_PATTERNS = {
		"yyyy-MM-dd'T'HH:mm:ssZZ", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-ddZZ", "yyyy-MM-dd"
	};

	private static final Pattern INTERNAL_TYPE = Pattern.compile("^dm[cir]?_.*$");
	private static final Pattern READ_ONLY_ATTRIBUTE = Pattern.compile("^[ria]_.*$");

	private static final Set<String> DM_SYSOBJECT_ATTR;
	static {
		Set<String> s = new TreeSet<>();
		String[] atts = {
			"authors", "keywords", "subject", "title"
		};
		for (String a : atts) {
			s.add(a);
		}

		DM_SYSOBJECT_ATTR = Tools.freezeSet(s);
	}

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AtomicLong totalCount = new AtomicLong(0);
	private final Map<String, Map<String, IDfAttr>> attributes;
	private final Set<String> folderTypes;
	private final Set<String> documentTypes;
	private final DataRecordManager<?> recordsManager;

	private static void buildAttributeData(IDfType type, Map<String, Map<String, IDfAttr>> attributes)
		throws DfException {
		Map<String, IDfAttr> a = new TreeMap<>();
		final String typeName = type.getName();
		while (type != null) {
			final String currentName = type.getName();
			final boolean internalType = NodeGenerator.INTERNAL_TYPE.matcher(currentName).matches();
			if (internalType && !currentName.equals("dm_sysobject")) {
				type = type.getSuperType();
				continue;
			}
			if (currentName.equals("dm_sysobject")) {
				// Special case...
				for (String attName : NodeGenerator.DM_SYSOBJECT_ATTR) {
					final int i = type.findTypeAttrIndex(attName);
					if (i < 0) {
						continue;
					}
					final IDfAttr attribute = type.getTypeAttr(i);

					if (internalType && NodeGenerator.READ_ONLY_ATTRIBUTE.matcher(attribute.getName()).matches()) {
						// If this is an internal type, and the attribute name is i_*, r_*, or
						// a_*, then we skip it
						continue;
					}
					a.put(attribute.getName(), attribute);
				}
			} else {
				// Not the root, so...
				if (attributes.containsKey(currentName)) {
					a.putAll(attributes.get(currentName));
				} else {
					final int attCount = type.getTypeAttrCount();
					final int startPos = type.getInt("start_pos");

					for (int i = startPos; i < attCount; i++) {
						final IDfAttr attribute = type.getTypeAttr(i);

						if (internalType && NodeGenerator.READ_ONLY_ATTRIBUTE.matcher(attribute.getName()).matches()) {
							// If this is an internal type, and the attribute name is i_*, r_*, or
							// a_*, then we skip it
							continue;
						}
						a.put(attribute.getName(), attribute);
					}
				}
			}
			type = type.getSuperType();
		}
		if (a.isEmpty()) {
			a = Collections.emptyMap();
		} else {
			a = Tools.freezeMap(new LinkedHashMap<>(a));
		}
		attributes.put(typeName, a);
	}

	public NodeGenerator(IDfSession session, Collection<String> objectTypes, DataRecordManager<?> recordsManager)
		throws DfException {
		if (objectTypes == null) {
			objectTypes = Collections.emptySet();
		}

		Map<String, Map<String, IDfAttr>> attributes = new TreeMap<>();
		Set<String> folderTypes = new TreeSet<>();
		Set<String> documentTypes = new TreeSet<>();

		this.recordsManager = recordsManager;

		for (final String typeName : objectTypes) {
			if (StringUtils.isEmpty(typeName)) {
				continue;
			}

			// This file will contain the CSV sample data we will loop around
			IDfType type = session.getType(typeName);
			if (type == null) {
				// Raise a warning
				this.log.warn("Failed to retrieve type [{}] - maybe it wasn't installed?", typeName);
				continue;
			}

			if (!type.isTypeOf("dm_sysobject")) {
				this.log.warn("Type [{}] is not of type dm_sysobject - can't generate", typeName);
				continue;
			}

			if (type.isTypeOf("dm_folder")) {
				folderTypes.add(typeName);
			} else {
				// This will include dm_sysobject and dm_document types
				documentTypes.add(typeName);
			}
		}

		if (folderTypes.isEmpty()) {
			folderTypes = Collections.singleton("dm_folder");
		} else {
			folderTypes = new LinkedHashSet<>(folderTypes);
		}
		this.folderTypes = Tools.freezeSet(folderTypes);

		if (documentTypes.isEmpty()) {
			documentTypes = Collections.singleton("dm_document");
		} else {
			documentTypes = new LinkedHashSet<>(documentTypes);
		}
		this.documentTypes = Tools.freezeSet(documentTypes);

		for (final String typeName : folderTypes) {
			NodeGenerator.buildAttributeData(session.getType(typeName), attributes);
		}
		for (final String typeName : documentTypes) {
			NodeGenerator.buildAttributeData(session.getType(typeName), attributes);
		}

		this.attributes = Tools.freezeMap(new LinkedHashMap<>(attributes));
	}

	public final Set<String> getFolderTypes() {
		return this.folderTypes;
	}

	public final Set<String> getDocumentTypes() {
		return this.documentTypes;
	}

	private IDfValue sanitizeValue(final IDfAttr attribute, String v) {
		switch (attribute.getDataType()) {
			case IDfValue.DF_BOOLEAN:
				Boolean B = (v != null ? Boolean.valueOf(v) : Boolean.FALSE);
				return new DfValue(B, IDfValue.DF_BOOLEAN);

			case IDfValue.DF_DOUBLE:
				Double D = (v != null ? Double.valueOf(v) : Double.valueOf(0.0));
				return new DfValue(D, IDfValue.DF_DOUBLE);

			case IDfValue.DF_ID:
				return NodeGenerator.NULL_ID;

			case IDfValue.DF_INTEGER:
				Integer I = (v != null ? Integer.valueOf(v) : Integer.valueOf(0));
				return new DfValue(I, IDfValue.DF_INTEGER);

			case IDfValue.DF_TIME:
				// Parse out the date in ISO-8601 UTC format
				DfTime T = DfTime.DF_NULLDATE;
				if (v != null) {
					try {
						T = new DfTime(DateUtils.parseDate(v, NodeGenerator.DATE_PATTERNS));
					} catch (ParseException e) {
						if (this.log.isDebugEnabled()) {
							this.log.warn("Failed to parse the date string [{}] using any of the formats {}", v,
								NodeGenerator.DATE_PATTERNS, e);
						}
						T = DfTime.DF_INVALIDDATE;
					}
				}
				return new DfValue(T);

			case IDfValue.DF_STRING:
				if (v != null) {
					// Truncate the string as necessary
					v = v.substring(0, attribute.getAllowedLength(v));
				}
				// Fall-through
			default: // Catch-all - anything else gets copied as a string, verbatim...
				return new DfValue(Tools.coalesce(v, ""), IDfValue.DF_STRING);

		}
	}

	private IDfValue generateRandomValue(final IDfAttr attribute) {
		switch (attribute.getDataType()) {
			case IDfValue.DF_BOOLEAN:
				return new DfValue(NodeGenerator.RANDOM.nextBoolean(), IDfValue.DF_BOOLEAN);
			case IDfValue.DF_DOUBLE:
				return new DfValue(NodeGenerator.RANDOM.nextGaussian(), IDfValue.DF_DOUBLE);
			case IDfValue.DF_ID:
				return NodeGenerator.NULL_ID;
			case IDfValue.DF_INTEGER:
				return new DfValue(NodeGenerator.RANDOM.nextInt(), IDfValue.DF_INTEGER);
			case IDfValue.DF_TIME:
				Date d = new Date();
				return new DfValue(new DfTime(d), IDfValue.DF_TIME);
			case IDfValue.DF_STRING:
				// Fall-through
			default:
				String str = UUID.randomUUID().toString();
				if (attribute.getDataType() == IDfValue.DF_STRING) {
					// Only truncate if the target type is exactly a string
					str = str.substring(0, attribute.getAllowedLength(str));
				}
				return new DfValue(str, IDfValue.DF_STRING);
		}
	}

	private void generateAttributes(String typeName, IDfSysObject sysObject) throws DfException {
		final IDfValidator validator = sysObject.getValidator();
		final boolean internalType = NodeGenerator.INTERNAL_TYPE.matcher(typeName).matches();

		Map<String, IDfAttr> attributes = this.attributes.get(typeName);
		DataRecordSet<?, ?, ?> records = this.recordsManager.getTypeRecords(typeName, 0);
		if ((attributes == null) || attributes.isEmpty()) { return; }

		DataRecord r = null;
		if (records != null) {
			synchronized (records) {
				if (records.hasNext()) {
					r = records.next();
				}
			}
		}

		// TODO: Make these configurable
		final int minRepeatingValues = 0;
		final int maxRepeatingValues = 50;

		for (final String attributeName : attributes.keySet()) {
			final IDfAttr attribute = attributes.get(attributeName);
			if (internalType && NodeGenerator.READ_ONLY_ATTRIBUTE.matcher(attributeName).matches()) {
				// If this is an internal type, and the attribute name is i_*, r_*, or a_*, then we
				// skip it
				continue;
			}

			final int valueCount;
			final List<IDfValue> externalValues;
			if ((r != null) && r.hasColumn(attributeName)) {
				String baseValue = r.get(attributeName);
				if (!attribute.isRepeating()) {
					externalValues = Collections.singletonList(sanitizeValue(attribute, baseValue));
				} else {
					List<IDfValue> l = new ArrayList<>();
					// Split by commas, then parse each value separately
					StringTokenizer tok = StringTokenizer.getCSVInstance(baseValue);
					tok.setIgnoreEmptyTokens(false); // Do not ignore empty tokens...
					for (String v : tok.getTokenList()) {
						l.add(sanitizeValue(attribute, v));
					}
					externalValues = Tools.freezeList(l);
				}
				valueCount = externalValues.size();
			} else {
				externalValues = null;
				valueCount = (!attribute.isRepeating() ? 1
					: NodeGenerator.RANDOM.nextInt(maxRepeatingValues - minRepeatingValues) + minRepeatingValues);
			}

			for (int p = 0; p < valueCount; p++) {
				final IDfValue value;
				if (externalValues != null) {
					// No need to range-check... if externalValues is not null then by
					// definition p will always be in range, since maxValueCount will be set
					// to the size of the externalValues list
					value = externalValues.get(p);
				} else {
					// No external values, so let's generate a random value for this position
					// in the (potentially) repeating attribute
					if ((validator == null) || !validator.hasValueAssistance(attributeName)) {
						value = generateRandomValue(attribute);
					} else {
						final IDfProperties dependencies = validator.getValueAssistanceDependencies(attributeName);
						final IDfValueAssistance valueAssist = validator.getValueAssistance(attributeName,
							dependencies);
						// Value assist is present, choose one of the values randomly... if it's not
						// complete, then a "random" value is also allowed
						final IDfList values = valueAssist.getActualValues();
						final int v = NodeGenerator.RANDOM
							.nextInt(values.getCount() + (valueAssist.isListComplete() ? 0 : 1));
						if (v >= values.getCount()) {
							// Generate a random value
							value = generateRandomValue(attribute);
						} else {
							// Select one of the available values from the valueAssist data
							value = new DfValue(values.get(v), attribute.getDataType());
						}
					}
				}

				// This will always work...for both single-valued and repeating
				sysObject.setRepeatingValue(attributeName, p, value);
			}
		}
	}

	private IDfSysObject generateNode(IDfFolder parent, String typeName, int childNumber, String nameFormat,
		InputStream data, int dataLength) throws DfException {
		final IDfSession session = parent.getSession();

		IDfLocalTransaction tx = null;
		try {
			if (session.isTransactionActive()) {
				tx = session.beginTransEx();
			} else {
				session.beginTrans();
			}
			final IDfSysObject sysObject = IDfSysObject.class.cast(session.newObject(typeName));

			generateAttributes(typeName, sysObject);

			Map<String, String> nameVariables = new HashMap<>();
			nameVariables.put("id", sysObject.getObjectId().getId());
			nameVariables.put("uuid", UUID.randomUUID().toString());
			nameVariables.put("number", String.format("#%08x", childNumber));
			nameVariables.put("type", typeName);
			sysObject.setObjectName(StringSubstitutor.replace(nameFormat, nameVariables));

			if (this.documentTypes.contains(typeName)) {

				DataRecord r = null;
				DataRecordSet<?, ?, ?> c = this.recordsManager.getStreamRecords(0);
				if ((c != null) && c.hasNext()) {
					r = c.next();
				}

				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					File srcFile = null;
					String contentType = "binary";
					boolean contentReady = false;

					if (r != null) {
						String newContentType = r.get("format");
						if (!StringUtils.isEmpty(newContentType)) {
							contentType = newContentType;
						}

						String src = r.get("location");
						URL srcUrl = null;
						if (!StringUtils.isEmpty(src)) {
							// Is src a URL?
							try {
								srcUrl = new URL(src);
							} catch (MalformedURLException e) {
								// Not a URL, assume it's a file
								srcFile = Tools.canonicalize(new File(src));
								srcUrl = null;
							}
						}

						if (srcUrl != null) {
							// It's a URL, so copy its contents into the BAOS...
							try (InputStream in = srcUrl.openStream()) {
								IOUtils.copy(in, baos);
								contentReady = true;
							} catch (IOException e) {
								// Failed to read
								this.log.warn("Failed to load the data from the URL [{}]", srcUrl.toString(), e);
							}
						}

						if (srcFile != null) {
							if (srcFile.exists() && srcFile.isFile() && srcFile.canRead()) {
								contentReady = true;
							}
						}
					}

					if (!contentReady) {
						try {
							NodeGenerator.copyPartial(data, baos, dataLength);
							contentReady = true;
						} catch (IOException e) {
							throw new RuntimeException("Unexpected exception writing to memory", e);
						}
					}

					// This is a document...generate a content stream
					sysObject.setContentType(contentType);
					if (srcFile != null) {
						sysObject.setFile(srcFile.getAbsolutePath());
					} else {
						sysObject.setContent(baos);
					}
				} catch (IOException e) {
					throw new RuntimeException("Unexpected exception while closing memory resources", e);
				}
			}

			sysObject.link(parent.getObjectId().getId());
			sysObject.save();
			if (tx != null) {
				session.commitTransEx(tx);
			} else {
				session.commitTrans();
			}
			return sysObject;
		} catch (DfException e) {
			if (tx != null) {
				session.abortTransEx(tx);
			} else {
				session.abortTrans();
			}
			throw e;
		}
	}

	public static IDfFolder ensureFolder(IDfSession session, String folderSpec) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to search with"); }
		if (folderSpec == null) { throw new IllegalArgumentException("Must provide a folder spec to search for"); }

		if (NodeGenerator.ID_PARSER.matcher(folderSpec).matches()) {
			// It's an (attempt at an) object ID, so we find by ID...we use this b/c it will
			// automatically fail for us if the ID isn't found, vs. us having to do the check
			return IDfFolder.class.cast(session.getObject(new DfId(folderSpec)));
		}

		// It's not an ID, so treat it as a path
		String newSpec = FilenameUtils.normalize(folderSpec, true);
		if (newSpec == null) { throw new DfException(String.format("The given path [%s] is not valid", folderSpec)); }
		if (!newSpec.startsWith("/")) {
			// We require paths to be absolute
			throw new DfException(String.format("The given path [%s] is not absolute", folderSpec));
		}
		folderSpec = newSpec;

		IDfFolder f = session.getFolderByPath(folderSpec);
		if (f != null) { return f; }

		// The folder doesn't exist, so let's figure this out...
		boolean ok = false;
		final IDfLocalTransaction tx;
		if (session.isTransactionActive()) {
			tx = session.beginTransEx();
		} else {
			session.beginTrans();
			tx = null;
		}
		try {
			List<String> components = FileNameTools.tokenize(newSpec, '/');
			StringBuilder path = new StringBuilder(folderSpec.length());
			IDfFolder prev = null;
			for (String component : components) {
				path.append('/').append(component);
				f = session.getFolderByPath(path.toString());
				if (f == null) {
					f = IDfFolder.class.cast(session.newObject(prev == null ? "dm_cabinet" : "dm_folder"));
					f.setObjectName(component);
					if (prev != null) {
						f.link(prev.getObjectId().getId());
					}
					f.save();
					f.fetch(null);
				}
				prev = f;
			}
			if (tx != null) {
				session.commitTransEx(tx);
			} else {
				session.commitTrans();
			}
			ok = true;
			return f;
		} finally {
			if (!ok) {
				if (tx != null) {
					session.abortTransEx(tx);
				} else {
					session.abortTrans();
				}
			}
		}
	}

	public int generateFolders(final IDfFolder parent, final int folderCount, final int treeDepth,
		final String nameFormat) throws DfException {
		try {
			return generateFolders(null, parent, folderCount, treeDepth, nameFormat);
		} catch (InterruptedException e) {
			// This is impossible as there is no blocking queue in play
			throw new RuntimeException("Impossible situation", e);
		}
	}

	public int generateFolders(final BlockingQueue<IDfId> queue, final IDfFolder parent, final int folderCount,
		final int treeDepth, final String nameFormat) throws DfException, InterruptedException {
		int totalCount = 0;
		final String basePath = parent.getFolderPath(0);
		final IDfSession session = parent.getSession();
		for (int i = 0; i < folderCount; i++) {
			for (String folderType : this.folderTypes) {

				IDfLocalTransaction tx = null;
				if (session.isTransactionActive()) {
					tx = session.beginTransEx();
				} else {
					session.beginTrans();
				}
				IDfSysObject so = null;
				boolean ok = false;
				try {
					so = generateNode(parent, folderType, i, nameFormat, null, 0);
					if (tx != null) {
						session.commitTransEx(tx);
					} else {
						session.commitTrans();
					}
					ok = true;
				} finally {
					if (!ok) {
						if (tx != null) {
							session.abortTransEx(tx);
						} else {
							session.abortTrans();
						}
					}
				}

				totalCount++;
				this.log.info("Generated FOLDER (object # {}) {}/{}", this.totalCount.incrementAndGet(), basePath,
					so.getObjectName());

				final IDfFolder folder = IDfFolder.class.cast(so);
				if (queue != null) {
					queue.put(folder.getObjectId());
				}
				if (treeDepth > 1) {
					totalCount += generateFolders(queue, folder, folderCount, treeDepth - 1, nameFormat);
				}
			}
		}
		return totalCount;
	}

	public int generateDocuments(final IDfFolder parent, final int documentCount, final String nameFormat,
		final InputStream data, final int dataLength) throws DfException {
		boolean ok = false;
		IDfLocalTransaction tx = null;
		final IDfSession session = parent.getSession();
		if (session.isTransactionActive()) {
			tx = session.beginTransEx();
		} else {
			session.beginTrans();
		}
		try {
			final String basePath = parent.getFolderPath(0);
			int totalCount = 0;
			for (int i = 0; i < documentCount; i++) {
				for (String documentType : this.documentTypes) {
					IDfSysObject so = generateNode(parent, documentType, i, nameFormat, data, dataLength);
					totalCount++;
					this.log.info("Generated DOCUMENT (object # {}) {}/{}", this.totalCount.incrementAndGet(), basePath,
						so.getObjectName());
				}
			}
			if (tx != null) {
				session.commitTransEx(tx);
			} else {
				session.commitTrans();
			}
			ok = true;
			return totalCount;
		} finally {
			if (!ok) {
				if (tx != null) {
					session.abortTransEx(tx);
				} else {
					session.abortTrans();
				}
			}
		}
	}

	private static long copyPartial(InputStream in, OutputStream out, long length) throws IOException {
		return NodeGenerator.copyPartial(in, out, length, 4096);
	}

	private static long copyPartial(InputStream in, OutputStream out, long length, int bufferSize) throws IOException {
		if (in == null) { throw new NullPointerException("Must provide an input stream to read from"); }
		if (out == null) { throw new NullPointerException("Must provide an output stream to write to"); }
		if (length < 0) { throw new IllegalArgumentException("Length must be > 0"); }
		if (bufferSize < 1024) { throw new IllegalArgumentException("Buffer size must be larger than 1024"); }
		final int bufMod = (bufferSize % 1024);
		if (bufMod != 0) {
			// Round up to the nearest 1KB boundary
			bufferSize += (1024 - bufMod);
		}
		byte[] buf = new byte[bufferSize];
		long chunks = (length / bufferSize);
		long written = 0;
		for (int i = 0; i < chunks; i++) {
			int r = in.read(buf);
			if (r < 0) { return written; }
			out.write(buf, 0, r);
			written += r;
		}
		// By definition, remainder will be an int, even though it's typed as a long.
		// as such, we can safely typecast it below in the read() and write() calls
		int r = in.read(buf, 0, (int) (length % bufferSize));
		if (r < 0) { return written; }
		out.write(buf, 0, r);
		written += r;
		return written;
	}
}