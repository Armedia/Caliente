/**
 *
 */

package com.delta.cmsmf.datastore;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

import com.delta.cmsmf.cmsobjects.DctmObjectType;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.properties.CMSMFProperties;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DataStore {

	public static interface ImportHandler {
		public boolean handle(DataObject dataObject) throws Exception;
	}

	public static interface PropertyReader {
		public boolean handleAsProperty(String attributeName);

		public Collection<DataProperty> readProperties(IDfPersistentObject obj) throws DfException;
	}

	private static final PropertyReader NULL_PROPERTY_READER = new PropertyReader() {

		@Override
		public Collection<DataProperty> readProperties(IDfPersistentObject obj) throws DfException {
			return Collections.emptyList();
		}

		@Override
		public boolean handleAsProperty(String attributeName) {
			return false;
		}
	};

	private static final String CHECK_IF_OBJECT_EXISTS_SQL = "select object_id from dctm_object where object_id = ?";

	private static final String INSERT_OBJECT_SQL = "insert into dctm_object (object_id, object_type, has_content, content_path) values (?, ?, ?, ?)";
	private static final String INSERT_DEPENDENCY_SQL = "insert into dctm_dependency (object_id, dependency_id) values (?, ?)";
	private static final String INSERT_ATTRIBUTE_SQL = "insert into dctm_attribute (object_id, attribute_name, attribute_id, attribute_type, attribute_length, is_qualifiable, is_repeating) values (?, ?, ?, ?, ?, ?, ?)";
	private static final String INSERT_ATTRIBUTE_VALUE_SQL = "insert into dctm_attribute_value (object_id, attribute_name, value_number, is_null, data) values (?, ?, ?, ?, ?)";
	private static final String INSERT_PROPERTY_SQL = "insert into dctm_property (object_id, property_name, property_type, is_repeating) values (?, ?, ?, ?)";
	private static final String INSERT_PROPERTY_VALUE_SQL = "insert into dctm_property_value (object_id, property_name, value_number, is_null, data) values (?, ?, ?, ?, ?)";
	private static final String FIND_SOURCE_ID_SQL = "select source_id from dctm_mapper where target_id = ?";
	private static final String FIND_TARGET_ID_SQL = "select target_id from dctm_mapper where source_id = ?";
	private static final String INSERT_MAPPING_SQL = "insert into dctm_mapper (source_id, target_id) values (?, ?)";
	private static final String DELETE_MAPPING_SQL = "delete from dctm_mapper where source_id = ?";

	/*
	private static final String LOAD_EVERYTHING_SQL = //
		"    select o.*, a.*, v.* " + //
		"  from dctm_object o, dctm_attribute a, dctm_value v " + //
		" where o.object_type = ? " + //
		"   and o.object_id = a.object_id " + //
		"   and a.object_id = v.object_id " + //
		"   and a.attribute_name = v.attribute_name " + //
		" order by o.object_number, v.value_number";
	 */

	private static final String LOAD_OBJECTS_SQL = //
	"    select * " + //
		"  from dctm_object " + //
		" where object_type = ? " + //
		" order by object_number";

	private static final String LOAD_ATTRIBUTES_SQL = //
	"    select * " + //
		"  from dctm_attribute " + //
		" where object_id = ? " + //
		" order by attribute_name";

	private static final String LOAD_VALUES_SQL = //
	"    select * " + //
		"  from dctm_attribute_value " + //
		" where object_id = ? " + //
		"   and attribute_name = ? " + //
		" order by value_number";

	private static final ResultSetHandler<Object> HANDLER_NULL = new ResultSetHandler<Object>() {
		@Override
		public Object handle(ResultSet rs) throws SQLException {
			return null;
		}
	};

	private static final ResultSetHandler<Boolean> HANDLER_EXISTS = new ResultSetHandler<Boolean>() {
		@Override
		public Boolean handle(ResultSet rs) throws SQLException {
			return rs.next();
		}
	};

	/*
	private static final ResultSetHandler<Integer> HANDLER_COUNT_ITERATOR = new ResultSetHandler<Integer>() {
		@Override
		public Integer handle(ResultSet rs) throws SQLException {
			int ret = 0;
			while (rs.next()) {
				ret++;
			}
			return ret;
		}
	};

	private static final ResultSetHandler<Integer> HANDLER_COUNT = new ResultSetHandler<Integer>() {
		@Override
		public Integer handle(ResultSet rs) throws SQLException {
			if (!rs.next()) { return 0; }
			return rs.getInt(1);
		}
	};
	 */

	private static final Logger LOG = Logger.getLogger(DataStore.class);

	private static DataSource DATA_SOURCE;

	private DataStore() {
	}

	public static void init(final boolean clearData) throws CMSMFException {
		final String driverName = CMSMFProperties.JDBC_DRIVER.getString();
		if (!DbUtils.loadDriver(driverName)) { throw new CMSMFException(String.format(
			"Failed to locate the JDBC driver class [%s]", driverName)); }

		if (DataStore.LOG.isDebugEnabled()) {
			DataStore.LOG.debug(String.format("JDBC driver class [%s] is loaded and valid", driverName));
		}

		String jdbcUrl = CMSMFProperties.JDBC_URL.getString();
		String targetPath = CMSMFProperties.STREAMS_DIRECTORY.getString();

		File targetDirectory = null;
		try {
			targetDirectory = new File(targetPath).getCanonicalFile();
		} catch (IOException e) {
			throw new CMSMFException(String.format("Failed to canonicalize the path [%s]", targetPath), e);
		}

		// Replace variables in the URL
		jdbcUrl = StrSubstitutor
			.replace(jdbcUrl, Collections.singletonMap("target", targetDirectory.getAbsolutePath()));
		if (DataStore.LOG.isInfoEnabled()) {
			DataStore.LOG.info(String.format("State database will be stored at [%s]", jdbcUrl));
		}
		final ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(jdbcUrl, null);
		final ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<PoolableConnection>();
		@SuppressWarnings("unused")
		final PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,
			connectionPool, null, null, false, true);
		DataStore.DATA_SOURCE = new PoolingDataSource(connectionPool);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					if (DataStore.LOG.isInfoEnabled()) {
						DataStore.LOG.info("Closing the state database connection pool");
					}
					connectionPool.close();
				} catch (Exception e) {
					DataStore.LOG.warn("Failed to close the JDBC connection pool", e);
				}
			}
		});

		Connection c = null;
		try {
			c = DataStore.DATA_SOURCE.getConnection();
		} catch (SQLException e) {
			throw new CMSMFException("Failed to get a SQL Connection to validate the schema", e);
		}
		boolean ok = false;
		try {
			Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
			Liquibase liquibase = new Liquibase("db.changelog.xml", new ClassLoaderResourceAccessor(), database);
			if (clearData) {
				DatabaseMetaData dmd = c.getMetaData();
				ResultSet rs = null;
				try {
					rs = dmd.getTables(null, null, "DCTM_OBJECT", new String[] {
						"TABLE"
					});
					QueryRunner qr = new QueryRunner();
					if (rs.next()) {
						qr.update(c, "delete from dctm_object");
					}
				} finally {
					DbUtils.closeQuietly(rs);
				}
			}
			liquibase.update((String) null);
			ok = true;
		} catch (DatabaseException e) {
			throw new CMSMFException("Failed to generate the SQL schema", e);
		} catch (LiquibaseException e) {
			throw new CMSMFException("Failed to generate the SQL schema", e);
		} catch (SQLException e) {
			throw new CMSMFException("Failed to generate the SQL schema", e);
		} finally {
			if (ok) {
				DbUtils.commitAndCloseQuietly(c);
			} else {
				DbUtils.rollbackAndCloseQuietly(c);
			}
		}
	}

	private static final ThreadLocal<QueryRunner> QUERY_RUNNER = new ThreadLocal<QueryRunner>();

	private static QueryRunner getQueryRunner() {
		QueryRunner q = DataStore.QUERY_RUNNER.get();
		if (q == null) {
			q = new QueryRunner();
			DataStore.QUERY_RUNNER.set(q);
		}
		return q;
	}

	private static String calculateContentPath(String objectId) {
		return null;
	}

	public static boolean serializeObject(IDfPersistentObject object) throws SQLException, DfException {
		return DataStore.serializeObject(object, (String) null, DataStore.NULL_PROPERTY_READER);
	}

	public static boolean serializeObject(IDfPersistentObject object, PropertyReader propertyReader)
		throws SQLException, DfException {
		return DataStore.serializeObject(object, (String) null, propertyReader);
	}

	public static boolean serializeObject(IDfPersistentObject object, IDfId dependentId) throws SQLException,
		DfException {
		return DataStore.serializeObject(object, (dependentId == null ? null : dependentId.getId()),
			DataStore.NULL_PROPERTY_READER);
	}

	public static boolean serializeObject(IDfPersistentObject object, IDfId dependentId, PropertyReader propertyReader)
		throws SQLException, DfException {
		return DataStore.serializeObject(object, (dependentId == null ? null : dependentId.getId()), propertyReader);
	}

	public static boolean serializeObject(IDfPersistentObject object, String dependentId) throws SQLException,
	DfException {
		return DataStore.serializeObject(object, dependentId, DataStore.NULL_PROPERTY_READER);
	}

	public static boolean serializeObject(IDfPersistentObject object, String dependentId, PropertyReader propertyReader)
		throws SQLException, DfException {
		if (propertyReader == null) {
			// Safeguard...
			propertyReader = DataStore.NULL_PROPERTY_READER;
		}

		// First...is the object
		// Put the object and its attributes into the state database
		boolean ok = false;
		// First, make sure no "left behind" garbage gets committed
		final String objectId = object.getObjectId().getId();
		final String objectType = object.getType().getName();
		final boolean hasContent = false; // TODO: Calculate
		final String contentPath = DataStore.calculateContentPath(objectId); // TODO: Calculate

		Connection c = DataStore.DATA_SOURCE.getConnection();
		c.rollback();
		c.setAutoCommit(false);
		try {
			QueryRunner qr = DataStore.getQueryRunner();
			if (qr.query(c, DataStore.CHECK_IF_OBJECT_EXISTS_SQL, DataStore.HANDLER_EXISTS, objectId)) {
				// Object is already there, so do nothing
				return false;
			}

			// Not there, insert the actual object
			qr.insert(c, DataStore.INSERT_OBJECT_SQL, DataStore.HANDLER_NULL, objectId, objectType, hasContent,
				contentPath);

			if (dependentId != null) {
				qr.insert(c, DataStore.INSERT_DEPENDENCY_SQL, DataStore.HANDLER_NULL, dependentId, objectId);
			}

			// Then, insert its attributes
			// TODO: Add support for a "custom attribute extractor" which will calculate "extra"
			// attributes to pull from the object and add them to the parameter matrix
			final int attCount = object.getAttrCount();
			Object[] attData = new Object[9];
			Object[] attValue = new Object[5];
			attData[0] = objectId; // This should never change within the loop
			attValue[0] = objectId; // This should never change within the loop
			for (int i = 0; i < attCount; i++) {
				final IDfAttr att = object.getAttr(i);
				final String name = att.getName();
				// If this attribute is to be handled as a property, then we
				// avoid storing it so it won't cause problems later on
				if (propertyReader.handleAsProperty(name)) {
					continue;
				}
				final boolean repeating = att.isRepeating();
				final int type = att.getDataType();
				final DataType cvt = DataType.fromDfConstant(type);

				// DO NOT process "undefined" attribute values
				if (cvt == DataType.DF_UNDEFINED) {
					DataStore.LOG.warn(String.format("Ignoring attribute of type UNDEFINED [{%s}.%s]", objectId, name));
					continue;
				}

				attData[1] = name;
				attData[2] = att.getId();
				attData[3] = cvt.name();
				attData[4] = att.getLength();
				attData[5] = false; // TODO: is_internal?
				attData[6] = att.isQualifiable();
				attData[7] = repeating;

				// Insert the attribute
				qr.insert(c, DataStore.INSERT_ATTRIBUTE_SQL, DataStore.HANDLER_NULL, attData);

				attValue[1] = name; // This never changes inside this next loop

				// TODO: Here we should intercept and determine if the attribute requires
				// special treatment

				// No special treatment, simply dump out all the values
				Object[][] values = new Object[object.getValueCount(name)][];
				for (int v = 0; v < values.length; v++) {
					attValue[2] = v;
					IDfValue value = object.getRepeatingValue(name, v);
					attValue[3] = ((value == null) || (value.asString() == null));
					attValue[4] = cvt.encode(value);
					values[v] = attValue.clone();
				}
				// Insert the values, as a batch
				qr.insertBatch(c, DataStore.INSERT_ATTRIBUTE_VALUE_SQL, DataStore.HANDLER_NULL, values);
			}

			Object[] propData = new Object[3];
			propData[0] = objectId;
			Collection<DataProperty> properties = propertyReader.readProperties(object);
			if ((properties != null) && !properties.isEmpty()) {
				for (final DataProperty prop : properties) {
					final String name = prop.getName();
					final DataType type = prop.getType();

					// DO NOT process "undefined" attribute values
					if (type == DataType.DF_UNDEFINED) {
						DataStore.LOG.warn(String.format("Ignoring property of type UNDEFINED [{%s}.%s]", objectId,
							name));
						continue;
					}

					propData[1] = name;
					propData[2] = type.name();

					// Insert the property
					qr.insert(c, DataStore.INSERT_PROPERTY_SQL, DataStore.HANDLER_NULL, propData);

					attValue[1] = name; // This never changes inside this next loop

					Object[][] values = new Object[prop.getValueCount()][];
					int v = 0;
					for (IDfValue value : prop) {
						attValue[2] = v;
						attValue[3] = ((value == null) || (value.asString() == null));
						attValue[4] = type.encode(value);
						values[v] = attValue.clone();
						v++;
					}
					// Insert the values, as a batch
					qr.insertBatch(c, DataStore.INSERT_PROPERTY_VALUE_SQL, DataStore.HANDLER_NULL, values);
				}
			}
			ok = true;
			return true;
		} finally {
			if (ok) {
				if (DataStore.LOG.isDebugEnabled()) {
					DataStore.LOG.debug(String.format("Committing insert transaction for [%s::%s]", objectId,
						dependentId));
				}
				DbUtils.commitAndClose(c);
			} else {
				DataStore.LOG
					.warn(String.format("Rolling back insert transaction for [%s::%s]", objectId, dependentId));
				DbUtils.rollbackAndClose(c);
			}
		}
	}

	public static boolean serializeObject(DataObject object) throws SQLException, DfException {
		// First...is the object
		// Put the object and its attributes into the state database
		boolean ok = false;

		// First, make sure no "left behind" garbage gets committed
		final String objectId = object.getId();
		final DctmObjectType objectType = object.getType();
		final boolean hasContent = object.isContentHolder();
		final String contentPath = object.getContentPath();

		Connection c = DataStore.DATA_SOURCE.getConnection();
		c.rollback();
		c.setAutoCommit(false);
		try {
			QueryRunner qr = DataStore.getQueryRunner();
			if (qr.query(c, DataStore.CHECK_IF_OBJECT_EXISTS_SQL, DataStore.HANDLER_EXISTS, objectId)) {
				// Object is already there, so do nothing
				return false;
			}

			// Not there, insert the actual object
			qr.insert(c, DataStore.INSERT_OBJECT_SQL, DataStore.HANDLER_NULL, objectId, objectType.name(), hasContent,
				contentPath);

			// Then, insert its attributes
			Object[] attData = new Object[8];
			Object[] attValue = new Object[5];
			attData[0] = objectId; // This should never change within the loop
			attValue[0] = objectId; // This should never change within the loop
			for (final DataAttribute attribute : object) {
				final String name = attribute.getName();
				final boolean repeating = attribute.isRepeating();
				final DataType type = attribute.getType();

				// DO NOT process "undefined" attribute values
				if (type == DataType.DF_UNDEFINED) {
					DataStore.LOG.warn(String.format("Ignoring attribute of type UNDEFINED [{%s}.%s]", objectId, name));
					continue;
				}

				attData[1] = name;
				attData[2] = attribute.getId();
				attData[3] = type.name();
				attData[4] = attribute.getLength();
				attData[5] = false; // TODO: is_internal?
				attData[6] = attribute.isQualifiable();
				attData[7] = repeating;

				// Insert the attribute
				qr.insert(c, DataStore.INSERT_ATTRIBUTE_SQL, DataStore.HANDLER_NULL, attData);

				attValue[1] = name; // This never changes inside this next loop
				Object[][] values = new Object[attribute.getValueCount()][];
				int v = 0;
				// No special treatment, simply dump out all the values
				for (IDfValue value : attribute) {
					attValue[2] = v;
					attValue[3] = ((value == null) || (value.asString() == null));
					attValue[4] = type.encode(value);
					values[v] = attValue.clone();
					v++;
				}
				// Insert the values, as a batch
				qr.insertBatch(c, DataStore.INSERT_ATTRIBUTE_VALUE_SQL, DataStore.HANDLER_NULL, values);
			}

			// Then, the properties
			Object[] propData = new Object[3];
			propData[0] = objectId; // This should never change within the loop
			for (final String name : object.getPropertyNames()) {
				final DataProperty property = object.getProperty(name);
				final DataType type = property.getType();

				// DO NOT process "undefined" property values
				if (type == DataType.DF_UNDEFINED) {
					DataStore.LOG.warn(String.format("Ignoring property of type UNDEFINED [{%s}.%s]", objectId, name));
					continue;
				}

				propData[1] = name;
				propData[2] = type.name();

				// Insert the attribute
				qr.insert(c, DataStore.INSERT_PROPERTY_SQL, DataStore.HANDLER_NULL, propData);

				attValue[1] = name; // This never changes inside this next loop
				Object[][] values = new Object[property.getValueCount()][];
				int v = 0;
				// No special treatment, simply dump out all the values
				for (IDfValue value : property) {
					attValue[2] = v;
					attValue[3] = ((value == null) || (value.asString() == null));
					attValue[4] = type.encode(value);
					values[v] = attValue.clone();
					v++;
				}
				// Insert the values, as a batch
				qr.insertBatch(c, DataStore.INSERT_PROPERTY_VALUE_SQL, DataStore.HANDLER_NULL, values);
			}
			ok = true;
			return true;
		} finally {
			if (ok) {
				if (DataStore.LOG.isDebugEnabled()) {
					DataStore.LOG.debug(String.format("Committing insert transaction for [%s]", objectId));
				}
				DbUtils.commitAndClose(c);
			} else {
				DataStore.LOG.warn(String.format("Rolling back insert transaction for [%s]", objectId));
				DbUtils.rollbackAndClose(c);
			}
		}
	}

	public static void deserializeObjects(DctmObjectType type, ImportHandler handler) throws SQLException,
	CMSMFException {
		Connection objConn = null;
		Connection attConn = null;
		Connection valConn = null;

		try {
			objConn = DataStore.DATA_SOURCE.getConnection();
			attConn = DataStore.DATA_SOURCE.getConnection();
			valConn = DataStore.DATA_SOURCE.getConnection();

			PreparedStatement objPS = null;
			PreparedStatement attPS = null;
			PreparedStatement valPS = null;
			try {
				objPS = objConn.prepareStatement(DataStore.LOAD_OBJECTS_SQL);
				attPS = attConn.prepareStatement(DataStore.LOAD_ATTRIBUTES_SQL);
				valPS = valConn.prepareStatement(DataStore.LOAD_VALUES_SQL);

				ResultSet objRS = null;
				ResultSet attRS = null;
				ResultSet valRS = null;

				objPS.setString(1, type.name());
				objRS = objPS.executeQuery();
				try {
					while (objRS.next()) {
						final int objNum = objRS.getInt("object_number");
						DataObject obj = new DataObject(objRS);
						if (DataStore.LOG.isTraceEnabled()) {
							DataStore.LOG.trace(String.format("De-serialized %s object #%d: %s", type, objNum, obj));
						} else if (DataStore.LOG.isDebugEnabled()) {
							DataStore.LOG.trace(String.format("De-serialized %s object #%d with ID [%s]", type, objNum,
								obj.getId()));
						}

						attPS.clearParameters();
						attPS.setString(1, obj.getId());
						attRS = attPS.executeQuery();
						try {
							obj.loadAttributes(attRS);
						} finally {
							DbUtils.closeQuietly(attRS);
						}

						valPS.clearParameters();
						valPS.setString(1, obj.getId());
						for (DataAttribute att : obj) {
							valPS.setString(2, att.getName());
							valRS = valPS.executeQuery();
							try {
								att.loadValues(valRS);
							} finally {
								DbUtils.closeQuietly(valRS);
							}
						}

						try {
							if (!handler.handle(obj)) {
								break;
							}
						} catch (Exception e) {
							throw new CMSMFException(String.format(
								"Failed to properly import %s object with ID [%s] (#%d)", type, obj.getId(), objNum), e);
						}
					}
				} finally {
					DbUtils.closeQuietly(objRS);
				}
			} finally {
				DbUtils.closeQuietly(valPS);
				DbUtils.closeQuietly(attPS);
				DbUtils.closeQuietly(objPS);
			}
		} finally {
			DbUtils.rollbackAndCloseQuietly(valConn);
			DbUtils.rollbackAndCloseQuietly(attConn);
			DbUtils.rollbackAndCloseQuietly(objConn);
		}

		/*
		final QueryRunner qr = new QueryRunner(DataStore.DATA_SOURCE);
		qr.query(DataStore.LOAD_OBJECTS_SQL, new ResultSetHandler<Void>() {
			@Override
			public Void handle(ResultSet objectRs) throws SQLException {
				while (objectRs.next()) {
					final DataObject obj = new DataObject(objectRs);
					qr.query(DataStore.LOAD_ATTRIBUTES_SQL, new ResultSetHandler<Void>() {
						@Override
						public Void handle(ResultSet attributeRs) throws SQLException {
							obj.loadAttributes(attributeRs);
							for (final DataAttribute attribute : obj) {
								qr.query(DataStore.LOAD_VALUES_SQL, new ResultSetHandler<Void>() {
									@Override
									public Void handle(ResultSet valueRs) throws SQLException {
										attribute.loadValues(valueRs);
										return null;
									}
								}, obj.getId(), attribute.getName());
							}
							return null;
						}
					}, obj.getId());
				}
				return null;
			}
		}, type.name());
		 */
	}

	/**
	 * <p>
	 * Assigns the given targetId as the new ID for the object with the given source ID
	 * </p>
	 *
	 * @param sourceId
	 * @throws SQLException
	 */
	public static void setIdMapping(String sourceId, String targetId) throws SQLException {
		final QueryRunner qr = new QueryRunner(DataStore.DATA_SOURCE);
		qr.insert(DataStore.INSERT_MAPPING_SQL, DataStore.HANDLER_NULL, sourceId, targetId);
	}

	/**
	 * <p>
	 * Removes any ID mappings for the given source object ID.
	 * </p>
	 *
	 * @param sourceId
	 * @throws SQLException
	 */
	public static void clearIdMapping(String sourceId) throws SQLException {
		final QueryRunner qr = new QueryRunner(DataStore.DATA_SOURCE);
		qr.update(DataStore.DELETE_MAPPING_SQL, DataStore.HANDLER_NULL, sourceId);
	}

	private static String getMappedId(boolean source, String id) throws SQLException {
		if (id == null) { throw new IllegalArgumentException("Must provide a valid ID to search against"); }
		final String sql = (source ? DataStore.FIND_SOURCE_ID_SQL : DataStore.FIND_TARGET_ID_SQL);
		final QueryRunner qr = new QueryRunner(DataStore.DATA_SOURCE);
		ResultSetHandler<String> h = new ResultSetHandler<String>() {
			@Override
			public String handle(ResultSet rs) throws SQLException {
				if (!rs.next()) { return null; }
				return rs.getString(1);
			}
		};
		return qr.query(sql, h, id);
	}

	/**
	 * <p>
	 * Retrieves the target ID for the object with the given source ID
	 * </p>
	 * <p>
	 * In particular, for a given target ID {@code tgtId} that has already been mapped to a source
	 * ID, the invocation {@code getTargetId(getSourceId(tgtId)).equals(tgtId)} <b><i>must</i></b>
	 * return {@code true}.
	 * </p>
	 *
	 * @param sourceId
	 * @throws SQLException
	 */
	public static String getTargetId(String sourceId) throws SQLException {
		return DataStore.getMappedId(false, sourceId);
	}

	/**
	 * <p>
	 * Retrieves the source ID for the object with the given target ID.
	 * </p>
	 * <p>
	 * In particular, for a given source ID {@code srcId} that has already been mapped to a target
	 * ID, the invocation {@code getSourceId(getTargetId(srcId)).equals(srcId)} <b><i>must</i></b>
	 * return {@code true}.
	 * </p>
	 *
	 * @param targetId
	 * @throws SQLException
	 */
	public static String getSourceId(String targetId) throws SQLException {
		return DataStore.getMappedId(true, targetId);
	}
}