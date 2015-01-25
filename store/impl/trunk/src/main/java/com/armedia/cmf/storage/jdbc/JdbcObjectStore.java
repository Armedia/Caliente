/**
 *
 */

package com.armedia.cmf.storage.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectHandler;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValueCodec;
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.armedia.cmf.storage.StoredValueEncoderException;
import com.armedia.commons.dslocator.DataSourceDescriptor;
import com.armedia.commons.utilities.Tools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class JdbcObjectStore extends ObjectStore<Connection, JdbcOperation> {

	private static final Object[][] NO_PARAMS = new Object[0][0];

	private static final String CHECK_IF_OBJECT_EXISTS_SQL = "select object_id from cmf_object where object_id = ? and object_type = ?";

	private static final String INSERT_OBJECT_SQL = "insert into cmf_object (object_id, object_type, object_subtype, object_label, batch_id) values (?, ?, ?, ?, ?)";
	private static final String INSERT_ATTRIBUTE_SQL = "insert into cmf_attribute (object_id, name, id, data_type, length, qualifiable, repeating) values (?, ?, ?, ?, ?, ?, ?)";
	private static final String INSERT_ATTRIBUTE_VALUE_SQL = "insert into cmf_attribute_value (object_id, name, value_number, data) values (?, ?, ?, ?)";
	private static final String INSERT_PROPERTY_SQL = "insert into cmf_property (object_id, name, data_type, repeating) values (?, ?, ?, ?)";
	private static final String INSERT_PROPERTY_VALUE_SQL = "insert into cmf_property_value (object_id, name, value_number, data) values (?, ?, ?, ?)";

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

	private static final String LOAD_OBJECTS_SQL = //
		"    select * " + //
		"  from cmf_object " + //
		" where object_type = ? " + //
		" order by batch_id, object_number";

	private static final String LOAD_OBJECTS_BY_ID_ANY_SQL = //
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

	private static final ResultSetHandler<Object> HANDLER_NULL = new ResultSetHandler<Object>() {
		@Override
		public Object handle(ResultSet rs) throws SQLException {
			return null;
		}
	};

	private static final ResultSetHandler<Long> HANDLER_OBJECT_NUMBER = new ResultSetHandler<Long>() {
		@Override
		public Long handle(ResultSet rs) throws SQLException {
			if (rs.next()) { return rs.getLong(1); }
			return null;
		}
	};

	private static final ResultSetHandler<Boolean> HANDLER_EXISTS = new ResultSetHandler<Boolean>() {
		@Override
		public Boolean handle(ResultSet rs) throws SQLException {
			return rs.next();
		}
	};

	private final boolean managedTransactions;
	private final DataSourceDescriptor<?> dataSourceDescriptor;
	private final DataSource dataSource;

	public JdbcObjectStore(DataSourceDescriptor<?> dataSourceDescriptor, boolean updateSchema) throws StorageException {
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
			throw new StorageException("Failed to get a SQL Connection to validate the schema", e);
		}

		boolean ok = false;
		try {
			Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
			Liquibase liquibase = new Liquibase("db.changelog.xml", new ClassLoaderResourceAccessor(), database);
			if (updateSchema) {
				liquibase.update((String) null);
			} else {
				liquibase.validate();
			}
			ok = true;
		} catch (DatabaseException e) {
			throw new StorageException("Failed to find a supported database for the given connection", e);
		} catch (LiquibaseException e) {
			if (updateSchema) {
				throw new StorageException("Failed to generate/update the SQL schema", e);
			} else {
				throw new StorageException("The SQL schema is of the wrong version or structure", e);
			}
		} finally {
			finalizeTransaction(c, ok);
		}
	}

	protected final DataSourceDescriptor<?> getDataSourceDescriptor() {
		return this.dataSourceDescriptor;
	}

	private void finalizeTransaction(Connection c, boolean commit) {
		if (this.managedTransactions) {
			// We're not owning the transaction
			DbUtils.closeQuietly(c);
		} else if (commit) {
			DbUtils.commitAndCloseQuietly(c);
		} else {
			DbUtils.rollbackAndCloseQuietly(c);
		}
	}

	private static final ThreadLocal<QueryRunner> QUERY_RUNNER = new ThreadLocal<QueryRunner>();

	private static QueryRunner getQueryRunner() {
		QueryRunner q = JdbcObjectStore.QUERY_RUNNER.get();
		if (q == null) {
			q = new QueryRunner();
			JdbcObjectStore.QUERY_RUNNER.set(q);
		}
		return q;
	}

	@Override
	protected <T, V> Long doStoreObject(JdbcOperation operation, StoredObject<V> object,
		ObjectStorageTranslator<T, V> translator) throws StorageException, StoredValueEncoderException {
		final Connection c = operation.getConnection();
		final StoredObjectType objectType = object.getType();
		final String objectId = object.getId();

		Collection<Object[]> attributeParameters = new ArrayList<Object[]>();
		Collection<Object[]> attributeValueParameters = new ArrayList<Object[]>();
		Collection<Object[]> propertyParameters = new ArrayList<Object[]>();
		Collection<Object[]> propertyValueParameters = new ArrayList<Object[]>();
		Object[] attData = new Object[7];
		Object[] attValue = new Object[4];
		Object[] propData = new Object[4];

		try {
			QueryRunner qr = JdbcObjectStore.getQueryRunner();

			// Then, insert its attributes
			attData[0] = objectId; // This should never change within the loop
			attValue[0] = objectId; // This should never change within the loop
			final Map<String, String> encodedNames = new HashMap<String, String>();
			for (final StoredAttribute<V> attribute : object.getAttributes()) {
				final String name = translator.encodeAttributeName(object.getType(), attribute.getName());
				final String duplicate = encodedNames.put(name, attribute.getName());
				if (duplicate != null) {
					this.log
					.warn(String
						.format(
							"Duplicate encoded attribute name [%s] resulted from encoding [%s] (previous encoding came from [%s])",
							name, attribute.getName(), duplicate));
					continue;
				}
				final boolean repeating = attribute.isRepeating();
				final String type = translator.encodeValue(attribute.getType());

				attData[1] = name;
				attData[2] = attribute.getId();
				attData[3] = type;
				attData[4] = attribute.getLength();
				attData[5] = attribute.isQualifiable();
				attData[6] = repeating;

				// Insert the attribute
				attributeParameters.add(attData.clone());

				if (attribute.getValueCount() <= 0) {
					continue;
				}

				attValue[1] = name; // This never changes inside this next loop
				Object[][] values = new Object[attribute.getValueCount()][];
				int v = 0;
				// No special treatment, simply dump out all the values
				final StoredValueCodec<V> codec = translator.getCodec(attribute.getType());
				for (V value : attribute) {
					attValue[2] = v;
					attValue[3] = codec.encodeValue(value);
					values[v] = attValue.clone();
					attributeValueParameters.add(attValue.clone());
					v++;
				}
			}

			// Then, the properties
			encodedNames.clear();
			propData[0] = objectId; // This should never change within the loop
			for (final StoredProperty<V> property : object.getProperties()) {
				final String name = translator.encodePropertyName(object.getType(), property.getName());
				final String duplicate = encodedNames.put(name, property.getName());
				if (duplicate != null) {
					this.log
					.warn(String
						.format(
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
				Object[][] values = new Object[property.getValueCount()][];
				int v = 0;
				// No special treatment, simply dump out all the values
				final StoredValueCodec<V> codec = translator.getCodec(property.getType());
				for (V value : property) {
					attValue[2] = v;
					attValue[3] = codec.encodeValue(value);
					values[v] = attValue.clone();
					propertyValueParameters.add(attValue.clone());
					v++;
				}
			}

			// Do all the inserts in a row
			Long ret = qr.insert(c, JdbcObjectStore.INSERT_OBJECT_SQL, JdbcObjectStore.HANDLER_OBJECT_NUMBER, objectId,
				objectType.name(), Tools.coalesce(object.getSubtype(), objectType.name()), object.getLabel(),
				object.getBatchId());
			qr.insertBatch(c, JdbcObjectStore.INSERT_ATTRIBUTE_SQL, JdbcObjectStore.HANDLER_NULL,
				attributeParameters.toArray(JdbcObjectStore.NO_PARAMS));
			qr.insertBatch(c, JdbcObjectStore.INSERT_ATTRIBUTE_VALUE_SQL, JdbcObjectStore.HANDLER_NULL,
				attributeValueParameters.toArray(JdbcObjectStore.NO_PARAMS));
			qr.insertBatch(c, JdbcObjectStore.INSERT_PROPERTY_SQL, JdbcObjectStore.HANDLER_NULL,
				propertyParameters.toArray(JdbcObjectStore.NO_PARAMS));
			qr.insertBatch(c, JdbcObjectStore.INSERT_PROPERTY_VALUE_SQL, JdbcObjectStore.HANDLER_NULL,
				propertyValueParameters.toArray(JdbcObjectStore.NO_PARAMS));
			// lockRS.updateBoolean(1, true);
			// lockRS.updateRow();
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Stored object #%d: %s", ret, object));
			}
			return ret;
		} catch (SQLException e) {
			throw new RuntimeException(String.format("Failed to serialize %s", object), e);
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
	protected <T, V> int doLoadObjects(JdbcOperation operation, ObjectStorageTranslator<T, V> translator,
		final StoredObjectType type, Collection<String> ids, StoredObjectHandler<V> handler) throws StorageException,
		StoredValueDecoderException {
		Connection objectConn = null;
		Connection attributeConn = null;

		// If we're retrieving by IDs and no IDs have been given, don't waste time or resources
		if ((ids != null) && ids.isEmpty()) { return 0; }

		try {
			objectConn = this.dataSource.getConnection();
			attributeConn = this.dataSource.getConnection();

			PreparedStatement objectPS = null;
			PreparedStatement attributePS = null;
			PreparedStatement attributeValuePS = null;
			PreparedStatement propertyPS = null;
			PreparedStatement propertyValuePS = null;
			try {
				boolean limitByIDs = false;
				boolean useSqlArray = false;
				if (ids == null) {
					objectPS = objectConn.prepareStatement(JdbcObjectStore.LOAD_OBJECTS_SQL);
				} else {
					limitByIDs = true;
					try {
						objectPS = objectConn.prepareStatement(JdbcObjectStore.LOAD_OBJECTS_BY_ID_ANY_SQL);
						useSqlArray = true;
					} catch (SQLException e) {
						objectPS = objectConn.prepareStatement(JdbcObjectStore.LOAD_OBJECTS_BY_ID_IN_SQL);
					}
				}

				attributePS = attributeConn.prepareStatement(JdbcObjectStore.LOAD_ATTRIBUTES_SQL);
				attributeValuePS = attributeConn.prepareStatement(JdbcObjectStore.LOAD_ATTRIBUTE_VALUES_SQL);
				propertyPS = attributeConn.prepareStatement(JdbcObjectStore.LOAD_PROPERTIES_SQL);
				propertyValuePS = attributeConn.prepareStatement(JdbcObjectStore.LOAD_PROPERTY_VALUES_SQL);

				ResultSet objectRS = null;
				ResultSet attributeRS = null;
				ResultSet propertyRS = null;
				ResultSet valueRS = null;

				if (!limitByIDs) {
					objectPS.setString(1, type.name());
				} else {
					if (useSqlArray) {
						objectPS.setString(1, type.name());
						objectPS.setArray(2, objectConn.createArrayOf("text", ids.toArray()));
					} else {
						objectPS.setObject(1, ids.toArray());
						objectPS.setString(2, type.name());
					}
				}
				objectRS = objectPS.executeQuery();
				String currentBatch = null;
				boolean ok = false;
				int ret = 0;
				try {
					while (objectRS.next()) {
						final StoredObject<V> obj;
						try {
							final int objNum = objectRS.getInt("object_number");
							// If batching is not required, then we simply use the object number
							// as the batch ID, to ensure that object_number remains the sole
							// ordering factor
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
										this.log.warn(String.format("%s batch [%s] requested processing cancellation",
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

							final String objId = objectRS.getString("object_id");
							final String objLabel = objectRS.getString("object_label");

							if (this.log.isInfoEnabled()) {
								this.log.info(String.format("De-serializing %s object #%d [%s](%s)", type, objNum,
									objLabel, objId));
							}

							obj = loadObject(objectRS);
							if (this.log.isTraceEnabled()) {
								this.log.trace(String.format("De-serialized %s object #%d: %s", type, objNum, obj));
							} else if (this.log.isDebugEnabled()) {
								this.log.debug(String.format("De-serialized %s object #%d [%s](%s)", type, objNum,
									objLabel, objId));
							}

							attributePS.clearParameters();
							attributePS.setString(1, obj.getId());
							attributeRS = attributePS.executeQuery();
							try {
								loadAttributes(translator, attributeRS, obj);
							} finally {
								DbUtils.closeQuietly(attributeRS);
							}

							attributeValuePS.clearParameters();
							attributeValuePS.setString(1, obj.getId());
							for (StoredAttribute<V> att : obj.getAttributes()) {
								// We need to re-encode, since that's the value that will be
								// referenced in the DB
								attributeValuePS.setString(2, translator.encodeAttributeName(type, att.getName()));
								valueRS = attributeValuePS.executeQuery();
								try {
									loadValues(translator.getCodec(att.getType()), valueRS, att);
								} finally {
									DbUtils.closeQuietly(valueRS);
								}
							}

							propertyPS.clearParameters();
							propertyPS.setString(1, obj.getId());
							propertyRS = propertyPS.executeQuery();
							try {
								loadProperties(translator, propertyRS, obj);
							} finally {
								DbUtils.closeQuietly(propertyRS);
							}

							propertyValuePS.clearParameters();
							propertyValuePS.setString(1, obj.getId());
							for (StoredProperty<V> prop : obj.getProperties()) {
								// We need to re-encode, since that's the value that will be
								// referenced in the DB
								propertyValuePS.setString(2, translator.encodePropertyName(type, prop.getName()));
								valueRS = propertyValuePS.executeQuery();
								try {
									loadValues(translator.getCodec(prop.getType()), valueRS, prop);
								} finally {
									DbUtils.closeQuietly(valueRS);
								}
							}
						} catch (SQLException e) {
							if (!handler.handleException(e)) { throw new StorageException(
								"Exception raised while loading objects - ObjectHandler did not handle the exception",
								e); }
							continue;
						}

						try {
							if (!handler.handleObject(obj)) {
								if (this.log.isDebugEnabled()) {
									this.log.debug(String.format(
										"ObjectHandler requested load loop break on object: %s", obj));
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
						} catch (StorageException e) {
							this.log.error(String
								.format("Exception caught attempting to close the pending batch [%s] (ok=%s)",
									currentBatch, ok), e);
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
			throw new StorageException(String.format("Exception raised trying to deserialize objects of type [%s]",
				type), e);
		} finally {
			DbUtils.rollbackAndCloseQuietly(attributeConn);
			DbUtils.rollbackAndCloseQuietly(objectConn);
		}
	}

	/**
	 * <p>
	 * Assigns the given targetId as the new ID for the object with the given source ID
	 * </p>
	 *
	 */
	protected void doCreateMappedValue(Connection c, StoredObjectType type, String name, String sourceValue,
		String targetValue) throws SQLException {
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
		if ((deleteCount > 0)
			|| !qr.query(c, JdbcObjectStore.FIND_EXACT_MAPPING_SQL, JdbcObjectStore.HANDLER_EXISTS, type.name(), name,
				sourceValue, targetValue)) {
			// New mapping...so...we need to delete anything that points to this source, and
			// anything that points to this target, since we don't accept duplicates on either
			// column

			// Now, add the new mapping...
			qr.insert(c, JdbcObjectStore.INSERT_MAPPING_SQL, JdbcObjectStore.HANDLER_NULL, type.name(), name,
				sourceValue, targetValue);
			this.log
			.info(String.format("Established the mapping [%s/%s/%s->%s]", type, name, sourceValue, targetValue));
		} else if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("The mapping [%s/%s/%s->%s] already exists", type, name, sourceValue,
				targetValue));
		}
	}

	@Override
	protected void doCreateMappedValue(JdbcOperation operation, StoredObjectType type, String name, String sourceValue,
		String targetValue) {
		try {
			doCreateMappedValue(operation.getConnection(), type, name, sourceValue, targetValue);
		} catch (SQLException e) {
			throw new RuntimeException(String.format("Failed to create the mapping for [%s/%s/%s->%s]", type, name,
				sourceValue, targetValue), e);
		}
	}

	@Override
	protected String doGetMappedValue(JdbcOperation operation, boolean source, StoredObjectType type, String name,
		String value) throws StorageException {
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
			throw new StorageException(String.format("Failed to retrieve the %s mapping for [%s::%s(%s)]",
				source ? "source" : "target", type, name, value), e);
		}
	}

	private boolean doIsStored(Connection c, StoredObjectType type, String objectId) throws SQLException {
		return JdbcObjectStore.getQueryRunner().query(c, JdbcObjectStore.CHECK_IF_OBJECT_EXISTS_SQL,
			JdbcObjectStore.HANDLER_EXISTS, objectId, type.name());
	}

	@Override
	protected boolean doIsStored(JdbcOperation operation, StoredObjectType type, String objectId)
		throws StorageException {
		final Connection c;
		try {
			c = this.dataSource.getConnection();
		} catch (SQLException e) {
			throw new StorageException("Failed to connect to the object store's database", e);
		}
		try {
			c.setAutoCommit(false);
			return doIsStored(c, type, objectId);
		} catch (SQLException e) {
			throw new StorageException(String.format("Failed to check whether object [%s] was already serialized",
				objectId), e);
		} finally {
			DbUtils.rollbackAndCloseQuietly(c);
		}
	}

	private Map<StoredObjectType, Integer> getStoredObjectTypes(Connection c) throws StorageException {
		try {
			return new QueryRunner().query(c, JdbcObjectStore.LOAD_OBJECT_TYPES_SQL,
				new ResultSetHandler<Map<StoredObjectType, Integer>>() {
				@Override
				public Map<StoredObjectType, Integer> handle(ResultSet rs) throws SQLException {
					Map<StoredObjectType, Integer> ret = new EnumMap<StoredObjectType, Integer>(
						StoredObjectType.class);
					while (rs.next()) {
						String t = rs.getString("object_type");
						if ((t == null) || rs.wasNull()) {
							JdbcObjectStore.this.log.warn(String.format("NULL TYPE STORED IN DATABASE: [%s]", t));
							continue;
						}
						try {
							ret.put(StoredObjectType.decodeString(t), rs.getInt("total"));
						} catch (IllegalArgumentException e) {
							JdbcObjectStore.this.log.warn(String.format(
								"UNSUPPORTED TYPE STORED IN DATABASE: [%s]", t));
							continue;
						}
					}
					return ret;
				}
			});
		} catch (SQLException e) {
			throw new StorageException("Failed to retrieve the stored object types", e);
		}
	}

	@Override
	protected Map<StoredObjectType, Integer> doGetStoredObjectTypes(JdbcOperation operation) throws StorageException {
		return getStoredObjectTypes(operation.getConnection());
	}

	private int clearAttributeMappings(Connection c) throws StorageException {
		try {
			return new QueryRunner().update(c, JdbcObjectStore.CLEAR_ALL_MAPPINGS_SQL);
		} catch (SQLException e) {
			throw new StorageException("Failed to clear all the stored mappings", e);
		}
	}

	@Override
	protected int doClearAttributeMappings(JdbcOperation operation) throws StorageException {
		return clearAttributeMappings(operation.getConnection());
	}

	@Override
	protected Map<StoredObjectType, Set<String>> doGetAvailableMappings(JdbcOperation operation)
		throws StorageException {
		final Map<StoredObjectType, Set<String>> ret = new EnumMap<StoredObjectType, Set<String>>(
			StoredObjectType.class);
		ResultSetHandler<Void> h = new ResultSetHandler<Void>() {
			@Override
			public Void handle(ResultSet rs) throws SQLException {
				StoredObjectType currentType = null;
				Set<String> names = null;
				while (rs.next()) {
					final StoredObjectType newType = StoredObjectType.decodeString(rs.getString("object_type"));
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
			throw new StorageException("Failed to retrieve the declared mapping types and names", e);
		}
		return ret;
	}

	@Override
	protected Set<String> doGetAvailableMappings(JdbcOperation operation, StoredObjectType type)
		throws StorageException {
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
			throw new RuntimeException(String.format("Failed to retrieve the declared mapping names for type [%s]",
				type), e);
		}
		return ret;
	}

	@Override
	protected Map<String, String> doGetMappings(JdbcOperation operation, StoredObjectType type, String name) {
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
			throw new RuntimeException(String.format("Failed to retrieve the declared mappings for [%s::%s]", type,
				name), e);
		}
		return ret;
	}

	@Override
	protected final void doClearAllObjects(JdbcOperation operation) throws StorageException {
		// Allow for subclasses to implement optimized clearing operations
		if (doOptimizedClearAllObjects(operation)) { return; }
		Connection c = operation.getConnection();
		try {
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
			doClearTables(operation, tableNames);
		} catch (SQLException e) {
			throw new StorageException("SQLException caught while removing all objects", e);
		}
	}

	private <V> StoredObject<V> loadObject(ResultSet rs) throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		StoredObjectType type = StoredObjectType.decodeString(rs.getString("object_type"));
		String id = rs.getString("object_id");
		String batchId = rs.getString("batch_id");
		String label = rs.getString("object_label");
		String subtype = rs.getString("object_subtype");
		return new StoredObject<V>(type, id, batchId, label, subtype);
	}

	private <T, V> StoredProperty<V> loadProperty(StoredObjectType objectType,
		ObjectStorageTranslator<T, V> translator, ResultSet rs) throws SQLException, StoredValueDecoderException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = translator.decodePropertyName(objectType, rs.getString("name"));
		StoredDataType type = translator.decodeValue(rs.getString("data_type"));
		boolean repeating = rs.getBoolean("repeating") && !rs.wasNull();
		return new StoredProperty<V>(name, type, repeating);
	}

	private <T, V> StoredAttribute<V> loadAttribute(StoredObjectType objectType,
		ObjectStorageTranslator<T, V> translator, ResultSet rs) throws SQLException, StoredValueDecoderException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = translator.decodeAttributeName(objectType, rs.getString("name"));
		StoredDataType type = translator.decodeValue(rs.getString("data_type"));
		boolean repeating = rs.getBoolean("repeating") && !rs.wasNull();
		String id = rs.getString("id");
		boolean qualifiable = rs.getBoolean("qualifiable") && !rs.wasNull();
		int length = rs.getInt("length");
		return new StoredAttribute<V>(name, type, id, length, repeating, qualifiable);
	}

	private <V> void loadValues(StoredValueCodec<V> codec, ResultSet rs, StoredProperty<V> property)
		throws SQLException, StoredValueDecoderException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		List<V> values = new LinkedList<V>();
		while (rs.next()) {
			String data = rs.getString("data");
			values.add(codec.decodeValue(data));
			if (!property.isRepeating()) {
				break;
			}
		}
		property.setValues(values);
	}

	private <T, V> void loadAttributes(ObjectStorageTranslator<T, V> translator, ResultSet rs, StoredObject<V> obj)
		throws SQLException, StoredValueDecoderException {
		List<StoredAttribute<V>> attributes = new LinkedList<StoredAttribute<V>>();
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		while (rs.next()) {
			attributes.add(loadAttribute(obj.getType(), translator, rs));
		}
		obj.setAttributes(attributes);
	}

	private <T, V> void loadProperties(ObjectStorageTranslator<T, V> translator, ResultSet rs, StoredObject<V> obj)
		throws SQLException, StoredValueDecoderException {
		List<StoredProperty<V>> properties = new LinkedList<StoredProperty<V>>();
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		while (rs.next()) {
			properties.add(loadProperty(obj.getType(), translator, rs));
		}
		obj.setProperties(properties);
	}

	@Override
	protected JdbcOperation newOperation() throws StorageException {
		try {
			return new JdbcOperation(this.dataSource.getConnection(), this.managedTransactions);
		} catch (SQLException e) {
			throw new StorageException("Failed to obtain a new connection from the datasource", e);
		}
	}

	@Override
	protected boolean doLockForStorage(JdbcOperation operation, StoredObjectType type, String id)
		throws StorageException {
		final Connection c = operation.getConnection();
		QueryRunner qr = JdbcObjectStore.getQueryRunner();
		try {
			if (qr.query(c, JdbcObjectStore.QUERY_EXPORT_PLAN_DUPE_SQL, JdbcObjectStore.HANDLER_EXISTS, id)) {
				// Duplicate dependency...we skip it
				if (this.log.isTraceEnabled()) {
					this.log.trace(String.format("DUPLICATE DEPENDENCY [%s::%s]", type.name(), id));
				}
				return false;
			}
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("PERSISTING DEPENDENCY [%s::%s]", type.name(), id));
			}
			qr.insert(c, JdbcObjectStore.INSERT_EXPORT_PLAN_SQL, JdbcObjectStore.HANDLER_NULL, type.name(), id);
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("PERSISTED DEPENDENCY [%s::%s]", type.name(), id));
			}
			return true;
		} catch (SQLException e) {
			throw new StorageException(String.format("Failed to persist the dependency [%s::%s]", type.name(), id), e);
		}
	}

	protected boolean doOptimizedClearAllObjects(JdbcOperation operation) throws StorageException {
		// This method exists in case there is a faster way to clear out the database schema
		return false;
	}

	protected void doClearTables(JdbcOperation operation, Set<String> tableNames) throws SQLException {
		// TODO: We can probably find a way to do this using TRUNCATE TABLE, but there are many
		// db-specific nuances to account for, so we'll defer that for later...
		final Connection c = operation.getConnection();
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
}