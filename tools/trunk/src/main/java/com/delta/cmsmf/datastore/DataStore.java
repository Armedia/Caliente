/**
 *
 */

package com.delta.cmsmf.datastore;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
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

import com.delta.cmsmf.cmsobjects.DctmObjectTypesEnum;
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

	private static final String CHECK_IF_OBJECT_EXISTS_SQL = "select object_id from dctm_object where object_id = ?";

	private static final String INSERT_OBJECT_SQL = "insert into dctm_object (object_id, object_type, has_content, content_path) values (?, ?, ?, ?)";
	private static final String INSERT_DEPENDENCY_SQL = "insert into dctm_dependency (object_id, dependency_id) values (?, ?)";
	private static final String INSERT_ATTRIBUTE_SQL = "insert into dctm_attribute (object_id, attribute_name, attribute_id, attribute_type, attribute_length, is_internal, is_qualifiable, is_repeating) values (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String INSERT_ATTRIBUTE_VALUE_SQL = "insert into dctm_attribute_value (object_id, attribute_name, value_number, is_null, data) values (?, ?, ?, ?, ?)";

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
		return DataStore.serializeObject(object, (String) null);
	}

	public static boolean serializeObject(IDfPersistentObject object, IDfId dependentId) throws SQLException,
		DfException {
		return DataStore.serializeObject(object, (dependentId == null ? null : dependentId.getId()));
	}

	public static boolean serializeObject(IDfPersistentObject object, String dependentId) throws SQLException,
		DfException {
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
				attData[3] = type;
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
		final DctmObjectTypesEnum objectType = object.getType();
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
			for (DataAttribute attribute : object) {
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
}