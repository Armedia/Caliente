/**
 *
 */

package com.armedia.cmf.storage.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import javax.sql.DataSource;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectHandler;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueCodec;
import com.armedia.cmf.storage.CmfValueDecoderException;
import com.armedia.cmf.storage.CmfValueEncoderException;
import com.armedia.cmf.storage.CmfValueSerializer;
import com.armedia.cmf.storage.tools.MimeTools;
import com.armedia.commons.dslocator.DataSourceDescriptor;
import com.armedia.commons.utilities.Tools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class JdbcObjectStore extends CmfObjectStore<Connection, JdbcOperation> {

	private static final String PROPERTY_TABLE = "cmf_info";
	private static final String SCHEMA_CHANGE_LOG = "db.changelog.xml";

	private static final String NULL = "{NULL-VALUE}";

	private static final String CHECK_IF_OBJECT_EXISTS_SQL = "select object_id from cmf_object where object_id = ? and object_type = ?";

	private static final String OBJECT_NUMBER_COLUMN = "object_number";

	private static final String INSERT_OBJECT_SQL = "insert into cmf_object (object_id, search_key, object_type, object_subtype, object_label, batch_id, product_name, product_version) values (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String INSERT_ATTRIBUTE_SQL = "insert into cmf_attribute (object_id, name, id, data_type, length, qualifiable, repeating) values (?, ?, ?, ?, ?, ?, ?)";
	private static final String INSERT_ATTRIBUTE_VALUE_SQL = "insert into cmf_attribute_value (object_id, name, value_number, null_value, data) values (?, ?, ?, ?, ?)";
	private static final String INSERT_PROPERTY_SQL = "insert into cmf_property (object_id, name, data_type, repeating) values (?, ?, ?, ?)";
	private static final String INSERT_PROPERTY_VALUE_SQL = "insert into cmf_property_value (object_id, name, value_number, null_value, data) values (?, ?, ?, ?, ?)";

	private static final String INSERT_CONTENT_SQL = "insert into cmf_content (object_id, qualifier, content_number, stream_length, mime_type, file_name) values (?, ?, ?, ?, ?, ?)";
	private static final String DELETE_CONTENT_SQL = "delete from cmf_content where object_id = ?";
	private static final String INSERT_CONTENT_PROPERTY_SQL = "insert into cmf_content_property (object_id, qualifier, name, value) values (?, ?, ?, ?)";

	private static final String QUERY_EXPORT_PLAN_DUPE_SQL = "select * from cmf_export_plan where object_id = ?";
	private static final String INSERT_EXPORT_PLAN_SQL = "insert into cmf_export_plan (object_type, object_id) values (?, ?)";

	private static final String CLEAR_ALL_MAPPINGS_SQL = "truncate table cmf_mapper";
	private static final String LOAD_ALL_MAPPINGS_SQL = "select distinct object_type, name from cmf_mapper order by object_type, name";
	private static final String LOAD_TYPE_MAPPINGS_SQL = "select distinct name from cmf_mapper where object_type = ? order by name";
	private static final String LOAD_TYPE_NAME_MAPPINGS_SQL = "select source_value, target_value from cmf_mapper where object_type = ? and name = ? order by source_value";
	private static final String FIND_EXACT_MAPPING_SQL = "select target_value from cmf_mapper where object_type = ? and name = ? and source_value = ? and target_value = ?";
	private static final String FIND_TARGET_MAPPING_SQL = "select target_value from cmf_mapper where object_type = ? and name = ? and source_value = ?";
	private static final String FIND_SOURCE_MAPPING_SQL = "select source_value from cmf_mapper where object_type = ? and name = ? and target_value = ?";
	private static final String INSERT_MAPPING_SQL = "insert into cmf_mapper (object_type, name, source_value, target_value) values (?, ?, ?, ?)";
	private static final String DELETE_TARGET_MAPPING_SQL = "delete from cmf_mapper where object_type = ? and name = ? and source_value = ?";
	private static final String DELETE_SOURCE_MAPPING_SQL = "delete from cmf_mapper where object_type = ? and name = ? and target_value = ?";
	private static final String DELETE_BOTH_MAPPINGS_SQL = "delete from cmf_mapper where object_type = ? and name = ? and not (source_value = ? and target_value = ?) and (source_value = ? or target_value = ?)";

	private static final String LOAD_OBJECT_TYPES_SQL = //
	"   select object_type, count(*) as total " + //
		" from cmf_object " + //
		"group by object_type " + // ;
		"having total > 0 " + //
		"order by object_type ";

	private static final String LOAD_CONTENTS_SQL = //
	"    select * " + //
		"  from cmf_content " + //
		" where object_id = ? " + //
		" order by content_number";

	private static final String LOAD_OBJECTS_SQL = //
	"    select * " + //
		"  from cmf_object " + //
		" where object_type = ? " + //
		" order by object_number";

	private static final String LOAD_OBJECTS_BATCHED_SQL = //
	"    select * " + //
		"  from cmf_object " + //
		" where object_type = ? " + //
		" order by batch_id, object_number";

	private static final String LOAD_OBJECTS_BY_ID_ANY_SQL = //
	"    select * " + //
		"  from cmf_object " + //
		" where object_type = ? " + //
		"   and object_id = any ( ? ) " + //
		" order by object_number";

	private static final String LOAD_OBJECTS_BY_ID_ANY_BATCHED_SQL = //
	"    select * " + //
		"  from cmf_object " + //
		" where object_type = ? " + //
		"   and object_id = any ( ? ) " + //
		" order by batch_id, object_number";

	private static final String LOAD_OBJECTS_BY_ID_IN_SQL = //
	"    select o.* " + //
		"  from cmf_object o, table(x varchar=?) t " + //
		" where o.object_type = ? " + //
		"   and o.object_id = t.x " + //
		" order by o.object_number";

	private static final String LOAD_OBJECTS_BY_ID_IN_BATCHED_SQL = //
	"    select o.* " + //
		"  from cmf_object o, table(x varchar=?) t " + //
		" where o.object_type = ? " + //
		"   and o.object_id = t.x " + //
		" order by o.batch_id, o.object_number";

	private static final String LOAD_ATTRIBUTES_SQL = //
	"    select * " + //
		"  from cmf_attribute " + //
		" where object_id = ? " + //
		" order by name";

	private static final String LOAD_ATTRIBUTE_VALUES_SQL = //
	"    select * " + //
		"  from cmf_attribute_value " + //
		" where object_id = ? " + //
		"   and name = ? " + //
		" order by value_number";

	private static final String LOAD_PROPERTIES_SQL = //
	"    select * " + //
		"  from cmf_property " + //
		" where object_id = ? " + //
		" order by name";

	private static final String LOAD_PROPERTY_VALUES_SQL = //
	"    select * " + //
		"  from cmf_property_value " + //
		" where object_id = ? " + //
		"   and name = ? " + //
		" order by value_number";

	private static final String LOAD_CONTENT_PROPERTIES_SQL = //
	"    select * " + //
		"  from cmf_content_property " + //
		" where object_id = ? " + //
		"   and qualifier = ? " + //
		" order by name ";

	private final ResultSetHandler<Long> objectNumberHandler = new ResultSetHandler<Long>() {

		private volatile Integer codePath = null;

		private int selectCodePath(ResultSet rs) throws SQLException {
			if (this.codePath == null) {
				synchronized (this) {
					if (this.codePath == null) {
						ResultSetMetaData md = rs.getMetaData();
						final int columns = md.getColumnCount();
						int cp = 1; // The default code path
						if (columns > 1) {
							for (int i = 1; i <= columns; i++) {
								if (JdbcObjectStore.OBJECT_NUMBER_COLUMN.equalsIgnoreCase(md.getColumnName(i))) {
									cp = 2; // The name-based code path
									break;
								}
							}
							// TODO: Other code paths may be needed for other databases
						}
						this.codePath = cp;
					}
				}
			}
			return this.codePath;
		}

		@Override
		public Long handle(ResultSet rs) throws SQLException {
			if (rs.next()) {
				switch (selectCodePath(rs)) {
					case 1:
						return rs.getLong(1);
					case 2:
						return rs.getLong(JdbcObjectStore.OBJECT_NUMBER_COLUMN);
				}
			}
			return null;
		}
	};

	private final boolean managedTransactions;
	private final DataSourceDescriptor<?> dataSourceDescriptor;
	private final DataSource dataSource;
	private final JdbcStorePropertyManager propertyManager;

	public JdbcObjectStore(DataSourceDescriptor<?> dataSourceDescriptor, boolean updateSchema, boolean cleanData)
		throws CmfStorageException {
		super(JdbcOperation.class, true);
		if (dataSourceDescriptor == null) { throw new IllegalArgumentException(
			"Must provide a valid DataSource instance"); }
		this.dataSourceDescriptor = dataSourceDescriptor;
		this.managedTransactions = dataSourceDescriptor.isManagedTransactions();
		this.dataSource = dataSourceDescriptor.getDataSource();

		Connection c = null;
		try {
			c = this.dataSource.getConnection();
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to get a SQL Connection to validate the schema", e);
		}

		this.propertyManager = new JdbcStorePropertyManager(JdbcObjectStore.PROPERTY_TABLE);

		JdbcSchemaManager.prepareSchema(JdbcObjectStore.SCHEMA_CHANGE_LOG, c, updateSchema, cleanData,
			this.managedTransactions, new JdbcSchemaManager.Callback() {
				@Override
				public void cleanData(JdbcOperation op) throws CmfStorageException {
					clearProperties(op);
					clearAllObjects(op);
					clearAttributeMappings(op);
				}
			});
	}

	protected final DataSourceDescriptor<?> getDataSourceDescriptor() {
		return this.dataSourceDescriptor;
	}

	@Override
	protected <V> Long storeObject(JdbcOperation operation, CmfObject<V> object, CmfAttributeTranslator<V> translator)
		throws CmfStorageException, CmfValueEncoderException {
		final Connection c = operation.getConnection();
		final CmfType objectType = object.getType();
		final String objectId = JdbcTools.composeDatabaseId(object);

		Collection<Object[]> attributeParameters = new ArrayList<Object[]>();
		Collection<Object[]> attributeValueParameters = new ArrayList<Object[]>();
		Collection<Object[]> propertyParameters = new ArrayList<Object[]>();
		Collection<Object[]> propertyValueParameters = new ArrayList<Object[]>();
		Object[] attData = new Object[7];
		Object[] attValue = new Object[5];
		Object[] propData = new Object[4];

		try {
			QueryRunner qr = JdbcTools.getQueryRunner();

			// Then, insert its attributes
			attData[0] = objectId; // This should never change within the loop
			attData[4] = 0; // Explicitly hardcoded
			attData[5] = false; // Explicitly hardcoded
			attValue[0] = objectId; // This should never change within the loop
			final Map<String, String> encodedNames = new HashMap<String, String>();
			for (final CmfAttribute<V> attribute : object.getAttributes()) {
				final String name = translator.encodeAttributeName(object.getType(), attribute.getName());
				final String duplicate = encodedNames.put(name, attribute.getName());
				if (duplicate != null) {
					this.log.warn(String.format(
						"Duplicate encoded attribute name [%s] resulted from encoding [%s] (previous encoding came from [%s])",
						name, attribute.getName(), duplicate));
					continue;
				}
				final boolean repeating = attribute.isRepeating();
				final String type = translator.encodeValue(attribute.getType());

				attData[1] = name;
				attData[2] = name;
				attData[3] = type;
				attData[6] = repeating;

				// Insert the attribute
				attributeParameters.add(attData.clone());

				if (attribute.getValueCount() <= 0) {
					continue;
				}

				attValue[1] = name; // This never changes inside this next loop
				int v = 0;
				// No special treatment, simply dump out all the values
				final CmfValueCodec<V> codec = translator.getCodec(attribute.getType());
				final CmfValueSerializer serializer = CmfValueSerializer.get(attribute.getType());
				if (serializer != null) {
					for (V value : attribute) {
						CmfValue encoded = codec.encodeValue(value);
						attValue[2] = v;
						attValue[3] = encoded.isNull();
						if (!encoded.isNull()) {
							try {
								attValue[4] = serializer.serialize(encoded);
							} catch (ParseException e) {
								throw new CmfValueEncoderException(
									String.format("Failed to encode value #%d for attribute [%s::%s]: %s", v,
										attValue[0], attValue[1], encoded),
									e);
							}
						} else {
							attValue[4] = JdbcObjectStore.NULL;
						}
						attributeValueParameters.add(attValue.clone());
						v++;
					}
				}
			}

			// Then, the properties
			encodedNames.clear();
			propData[0] = objectId; // This should never change within the loop
			for (final CmfProperty<V> property : object.getProperties()) {
				final String name = property.getName();
				final String duplicate = encodedNames.put(name, property.getName());
				if (duplicate != null) {
					this.log.warn(String.format(
						"Duplicate encoded property name [%s] resulted from encoding [%s] (previous encoding came from [%s])",
						name, property.getName(), duplicate));
					continue;
				}
				final String type = translator.encodeValue(property.getType());

				propData[1] = name;
				propData[2] = type;
				propData[3] = property.isRepeating();

				// Insert the attribute
				propertyParameters.add(propData.clone());

				attValue[1] = name; // This never changes inside this next loop
				int v = 0;
				// No special treatment, simply dump out all the values
				final CmfValueCodec<V> codec = translator.getCodec(property.getType());
				final CmfValueSerializer serializer = CmfValueSerializer.get(property.getType());
				if (serializer != null) {
					for (V value : property) {
						CmfValue encoded = codec.encodeValue(value);
						attValue[2] = v;
						attValue[3] = encoded.isNull();
						if (!encoded.isNull()) {
							try {
								attValue[4] = serializer.serialize(encoded);
							} catch (ParseException e) {
								throw new CmfValueEncoderException(
									String.format("Failed to encode value #%d for property [%s::%s]: %s", v,
										attValue[0], attValue[1], encoded),
									e);
							}
						} else {
							attValue[4] = JdbcObjectStore.NULL;
						}
						propertyValueParameters.add(attValue.clone());
						v++;
					}
				}
			}

			// Do all the inserts in a row
			Long ret = qr.insert(c, JdbcObjectStore.INSERT_OBJECT_SQL, this.objectNumberHandler, objectId,
				object.getSearchKey(), objectType.name(), Tools.coalesce(object.getSubtype(), objectType.name()),
				object.getLabel(), object.getBatchId(), object.getProductName(), object.getProductVersion());
			qr.insertBatch(c, JdbcObjectStore.INSERT_ATTRIBUTE_SQL, JdbcTools.HANDLER_NULL,
				attributeParameters.toArray(JdbcTools.NO_PARAMS));
			qr.insertBatch(c, JdbcObjectStore.INSERT_ATTRIBUTE_VALUE_SQL, JdbcTools.HANDLER_NULL,
				attributeValueParameters.toArray(JdbcTools.NO_PARAMS));
			qr.insertBatch(c, JdbcObjectStore.INSERT_PROPERTY_SQL, JdbcTools.HANDLER_NULL,
				propertyParameters.toArray(JdbcTools.NO_PARAMS));
			qr.insertBatch(c, JdbcObjectStore.INSERT_PROPERTY_VALUE_SQL, JdbcTools.HANDLER_NULL,
				propertyValueParameters.toArray(JdbcTools.NO_PARAMS));
			// lockRS.updateBoolean(1, true);
			// lockRS.updateRow();
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Stored object #%d: %s", ret, object));
			}
			return ret;
		} catch (SQLException e) {
			throw new CmfStorageException(String.format("Failed to serialize %s", object), e);
		} finally {
			// Help the GC along...not strictly necessary, but should help manage the memory
			// footprint long-term
			attributeParameters.clear();
			attributeParameters = null;
			attributeValueParameters.clear();
			attributeValueParameters = null;
			propertyParameters.clear();
			propertyParameters = null;
			propertyValueParameters.clear();
			propertyValueParameters = null;
			attData = null;
			attValue = null;
			propData = null;
		}
	}

	@Override
	protected <V> int loadObjects(JdbcOperation operation, CmfAttributeTranslator<V> translator, final CmfType type,
		Collection<String> ids, CmfObjectHandler<V> handler, boolean batching)
			throws CmfStorageException, CmfValueDecoderException {

		// If we're retrieving by IDs and no IDs have been given, don't waste time or resources
		if ((ids != null) && ids.isEmpty()) { return 0; }

		Connection connection = operation.getConnection();
		try {
			PreparedStatement objectPS = null;
			PreparedStatement attributePS = null;
			PreparedStatement attributeValuePS = null;
			PreparedStatement propertyPS = null;
			PreparedStatement propertyValuePS = null;
			try {
				boolean limitByIDs = false;
				boolean useSqlArray = false;
				if (ids == null) {
					objectPS = connection.prepareStatement(
						batching ? JdbcObjectStore.LOAD_OBJECTS_BATCHED_SQL : JdbcObjectStore.LOAD_OBJECTS_SQL);
				} else {
					limitByIDs = true;
					try {
						objectPS = connection
							.prepareStatement(batching ? JdbcObjectStore.LOAD_OBJECTS_BY_ID_ANY_BATCHED_SQL
								: JdbcObjectStore.LOAD_OBJECTS_BY_ID_ANY_SQL);
						useSqlArray = true;
					} catch (SQLException e) {
						objectPS = connection
							.prepareStatement(batching ? JdbcObjectStore.LOAD_OBJECTS_BY_ID_IN_BATCHED_SQL
								: JdbcObjectStore.LOAD_OBJECTS_BY_ID_IN_SQL);
					}
				}

				attributePS = connection.prepareStatement(JdbcObjectStore.LOAD_ATTRIBUTES_SQL);
				attributeValuePS = connection.prepareStatement(JdbcObjectStore.LOAD_ATTRIBUTE_VALUES_SQL);
				propertyPS = connection.prepareStatement(JdbcObjectStore.LOAD_PROPERTIES_SQL);
				propertyValuePS = connection.prepareStatement(JdbcObjectStore.LOAD_PROPERTY_VALUES_SQL);

				ResultSet objectRS = null;
				ResultSet attributeRS = null;
				ResultSet propertyRS = null;
				ResultSet valueRS = null;

				if (!limitByIDs) {
					objectPS.setString(1, type.name());
				} else {
					// Process the IDs
					Object[] arr = ids.toArray();
					for (int i = 0; i < arr.length; i++) {
						arr[i] = JdbcTools.composeDatabaseId(type, arr[i].toString());
					}
					if (useSqlArray) {
						objectPS.setString(1, type.name());
						objectPS.setArray(2, connection.createArrayOf("text", arr));
					} else {
						objectPS.setObject(1, arr);
						objectPS.setString(2, type.name());
					}
				}
				objectRS = objectPS.executeQuery();
				String currentBatch = null;
				boolean ok = false;
				int ret = 0;
				try {
					while (objectRS.next()) {
						final CmfObject<V> obj;
						try {
							final int objNum = objectRS.getInt("object_number");
							// If batching is not required, then we simply use the object number
							// as the batch ID, to ensure that object_number remains the sole
							// ordering factor
							if (batching) {
								String batchId = objectRS.getString("batch_id");
								if ((batchId == null) || objectRS.wasNull()) {
									batchId = String.format("%08x", objNum);
								}
								if (!Tools.equals(currentBatch, batchId)) {
									if (currentBatch != null) {
										if (this.log.isDebugEnabled()) {
											this.log.debug(String.format("CLOSE BATCH: %s", currentBatch));
										}
										if (!handler.closeBatch(true)) {
											this.log
												.warn(String.format("%s batch [%s] requested processing cancellation",
													type.name(), batchId));
											currentBatch = null;
											break;
										}
									}

									if (this.log.isDebugEnabled()) {
										this.log.debug(String.format("NEW BATCH: %s", batchId));
									}
									if (!handler.newBatch(batchId)) {
										this.log.warn(String.format("%s batch [%s] skipped", type.name(), batchId));
										continue;
									}
									currentBatch = batchId;
								}
							}

							final String objId = objectRS.getString("object_id");
							final String objLabel = objectRS.getString("object_label");

							if (this.log.isInfoEnabled()) {
								this.log.info(String.format("De-serializing %s object #%d [%s](%s)", type, objNum,
									objLabel, objId));
							}

							obj = loadObject(translator, objectRS);
							if (this.log.isTraceEnabled()) {
								this.log.trace(String.format("De-serialized %s object #%d: %s", type, objNum, obj));
							} else if (this.log.isDebugEnabled()) {
								this.log.debug(String.format("De-serialized %s object #%d [%s](%s)", type, objNum,
									objLabel, objId));
							}

							attributePS.clearParameters();
							attributePS.setString(1, objId);
							attributeRS = attributePS.executeQuery();
							try {
								loadAttributes(translator, attributeRS, obj);
							} finally {
								DbUtils.closeQuietly(attributeRS);
							}

							attributeValuePS.clearParameters();
							attributeValuePS.setString(1, objId);
							for (CmfAttribute<V> att : obj.getAttributes()) {
								// We need to re-encode, since that's the value that will be
								// referenced in the DB
								attributeValuePS.setString(2, translator.encodeAttributeName(type, att.getName()));
								valueRS = attributeValuePS.executeQuery();
								try {
									loadValues(translator.getCodec(att.getType()),
										CmfValueSerializer.get(att.getType()), valueRS, att);
								} finally {
									DbUtils.closeQuietly(valueRS);
								}
							}

							propertyPS.clearParameters();
							propertyPS.setString(1, objId);
							propertyRS = propertyPS.executeQuery();
							try {
								loadProperties(translator, propertyRS, obj);
							} finally {
								DbUtils.closeQuietly(propertyRS);
							}

							propertyValuePS.clearParameters();
							propertyValuePS.setString(1, objId);
							for (CmfProperty<V> prop : obj.getProperties()) {
								// We need to re-encode, since that's the value that will be
								// referenced in the DB
								propertyValuePS.setString(2, prop.getName());
								valueRS = propertyValuePS.executeQuery();
								try {
									loadValues(translator.getCodec(prop.getType()),
										CmfValueSerializer.get(prop.getType()), valueRS, prop);
								} finally {
									DbUtils.closeQuietly(valueRS);
								}
							}
						} catch (SQLException e) {
							if (!handler.handleException(e)) { throw new CmfStorageException(
								"Exception raised while loading objects - ObjectHandler did not handle the exception",
								e); }
							continue;
						}

						try {
							if (!handler.handleObject(obj)) {
								if (this.log.isDebugEnabled()) {
									this.log.debug(
										String.format("ObjectHandler requested load loop break on object: %s", obj));
								}
								break;
							}
						} finally {
							ret++;
						}
					}
					ok = true;
					return ret;
				} finally {
					// Make sure we clean up after ourselves
					if (currentBatch != null) {
						try {
							handler.closeBatch(ok);
						} catch (CmfStorageException e) {
							this.log.error(
								String.format("Exception caught attempting to close the pending batch [%s] (ok=%s)",
									currentBatch, ok),
								e);
						}
					}
					DbUtils.closeQuietly(objectRS);
				}
			} finally {
				DbUtils.closeQuietly(propertyValuePS);
				DbUtils.closeQuietly(propertyPS);
				DbUtils.closeQuietly(attributeValuePS);
				DbUtils.closeQuietly(attributePS);
				DbUtils.closeQuietly(objectPS);
			}
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Exception raised trying to deserialize objects of type [%s]", type), e);
		}
	}

	/**
	 * <p>
	 * Assigns the given targetId as the new ID for the object with the given source ID
	 * </p>
	 *
	 */
	protected void createMapping(Connection c, CmfType type, String name, String sourceValue, String targetValue)
		throws SQLException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		final QueryRunner qr = new QueryRunner();

		if ((targetValue == null) || (sourceValue == null)) {
			// Delete instead
			final String sql = (targetValue == null ? JdbcObjectStore.DELETE_TARGET_MAPPING_SQL
				: JdbcObjectStore.DELETE_SOURCE_MAPPING_SQL);
			final String refValue = (targetValue == null ? sourceValue : targetValue);
			int count = qr.update(c, sql, type.name(), name, refValue);
			if (count > 0) {
				this.log.info(String.format("Deleted the mappings [%s/%s/%s->%s] : %d", type, name, sourceValue,
					targetValue, count));
			}
			return;
		}

		// This delete will clear out any potential conflicts in one fell swoop, while also allowing
		// us to potentially avoid re-creating mappings that are already there.
		int deleteCount = qr.update(c, JdbcObjectStore.DELETE_BOTH_MAPPINGS_SQL, type.name(), name, sourceValue,
			targetValue, sourceValue, targetValue);

		// First, check to see if the exact mapping we're looking to create already exists...
		// If the deleteCount is 0, then that means that either there was no mapping there,
		// or the exact mapping we wanted was already there. So if the deleteCount is not 0,
		// we're already good to go on the insert. Otherwise, we have to check for an
		// existing, identical mapping
		if ((deleteCount > 0) || !qr.query(c, JdbcObjectStore.FIND_EXACT_MAPPING_SQL, JdbcTools.HANDLER_EXISTS,
			type.name(), name, sourceValue, targetValue)) {
			// New mapping...so...we need to delete anything that points to this source, and
			// anything that points to this target, since we don't accept duplicates on either
			// column

			// Now, add the new mapping...
			qr.insert(c, JdbcObjectStore.INSERT_MAPPING_SQL, JdbcTools.HANDLER_NULL, type.name(), name, sourceValue,
				targetValue);
			this.log
				.info(String.format("Established the mapping [%s/%s/%s->%s]", type, name, sourceValue, targetValue));
		} else if (this.log.isDebugEnabled()) {
			this.log.debug(
				String.format("The mapping [%s/%s/%s->%s] already exists", type, name, sourceValue, targetValue));
		}
	}

	@Override
	protected void createMapping(JdbcOperation operation, CmfType type, String name, String sourceValue,
		String targetValue) {
		try {
			createMapping(operation.getConnection(), type, name, sourceValue, targetValue);
		} catch (SQLException e) {
			throw new RuntimeException(
				String.format("Failed to create the mapping for [%s/%s/%s->%s]", type, name, sourceValue, targetValue),
				e);
		}
	}

	@Override
	protected String getMapping(JdbcOperation operation, boolean source, CmfType type, String name, String value)
		throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (value == null) { throw new IllegalArgumentException("Must provide a value to search against"); }
		final QueryRunner qr = new QueryRunner(this.dataSource);
		ResultSetHandler<String> h = new ResultSetHandler<String>() {
			@Override
			public String handle(ResultSet rs) throws SQLException {
				if (!rs.next()) { return null; }
				return rs.getString(1);
			}
		};
		final String sql = (source ? JdbcObjectStore.FIND_TARGET_MAPPING_SQL : JdbcObjectStore.FIND_SOURCE_MAPPING_SQL);
		try {
			return qr.query(sql, h, type.name(), name, value);
		} catch (SQLException e) {
			throw new CmfStorageException(String.format("Failed to retrieve the %s mapping for [%s::%s(%s)]",
				source ? "source" : "target", type, name, value), e);
		}
	}

	private boolean isStored(Connection c, CmfType type, String objectId) throws SQLException {
		return JdbcTools.getQueryRunner().query(c, JdbcObjectStore.CHECK_IF_OBJECT_EXISTS_SQL, JdbcTools.HANDLER_EXISTS,
			JdbcTools.composeDatabaseId(type, objectId), type.name());
	}

	@Override
	protected boolean isStored(JdbcOperation operation, CmfType type, String objectId) throws CmfStorageException {
		try {
			return isStored(operation.getConnection(), type, objectId);
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to check whether object [%s] was already serialized", objectId), e);
		}
	}

	private Map<CmfType, Integer> getStoredObjectTypes(Connection c) throws CmfStorageException {
		try {
			return new QueryRunner().query(c, JdbcObjectStore.LOAD_OBJECT_TYPES_SQL,
				new ResultSetHandler<Map<CmfType, Integer>>() {
					@Override
					public Map<CmfType, Integer> handle(ResultSet rs) throws SQLException {
						Map<CmfType, Integer> ret = new EnumMap<CmfType, Integer>(CmfType.class);
						while (rs.next()) {
							String t = rs.getString("object_type");
							if ((t == null) || rs.wasNull()) {
								JdbcObjectStore.this.log.warn(String.format("NULL TYPE STORED IN DATABASE: [%s]", t));
								continue;
							}
							try {
								ret.put(CmfType.decodeString(t), rs.getInt("total"));
							} catch (IllegalArgumentException e) {
								JdbcObjectStore.this.log
									.warn(String.format("UNSUPPORTED TYPE STORED IN DATABASE: [%s]", t));
								continue;
							}
						}
						return ret;
					}
				});
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to retrieve the stored object types", e);
		}
	}

	@Override
	protected Map<CmfType, Integer> getStoredObjectTypes(JdbcOperation operation) throws CmfStorageException {
		return getStoredObjectTypes(operation.getConnection());
	}

	private int clearAttributeMappings(Connection c) throws CmfStorageException {
		try {
			return new QueryRunner().update(c, JdbcObjectStore.CLEAR_ALL_MAPPINGS_SQL);
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to clear all the stored mappings", e);
		}
	}

	@Override
	protected int clearAttributeMappings(JdbcOperation operation) throws CmfStorageException {
		return clearAttributeMappings(operation.getConnection());
	}

	@Override
	protected Map<CmfType, Set<String>> getAvailableMappings(JdbcOperation operation) throws CmfStorageException {
		final Map<CmfType, Set<String>> ret = new EnumMap<CmfType, Set<String>>(CmfType.class);
		ResultSetHandler<Void> h = new ResultSetHandler<Void>() {
			@Override
			public Void handle(ResultSet rs) throws SQLException {
				CmfType currentType = null;
				Set<String> names = null;
				while (rs.next()) {
					final CmfType newType = CmfType.decodeString(rs.getString("object_type"));
					if (newType != currentType) {
						names = new TreeSet<String>();
						ret.put(newType, names);
						currentType = newType;
					}
					names.add(rs.getString("name"));
				}
				return null;
			}
		};
		try {
			new QueryRunner().query(operation.getConnection(), JdbcObjectStore.LOAD_ALL_MAPPINGS_SQL, h);
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to retrieve the declared mapping types and names", e);
		}
		return ret;
	}

	@Override
	protected Set<String> getAvailableMappings(JdbcOperation operation, CmfType type) throws CmfStorageException {
		final Set<String> ret = new TreeSet<String>();
		ResultSetHandler<Void> h = new ResultSetHandler<Void>() {
			@Override
			public Void handle(ResultSet rs) throws SQLException {
				while (rs.next()) {
					ret.add(rs.getString("name"));
				}
				return null;
			}
		};
		try {
			new QueryRunner().query(operation.getConnection(), JdbcObjectStore.LOAD_TYPE_MAPPINGS_SQL, h, type.name());
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to retrieve the declared mapping names for type [%s]", type), e);
		}
		return ret;
	}

	@Override
	protected Map<String, String> getMappings(JdbcOperation operation, CmfType type, String name) {
		final Map<String, String> ret = new HashMap<String, String>();
		ResultSetHandler<Void> h = new ResultSetHandler<Void>() {
			@Override
			public Void handle(ResultSet rs) throws SQLException {
				while (rs.next()) {
					ret.put(rs.getString("source_value"), rs.getString("target_value"));
				}
				return null;
			}
		};
		try {
			new QueryRunner().query(operation.getConnection(), JdbcObjectStore.LOAD_TYPE_NAME_MAPPINGS_SQL, h,
				type.name(), name);
		} catch (SQLException e) {
			throw new RuntimeException(
				String.format("Failed to retrieve the declared mappings for [%s::%s]", type, name), e);
		}
		return ret;
	}

	@Override
	protected final void clearAllObjects(JdbcOperation operation) throws CmfStorageException {
		// Allow for subclasses to implement optimized clearing operations
		if (optimizedClearAllObjects(operation)) { return; }
		try {
			clearAllObjects(operation.getConnection());
		} catch (SQLException e) {
			throw new CmfStorageException("SQLException caught while removing all objects", e);
		}
	}

	protected boolean optimizedClearAllObjects(JdbcOperation operation) throws CmfStorageException {
		String[] tables = {
			"CMF_CONTENT_PROPERTY", "CMF_PROPERTY_VALUE", "CMF_ATTRIBUTE_VALUE", "CMF_MAPPER",
			// "CMF_CONTENT", "CMF_PROPERTY", "CMF_ATTRIBUTE", "CMF_OBJECT",
			// "CMF_EXPORT_PLAN", "CMF_INFO"
		};
		final Connection c = operation.getConnection();
		try {
			Statement s = c.createStatement();
			try {
				for (String t : tables) {
					s.executeUpdate(String.format("truncate table %s", t));
				}
			} finally {
				DbUtils.close(s);
			}
			return true;
		} catch (SQLException e) {
			this.log.trace("Failed to truncate the tables", e);
			return false;
		}
	}

	private void clearAllObjects(Connection c) throws SQLException {
		// Allow for subclasses to implement optimized clearing operations
		DatabaseMetaData dmd = c.getMetaData();
		ResultSet rs = null;
		Set<String> tableNames = new TreeSet<String>();
		try {
			rs = dmd.getTables(null, null, "CMF_%", new String[] {
				"TABLE"
			});
			while (rs.next()) {
				tableNames.add(rs.getString("TABLE_NAME"));
			}
		} finally {
			DbUtils.closeQuietly(rs);
		}
		QueryRunner qr = new QueryRunner();
		for (String tableName : tableNames) {
			if (this.log.isTraceEnabled()) {
				this.log.trace("Deleting all records from [%s]", tableName);
			}
			qr.update(c, String.format("delete from %s", tableName));
			if (this.log.isTraceEnabled()) {
				this.log.trace("Records in [%s] deleted", tableName);
			}
		}
	}

	private <V> CmfObject<V> loadObject(CmfAttributeTranslator<V> translator, ResultSet rs) throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		CmfType type = CmfType.decodeString(rs.getString("object_type"));
		String id = rs.getString("object_id");
		Matcher m = JdbcTools.OBJECT_ID_PARSER.matcher(id);
		if (m.matches()) {
			id = m.group(1);
		}
		String searchKey = rs.getString("search_key");
		if (rs.wasNull()) {
			searchKey = id;
		}
		Long number = rs.getLong("object_number");
		if (rs.wasNull()) {
			number = null;
		}
		String batchId = rs.getString("batch_id");
		String label = rs.getString("object_label");
		String subtype = rs.getString("object_subtype");
		String productName = rs.getString("product_name");
		String productVersion = rs.getString("product_version");

		return new CmfObject<V>(translator, type, id, searchKey, batchId, label, subtype, productName, productVersion,
			number);
	}

	private <V> CmfProperty<V> loadProperty(CmfType objectType, CmfAttributeTranslator<V> translator, ResultSet rs)
		throws SQLException, CmfValueDecoderException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = rs.getString("name");
		CmfDataType type = translator.decodeValue(rs.getString("data_type"));
		boolean repeating = rs.getBoolean("repeating") && !rs.wasNull();
		return new CmfProperty<V>(name, type, repeating);
	}

	private <V> CmfAttribute<V> loadAttribute(CmfType objectType, CmfAttributeTranslator<V> translator, ResultSet rs)
		throws SQLException, CmfValueDecoderException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = translator.decodeAttributeName(objectType, rs.getString("name"));
		CmfDataType type = translator.decodeValue(rs.getString("data_type"));
		boolean repeating = rs.getBoolean("repeating") && !rs.wasNull();
		return new CmfAttribute<V>(name, type, repeating);
	}

	private <V> void loadValues(CmfValueCodec<V> codec, CmfValueSerializer deserializer, ResultSet rs,
		CmfProperty<V> property) throws SQLException, CmfValueDecoderException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		List<V> values = new LinkedList<V>();
		while (rs.next()) {
			final boolean nullValue = rs.getBoolean("null_value");
			final String data = rs.getString("data");
			final CmfValue v;
			try {
				if (rs.wasNull() || (nullValue && JdbcObjectStore.NULL.equals(data))) {
					v = new CmfValue(property.getType(), null);
				} else {
					v = deserializer.deserialize(data);
				}
			} catch (ParseException e) {
				throw new CmfValueDecoderException(
					String.format("Failed to deserialize value [%s] as a %s", data, property.getType()), e);
			}
			values.add(codec.decodeValue(v));
			if (!property.isRepeating()) {
				break;
			}
		}
		property.setValues(values);
	}

	private <V> void loadAttributes(CmfAttributeTranslator<V> translator, ResultSet rs, CmfObject<V> obj)
		throws SQLException, CmfValueDecoderException {
		List<CmfAttribute<V>> attributes = new LinkedList<CmfAttribute<V>>();
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		while (rs.next()) {
			attributes.add(loadAttribute(obj.getType(), translator, rs));
		}
		obj.setAttributes(attributes);
	}

	private <V> void loadProperties(CmfAttributeTranslator<V> translator, ResultSet rs, CmfObject<V> obj)
		throws SQLException, CmfValueDecoderException {
		List<CmfProperty<V>> properties = new LinkedList<CmfProperty<V>>();
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		while (rs.next()) {
			properties.add(loadProperty(obj.getType(), translator, rs));
		}
		obj.setProperties(properties);
	}

	@Override
	protected JdbcOperation newOperation() throws CmfStorageException {
		try {
			return new JdbcOperation(this.dataSource.getConnection(), this.managedTransactions);
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to obtain a new connection from the datasource", e);
		}
	}

	@Override
	protected boolean lockForStorage(JdbcOperation operation, CmfType type, String id) throws CmfStorageException {
		final Connection c = operation.getConnection();
		QueryRunner qr = JdbcTools.getQueryRunner();
		final String dbid = JdbcTools.composeDatabaseId(type, id);
		try {
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("ATTEMPTING TO PERSIST DEPENDENCY [%s::%s]", type.name(), id));
			}
			qr.insert(c, JdbcObjectStore.INSERT_EXPORT_PLAN_SQL, JdbcTools.HANDLER_NULL, type.name(), dbid);
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("PERSISTED DEPENDENCY [%s::%s]", type.name(), id));
			}
			return true;
		} catch (SQLException e) {
			try {
				if (qr.query(c, JdbcObjectStore.QUERY_EXPORT_PLAN_DUPE_SQL, JdbcTools.HANDLER_EXISTS, dbid)) {
					// Duplicate dependency...we skip it
					if (this.log.isTraceEnabled()) {
						this.log.trace(String.format("DUPLICATE DEPENDENCY [%s::%s]", type.name(), id));
					}
					return false;
				}
			} catch (SQLException e2) {
				this.log.trace(
					String.format("Exception caught attempting to avoid a race condition with dependency [%s::%s]",
						type.name(), id),
					e2);
			}
			throw new CmfStorageException(String.format("Failed to persist the dependency [%s::%s]", type.name(), id),
				e);
		}
	}

	@Override
	protected CmfValue getProperty(JdbcOperation operation, String property) throws CmfStorageException {
		return this.propertyManager.getProperty(operation, property);
	}

	@Override
	protected CmfValue setProperty(JdbcOperation operation, String property, final CmfValue newValue)
		throws CmfStorageException {
		return this.propertyManager.setProperty(operation, property, newValue);
	}

	@Override
	protected Set<String> getPropertyNames(JdbcOperation operation) throws CmfStorageException {
		return this.propertyManager.getPropertyNames(operation);
	}

	@Override
	protected CmfValue clearProperty(JdbcOperation operation, String property) throws CmfStorageException {
		return this.propertyManager.clearProperty(operation, property);
	}

	@Override
	protected void clearProperties(JdbcOperation operation) throws CmfStorageException {
		this.propertyManager.clearProperties(operation);
	}

	@Override
	protected <V> void setContentInfo(JdbcOperation operation, CmfObject<V> object, Collection<CmfContentInfo> content)
		throws CmfStorageException {
		final Connection c = operation.getConnection();
		final String objectId = JdbcTools.composeDatabaseId(object);
		final QueryRunner qr = new QueryRunner();

		// Step 1: Delete what's there
		try {
			qr.update(c, JdbcObjectStore.DELETE_CONTENT_SQL, objectId);
		} catch (SQLException e) {
			throw new CmfStorageException(String.format("Failed to delete the existing content records for %s [%s](%s)",
				object.getType(), object.getLabel(), object.getId()), e);
		}

		// Step 2: prepare the new content records and properties
		List<Object[]> contents = new ArrayList<Object[]>();
		List<Object[]> properties = new ArrayList<Object[]>();
		Object[] cArr = new Object[6];
		Object[] pArr = new Object[4];
		int pos = 0;
		for (CmfContentInfo i : content) {
			// First, the content record...
			cArr[0] = objectId;
			cArr[1] = i.getQualifier();
			cArr[2] = pos++;
			cArr[3] = i.getLength();
			cArr[4] = Tools.toString(Tools.coalesce(i.getMimeType(), MimeTools.DEFAULT_MIME_TYPE));
			cArr[5] = i.getFileName();
			contents.add(cArr.clone());

			// Then, the properties...
			pArr[0] = objectId;
			pArr[1] = i.getQualifier();
			for (String s : i.getPropertyNames()) {
				if (s == null) {
					continue;
				}
				pArr[2] = s;
				pArr[3] = i.getProperty(s);
				if (pArr[3] == null) {
					continue;
				}
				properties.add(pArr.clone());
			}
		}

		// Step 3: execute the batch inserts
		try {
			qr.insertBatch(c, JdbcObjectStore.INSERT_CONTENT_SQL, JdbcTools.HANDLER_NULL,
				contents.toArray(JdbcTools.NO_PARAMS));
			qr.insertBatch(c, JdbcObjectStore.INSERT_CONTENT_PROPERTY_SQL, JdbcTools.HANDLER_NULL,
				properties.toArray(JdbcTools.NO_PARAMS));
		} catch (SQLException e) {
			throw new CmfStorageException(String.format("Failed to insert the new content records for %s [%s](%s)",
				object.getType(), object.getLabel(), object.getId()), e);
		}
	}

	@Override
	protected <V> List<CmfContentInfo> getContentInfo(JdbcOperation operation, CmfObject<V> object)
		throws CmfStorageException {
		final Connection c = operation.getConnection();
		final String objectId = JdbcTools.composeDatabaseId(object);

		PreparedStatement cPS = null;
		PreparedStatement pPS = null;

		try {
			cPS = c.prepareStatement(JdbcObjectStore.LOAD_CONTENTS_SQL);
			pPS = c.prepareStatement(JdbcObjectStore.LOAD_CONTENT_PROPERTIES_SQL);

			// This one will simply gather up the properties for each content record
			final ResultSetHandler<Map<String, String>> pHandler = new ResultSetHandler<Map<String, String>>() {
				@Override
				public Map<String, String> handle(ResultSet rs) throws SQLException {
					final Map<String, String> ret = new TreeMap<String, String>();
					while (rs.next()) {
						ret.put(rs.getString("name"), rs.getString("value"));
					}
					return ret;
				}
			};

			// This one will process each content record
			final ResultSetHandler<List<CmfContentInfo>> cHandler = new ResultSetHandler<List<CmfContentInfo>>() {
				@Override
				public List<CmfContentInfo> handle(ResultSet rs) throws SQLException {
					final List<CmfContentInfo> ret = new ArrayList<CmfContentInfo>();
					final QueryRunner qr = new QueryRunner();
					while (rs.next()) {
						final CmfContentInfo info = new CmfContentInfo(rs.getString("qualifier"));
						info.setLength(rs.getLong("stream_length"));
						String str = rs.getString("mime_type");
						if ((str != null) && !rs.wasNull()) {
							info.setMimeType(MimeTools.resolveMimeType(str));
						}
						str = rs.getString("file_name");
						if ((str != null) && !rs.wasNull()) {
							info.setFileName(str);
						}

						Map<String, String> props = qr.query(c, JdbcObjectStore.LOAD_CONTENT_PROPERTIES_SQL, pHandler,
							objectId, info.getQualifier());
						for (String s : props.keySet()) {
							String v = props.get(s);
							if ((s != null) && (v != null)) {
								info.setProperty(s, v);
							}
						}
						ret.add(info);
					}
					return ret;
				}
			};

			return new QueryRunner().query(c, JdbcObjectStore.LOAD_CONTENTS_SQL, cHandler, objectId);
		} catch (SQLException e) {
			throw new CmfStorageException(String.format("Failed to load the content records for %s [%s](%s)",
				object.getType(), object.getLabel(), object.getId()), e);
		} finally {
			DbUtils.closeQuietly(pPS);
			DbUtils.closeQuietly(cPS);
		}
	}
}