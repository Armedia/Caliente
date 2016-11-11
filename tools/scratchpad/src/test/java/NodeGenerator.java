import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.calienteng.DfUtils;
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

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final Random RANDOM = new Random(System.currentTimeMillis());

	private final Map<String, IDfType> types;
	private final Set<String> folderTypes;
	private final Set<String> documentTypes;

	public NodeGenerator(IDfSession session, Collection<String> objectTypes) throws DfException {
		if ((objectTypes == null) || objectTypes.isEmpty()) { throw new IllegalArgumentException(
			"Must provide a collection of object types from which to build the data tree"); }

		Map<String, IDfType> types = new TreeMap<String, IDfType>();
		Set<String> folderTypes = new TreeSet<String>();
		Set<String> documentTypes = new TreeSet<String>();

		for (String t : objectTypes) {
			if (t == null) {
				continue;
			}

			if (types.containsKey(t)) {
				// We ignore duplicates
				continue;
			}

			IDfType type = session.getType(t);
			if (type == null) {
				// Raise a warning
				this.log.warn("Failed to retrieve type [{}] - maybe it wasn't installed?", t);
				continue;
			}

			if (type.isTypeOf("dm_folder")) {
				folderTypes.add(t);
			} else if (type.isTypeOf("dm_document")) {
				documentTypes.add(t);
			}
			types.put(t, type);
		}

		this.types = Tools.freezeMap(new LinkedHashMap<String, IDfType>(types));
		this.folderTypes = Tools.freezeSet(new LinkedHashSet<String>(folderTypes));
		this.documentTypes = Tools.freezeSet(new LinkedHashSet<String>(documentTypes));
	}

	public final Set<String> getFolderTypes() {
		return this.folderTypes;
	}

	public final Set<String> getDocumentTypes() {
		return this.documentTypes;
	}

	private IDfValue generateRandomValue(final IDfAttr attribute) {
		switch (attribute.getDataType()) {
			case IDfValue.DF_BOOLEAN:
				return new DfValue(NodeGenerator.RANDOM.nextBoolean(), IDfValue.DF_BOOLEAN);
			case IDfValue.DF_DOUBLE:
				return new DfValue(NodeGenerator.RANDOM.nextGaussian(), IDfValue.DF_DOUBLE);
			case IDfValue.DF_ID:
				return new DfValue(DfId.DF_NULLID_STR, IDfValue.DF_ID);
			case IDfValue.DF_INTEGER:
				return new DfValue(NodeGenerator.RANDOM.nextInt(), IDfValue.DF_INTEGER);
			case IDfValue.DF_STRING:
				String str = UUID.randomUUID().toString();
				int trunc = attribute.getAllowedLength(str);
				return new DfValue(str.substring(0, trunc), IDfValue.DF_STRING);
			case IDfValue.DF_TIME:
				Date d = new Date();
				return new DfValue(new DfTime(d), IDfValue.DF_TIME);
			default:
				return null;
		}
	}

	private IDfSysObject generateNode(IDfFolder parent, IDfType type, int childNumber) throws DfException {
		final IDfSession session = parent.getSession();

		IDfLocalTransaction tx = null;
		try {
			if (session.isTransactionActive()) {
				tx = session.beginTransEx();
			} else {
				session.beginTrans();
			}
			final IDfSysObject sysObject = IDfSysObject.class.cast(session.newObject(type.getName()));

			sysObject.setObjectName(
				String.format("Sample %s #%08x [%s]", type.getName(), childNumber, UUID.randomUUID().toString()));

			final IDfValidator validator = sysObject.getValidator();

			// Now go through the class-specific attributes
			final int attCount = type.getTypeAttrCount();
			final int startPos = type.getInt("start_pos");
			for (int i = startPos; i < attCount; i++) {
				final IDfAttr attribute = type.getTypeAttr(i);
				final IDfValue value;
				if ((validator == null) || !validator.hasValueAssistance(attribute.getName())) {
					value = generateRandomValue(attribute);
				} else {
					final IDfProperties dependencies = validator.getValueAssistanceDependencies(attribute.getName());
					final IDfValueAssistance valueAssist = validator.getValueAssistance(attribute.getName(),
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

				if (value != null) {
					sysObject.setValue(attribute.getName(), value);
				}
			}

			if (this.documentTypes.contains(type.getName())) {
				// This is a document...generate a content stream
				sysObject.setContentType("text");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					try {
						baos.write(sysObject.getObjectName().getBytes());
					} catch (IOException e) {
						throw new RuntimeException("Unexpected exception writing to memory", e);
					}
					sysObject.setContent(baos);
				} finally {
					IOUtils.closeQuietly(baos);
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

	public static IDfFolder ensureCabinet(IDfSession session, String name) throws DfException {
		IDfFolder f = IDfFolder.class.cast(session
			.getObjectByQualification(String.format("dm_cabinet where object_name = %s", DfUtils.quoteString(name))));
		if (f == null) {
			f = IDfFolder.class.cast(session.newObject("dm_cabinet"));
			f.setObjectName(name);
			f.save();
		}
		return f;
	}

	public int generateFolders(final Collection<IDfId> list, final IDfFolder parent, final int breadth, final int depth)
		throws DfException {
		int totalCount = 0;
		final String basePath = parent.getFolderPath(0);
		final IDfSession session = parent.getSession();
		outer: for (int i = 0; (breadth <= 0) || (i < breadth);) {
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
					so = generateNode(parent, this.types.get(folderType), 1);
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
				this.log.info("Generated FOLDER {}/{}", basePath, so.getObjectName());

				final IDfFolder folder = IDfFolder.class.cast(so);
				list.add(folder.getObjectId());
				if (depth > 1) {
					totalCount += generateFolders(list, folder, breadth, depth - 1);
				}

				if ((breadth > 0) && (++i >= breadth)) {
					break outer;
				}
			}
			if (breadth <= 0) {
				break;
			}
		}
		return totalCount;
	}

	public List<IDfId> generateFolders(final IDfFolder root, final int breadth, final int depth) throws DfException {
		List<IDfId> list = new LinkedList<IDfId>();
		generateFolders(list, root, breadth, depth);
		return list;
	}

	public int generateDocuments(final IDfFolder parent, final int count) throws DfException {
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
			outer: for (int i = 0; (count <= 0) || (i < count);) {
				for (String documentType : this.documentTypes) {
					IDfSysObject so = generateNode(parent, this.types.get(documentType), 1);
					totalCount++;
					this.log.info("Generated DOCUMENT {}/{}", basePath, so.getObjectName());
					if ((count > 0) && (++i >= count)) {
						break outer;
					}
				}
				if (count <= 0) {
					break;
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
}