/**
 *
 */

package com.delta.cmsmf.cms.storage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

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
import org.apache.log4j.Logger;

import com.delta.cmsmf.cms.CmsAttribute;
import com.delta.cmsmf.cms.CmsDataType;
import com.delta.cmsmf.cms.CmsDependencyManager;
import com.delta.cmsmf.cms.CmsObject;
import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.cms.CmsProperty;
import com.delta.cmsmf.cms.UnsupportedObjectTypeException;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsObjectStore extends CmsDependencyManager {

	public static interface ObjectHandler {
		public boolean handle(CmsObject<?> dataObject) throws CMSMFException;
	}

	public static interface DependencyHandler {
		public boolean handle(CmsObjectType type, String id) throws CMSMFException;
	}

	private static final Object[][] NO_PARAMS = new Object[0][0];

	private static final String CHECK_IF_OBJECT_EXISTS_SQL = "select object_id from dctm_object where object_id = ?";

	private static final String INSERT_OBJECT_SQL = "insert into dctm_object (object_id, object_type) values (?, ?)";
	private static final String INSERT_ATTRIBUTE_SQL = "insert into dctm_attribute (object_id, name, id, data_type, length, qualifiable, repeating) values (?, ?, ?, ?, ?, ?, ?)";
	private static final String INSERT_ATTRIBUTE_VALUE_SQL = "insert into dctm_attribute_value (object_id, name, value_number, data) values (?, ?, ?, ?)";
	private static final String INSERT_PROPERTY_SQL = "insert into dctm_property (object_id, name, data_type, repeating) values (?, ?, ?, ?)";
	private static final String INSERT_PROPERTY_VALUE_SQL = "insert into dctm_property_value (object_id, name, value_number, data) values (?, ?, ?, ?)";

	private static final String QUERY_EXPORT_PLAN_DUPE_SQL = "select * from dctm_export_plan where object_id = ?";
	private static final String INSERT_EXPORT_PLAN_SQL = "insert into dctm_export_plan (object_type, object_id) values (?, ?)";
	private static final String MARK_EXPORT_PLAN_TRAVERSED_SQL = "update dctm_export_plan set traversed = true where object_id = ? and traversed = false";

	private static final String FIND_TARGET_MAPPING_SQL = "select target_value from dctm_mapper where object_type = ? and name = ? and source_value = ?";
	private static final String FIND_SOURCE_MAPPING_SQL = "select source_value from dctm_mapper where object_type = ? and name = ? and target_value = ?";
	private static final String INSERT_MAPPING_SQL = "insert into dctm_mapper (object_type, name, source_value, target_value) values (?, ?, ?, ?)";
	private static final String DELETE_TARGET_MAPPING_SQL = "delete from dctm_mapper where object_type = ? and name = ? and source_value = ?";
	private static final String DELETE_SOURCE_MAPPING_SQL = "delete from dctm_mapper where object_type = ? and name = ? and target_value = ?";

	private static final String LOAD_OBJECTS_SQL = //
		"    select * " + //
		"  from dctm_object " + //
		" where object_type = ? " + //
		" order by object_number";

	private static final String LOAD_ATTRIBUTES_SQL = //
		"    select * " + //
		"  from dctm_attribute " + //
		" where object_id = ? " + //
		" order by name";

	private static final String LOAD_ATTRIBUTE_VALUES_SQL = //
		"    select * " + //
		"  from dctm_attribute_value " + //
		" where object_id = ? " + //
		"   and name = ? " + //
		" order by value_number";

	private static final String LOAD_PROPERTIES_SQL = //
		"    select * " + //
		"  from dctm_property " + //
		" where object_id = ? " + //
		" order by name";

	private static final String LOAD_PROPERTY_VALUES_SQL = //
		"    select * " + //
		"  from dctm_property_value " + //
		" where object_id = ? " + //
		"   and name = ? " + //
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

	protected final Logger log = Logger.getLogger(getClass());

	private final DataSource dataSource;

	public CmsObjectStore(DataSource dataSource, boolean clearData) throws CMSMFException {
		this.dataSource = dataSource;

		Connection c = null;
		try {
			c = this.dataSource.getConnection();
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
					rs = dmd.getTables(null, null, "DCTM_%", new String[] {
						"TABLE"
					});
					QueryRunner qr = new QueryRunner(this.dataSource);
					while (rs.next()) {
						String tableName = rs.getString("TABLE_NAME");
						qr.update(String.format("delete from %s", tableName));
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
		QueryRunner q = CmsObjectStore.QUERY_RUNNER.get();
		if (q == null) {
			q = new QueryRunner();
			CmsObjectStore.QUERY_RUNNER.set(q);
		}
		return q;
	}

	private boolean serializeObject(Connection c, CmsObject<?> object) throws DfException, CMSMFException {
		final String objectId = object.getId();
		final CmsObjectType objectType = object.getType();
		Collection<Object[]> attributeParameters = new ArrayList<Object[]>();
		Collection<Object[]> attributeValueParameters = new ArrayList<Object[]>();
		Collection<Object[]> propertyParameters = new ArrayList<Object[]>();
		Collection<Object[]> propertyValueParameters = new ArrayList<Object[]>();
		Object[] attData = new Object[7];
		Object[] attValue = new Object[4];
		Object[] propData = new Object[4];

		PreparedStatement lockPS = null;
		ResultSet lockRS = null;
		try {
			QueryRunner qr = CmsObjectStore.getQueryRunner();
			if (isSerialized(c, objectId)) {
				// Object is already there, so do nothing
				return false;
			}
			persistDependency(c, new Dependency(object));
			final String sql = "select * from dctm_export_plan where traversed = false and object_id = ? and not exists ( select o.object_id from dctm_object o where o.object_id = dctm_export_plan.object_id ) for update";
			lockPS = c.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			lockPS.setString(1, objectId);
			lockRS = lockPS.executeQuery();
			if (!lockRS.next()) {
				// Already serialized...
				return false;
			}
			// TODO: Attempt to lock the object ID for the rest of the import, to
			// manage the race condition of two threads attempting to commit the same object

			// Then, insert its attributes
			attData[0] = objectId; // This should never change within the loop
			attValue[0] = objectId; // This should never change within the loop
			for (final CmsAttribute attribute : object.getAllAttributes()) {
				final String name = attribute.getName();
				final boolean repeating = attribute.isRepeating();
				final CmsDataType type = attribute.getType();

				// DO NOT process "undefined" attribute values
				if (type == CmsDataType.DF_UNDEFINED) {
					this.log.warn(String.format("Ignoring attribute of type UNDEFINED [{%s}.%s]", objectId, name));
					continue;
				}

				attData[1] = name;
				attData[2] = attribute.getId();
				attData[3] = type.name();
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
				for (IDfValue value : attribute) {
					attValue[2] = v;
					attValue[3] = type.encode(value);
					values[v] = attValue.clone();
					attributeValueParameters.add(attValue.clone());
					v++;
				}
			}

			// Then, the properties
			propData[0] = objectId; // This should never change within the loop
			for (final String name : object.getPropertyNames()) {
				final CmsProperty property = object.getProperty(name);
				final CmsDataType type = property.getType();

				// DO NOT process "undefined" property values
				if (type == CmsDataType.DF_UNDEFINED) {
					this.log.warn(String.format("Ignoring property of type UNDEFINED [{%s}.%s]", objectId, name));
					continue;
				}

				propData[1] = name;
				propData[2] = type.name();
				propData[3] = property.isRepeating();

				// Insert the attribute
				propertyParameters.add(propData.clone());

				attValue[1] = name; // This never changes inside this next loop
				Object[][] values = new Object[property.getValueCount()][];
				int v = 0;
				// No special treatment, simply dump out all the values
				for (IDfValue value : property) {
					attValue[2] = v;
					attValue[3] = type.encode(value);
					values[v] = attValue.clone();
					propertyValueParameters.add(attValue.clone());
					v++;
				}
			}

			// Do all the inserts in a row
			qr.insert(c, CmsObjectStore.INSERT_OBJECT_SQL, CmsObjectStore.HANDLER_NULL, objectId, objectType.name());
			qr.insertBatch(c, CmsObjectStore.INSERT_ATTRIBUTE_SQL, CmsObjectStore.HANDLER_NULL,
				attributeParameters.toArray(CmsObjectStore.NO_PARAMS));
			qr.insertBatch(c, CmsObjectStore.INSERT_ATTRIBUTE_VALUE_SQL, CmsObjectStore.HANDLER_NULL,
				attributeValueParameters.toArray(CmsObjectStore.NO_PARAMS));
			qr.insertBatch(c, CmsObjectStore.INSERT_PROPERTY_SQL, CmsObjectStore.HANDLER_NULL,
				propertyParameters.toArray(CmsObjectStore.NO_PARAMS));
			qr.insertBatch(c, CmsObjectStore.INSERT_PROPERTY_VALUE_SQL, CmsObjectStore.HANDLER_NULL,
				propertyValueParameters.toArray(CmsObjectStore.NO_PARAMS));
			// lockRS.updateBoolean(1, true);
			// lockRS.updateRow();
			return true;
		} catch (SQLException e) {
			throw new RuntimeException(String.format("Failed to serialize %s", object), e);
		} finally {
			DbUtils.closeQuietly(lockRS);
			DbUtils.closeQuietly(lockPS);
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

	public boolean serializeObject(CmsObject<?> object) throws DfException, CMSMFException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to serialize"); }
		boolean ok = false;
		try {
			Connection c = this.dataSource.getConnection();
			// First, make sure no "left behind" garbage gets committed
			c.rollback();
			c.setAutoCommit(false);
			try {
				boolean ret = serializeObject(c, object);
				ok = true;
				return ret;
			} finally {
				if (ok) {
					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("Committing insert transaction for [%s]", object.getId()));
					}
					DbUtils.commitAndClose(c);
				} else {
					this.log.warn(String.format("Rolling back insert transaction for [%s]", object.getId()));
					DbUtils.rollbackAndClose(c);
				}
			}
		} catch (SQLException e) {
			throw new CMSMFException(String.format("Failed to serialize %s", object), e);
		}
	}

	public void deserializeObjects(CmsObjectType type, ObjectHandler handler) throws SQLException, CMSMFException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to deserialize"); }
		if (handler == null) { throw new IllegalArgumentException(
			"Must provide an object handler to handle the deserialized objects"); }
		Connection objConn = null;
		Connection attConn = null;

		try {
			objConn = this.dataSource.getConnection();
			attConn = this.dataSource.getConnection();

			PreparedStatement objPS = null;
			PreparedStatement attPS = null;
			PreparedStatement valPS = null;
			PreparedStatement propPS = null;
			PreparedStatement pvalPS = null;
			try {
				objPS = objConn.prepareStatement(CmsObjectStore.LOAD_OBJECTS_SQL);
				attPS = attConn.prepareStatement(CmsObjectStore.LOAD_ATTRIBUTES_SQL);
				valPS = attConn.prepareStatement(CmsObjectStore.LOAD_ATTRIBUTE_VALUES_SQL);
				propPS = attConn.prepareStatement(CmsObjectStore.LOAD_PROPERTIES_SQL);
				pvalPS = attConn.prepareStatement(CmsObjectStore.LOAD_PROPERTY_VALUES_SQL);

				ResultSet objRS = null;
				ResultSet attRS = null;
				ResultSet propRS = null;
				ResultSet valRS = null;

				objPS.setString(1, type.name());
				objRS = objPS.executeQuery();
				try {
					while (objRS.next()) {
						final int objNum = objRS.getInt("object_number");
						final CmsObjectType objType = CmsObjectType.valueOf(objRS.getString("object_type"));
						final String objId = objRS.getString("object_id");
						if (this.log.isInfoEnabled()) {
							this.log.info(String.format("De-serializing %s object #%d [id=%s]", type, objNum, objId));
						}
						CmsObject<?> obj = objType.newInstance();
						obj.load(objRS);
						if (this.log.isTraceEnabled()) {
							this.log.trace(String.format("De-serialized %s object #%d: %s", type, objNum, obj));
						} else if (this.log.isDebugEnabled()) {
							this.log.debug(String.format("De-serialized %s object #%d with ID [%s]", type, objNum,
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
						for (CmsAttribute att : obj.getAllAttributes()) {
							valPS.setString(2, att.getName());
							valRS = valPS.executeQuery();
							try {
								att.loadValues(valRS);
							} finally {
								DbUtils.closeQuietly(valRS);
							}
						}

						propPS.clearParameters();
						propPS.setString(1, obj.getId());
						propRS = propPS.executeQuery();
						try {
							obj.loadProperties(propRS);
						} finally {
							DbUtils.closeQuietly(propRS);
						}

						pvalPS.clearParameters();
						pvalPS.setString(1, obj.getId());
						for (CmsProperty prop : obj.getAllProperties()) {
							pvalPS.setString(2, prop.getName());
							valRS = pvalPS.executeQuery();
							try {
								prop.loadValues(valRS);
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
				DbUtils.closeQuietly(pvalPS);
				DbUtils.closeQuietly(propPS);
				DbUtils.closeQuietly(valPS);
				DbUtils.closeQuietly(attPS);
				DbUtils.closeQuietly(objPS);
			}
		} finally {
			DbUtils.rollbackAndCloseQuietly(attConn);
			DbUtils.rollbackAndCloseQuietly(objConn);
		}
	}

	/**
	 * <p>
	 * Assigns the given targetId as the new ID for the object with the given source ID
	 * </p>
	 *
	 */
	public void setMapping(CmsObjectType type, String name, String sourceValue, String targetValue) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if ((sourceValue == null) && (targetValue == null)) { throw new IllegalArgumentException(
			"Must provide a source or target value to search against"); }
		final QueryRunner qr = new QueryRunner(this.dataSource);
		if ((targetValue == null) || (sourceValue == null)) {
			// Delete instead
			final String sql = (targetValue == null ? CmsObjectStore.DELETE_TARGET_MAPPING_SQL
				: CmsObjectStore.DELETE_SOURCE_MAPPING_SQL);
			final String refValue = (targetValue == null ? sourceValue : targetValue);
			try {
				qr.update(sql, type.name(), name, refValue);
			} catch (SQLException e) {
				final String refType = (targetValue == null ? "source" : "target");
				throw new RuntimeException(String.format("Failed to clear the %s mapping for [%s/%s/%s]", refType,
					type, name, refValue), e);
			}
		} else {
			// TODO: Support updating the mapping
			try {
				qr.insert(CmsObjectStore.INSERT_MAPPING_SQL, CmsObjectStore.HANDLER_NULL, type.name(), name,
					sourceValue, targetValue);
			} catch (SQLException e) {
				throw new RuntimeException(String.format("Failed to insert the mapping for [%s/%s/%s->%s]", type, name,
					sourceValue, targetValue), e);
			}
		}
	}

	/**
	 * <p>
	 * Removes any source mappings for the given source object type, mapping name, with the given
	 * source value.
	 * </p>
	 *
	 * @param type
	 * @param name
	 * @param sourceValue
	 */
	public void clearTargetMapping(CmsObjectType type, String name, String sourceValue) {
		setMapping(type, name, sourceValue, null);
	}

	public void clearSourceMapping(CmsObjectType type, String name, String targetValue) {
		setMapping(type, name, null, targetValue);
	}

	private String getMappedValue(boolean source, CmsObjectType type, String name, String value) {
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
		final String sql = (source ? CmsObjectStore.FIND_TARGET_MAPPING_SQL : CmsObjectStore.FIND_SOURCE_MAPPING_SQL);
		try {
			return qr.query(sql, h, type.name(), name, value);
		} catch (SQLException e) {
			throw new RuntimeException(String.format("Failed to retrieve the %s mapping for [%s/%s/%s]",
				source ? "source" : "target", type, name, value), e);
		}
	}

	/**
	 * <p>
	 * Retrieves the target value for the mapping with the given object type, mapping name and
	 * source value.
	 * </p>
	 *
	 * @param type
	 * @param name
	 * @param sourceValue
	 */
	public String getTargetMapping(CmsObjectType type, String name, String sourceValue) {
		return getMappedValue(true, type, name, sourceValue);
	}

	/**
	 * <p>
	 * Retrieves the source value for the mapping with the given object type, mapping name and
	 * target value.
	 * </p>
	 *
	 * @param type
	 * @param name
	 * @param targetValue
	 */
	public String getSourceMapping(CmsObjectType type, String name, String targetValue) {
		return getMappedValue(false, type, name, targetValue);
	}

	private boolean isSerialized(Connection c, String objectId) throws CMSMFException, SQLException {
		return CmsObjectStore.getQueryRunner().query(c, CmsObjectStore.CHECK_IF_OBJECT_EXISTS_SQL,
			CmsObjectStore.HANDLER_EXISTS, objectId);
	}

	public boolean isSerialized(String objectId) throws CMSMFException {
		final Connection c;
		try {
			c = this.dataSource.getConnection();
		} catch (SQLException e) {
			throw new CMSMFException("Failed to connecto to the object store's database", e);
		}
		try {
			c.setAutoCommit(false);
			return isSerialized(c, objectId);
		} catch (SQLException e) {
			throw new CMSMFException(String.format("Failed to check whether object [%s] was already serialized",
				objectId), e);
		} finally {
			DbUtils.rollbackAndCloseQuietly(c);
		}
	}

	private boolean persistDependency(Connection c, Dependency dependency) throws CMSMFException {
		QueryRunner qr = CmsObjectStore.getQueryRunner();
		try {
			if (qr.query(c, CmsObjectStore.QUERY_EXPORT_PLAN_DUPE_SQL, CmsObjectStore.HANDLER_EXISTS,
				dependency.getId())) {
				// Duplicate dependency...we skip it
				return false;
			}
			qr.insert(c, CmsObjectStore.INSERT_EXPORT_PLAN_SQL, CmsObjectStore.HANDLER_NULL, dependency.getType()
				.name(), dependency.getId());
			return true;
		} catch (SQLException e) {
			throw new CMSMFException(String.format("Failed to register the dependency [%s]", dependency), e);
		}
	}

	@Override
	protected boolean persistDependency(Dependency dependency) throws CMSMFException {
		final Connection c;
		try {
			c = this.dataSource.getConnection();
		} catch (SQLException e) {
			throw new CMSMFException("Failed to connecto to the object store's database", e);
		}
		boolean ok = false;
		try {
			c.setAutoCommit(false);
			boolean ret = persistDependency(c, dependency);
			ok = true;
			return ret;
		} catch (SQLException e) {
			throw new CMSMFException(String.format("Failed to register the dependency [%s]", dependency), e);
		} finally {
			if (ok) {
				DbUtils.commitAndCloseQuietly(c);
			} else {
				DbUtils.rollbackAndCloseQuietly(c);
			}
		}
	}

	@Override
	public Boolean persistDfObject(IDfPersistentObject dfObject) throws DfException, CMSMFException {
		Dependency dependency;
		try {
			dependency = new Dependency(dfObject);
		} catch (UnsupportedObjectTypeException e) {
			if (this.log.isDebugEnabled()) {
				this.log.warn(e.getMessage());
			}
			return false;
		}
		persistDependency(dependency);
		// If it's already serialized, we skip it
		if (isSerialized(dfObject.getObjectId().getId())) { return false; }

		// Not already serialized, so we do the deed.
		CmsObject<?> obj = dependency.getType().newInstance();
		obj.loadFromCMS(dfObject);
		// If somehow it got serialized underneath us (perhaps by another thread), we skip it
		if (!serializeObject(obj)) { return false; }
		// We try to traverse its dependencies
		obj.persistDependencies(dfObject, this);
		// markTraversed(obj.getId());
		return true;
	}

	private boolean markTraversed(Connection c, String objectId) throws CMSMFException {
		QueryRunner qr = CmsObjectStore.getQueryRunner();
		try {
			return (qr.update(c, CmsObjectStore.MARK_EXPORT_PLAN_TRAVERSED_SQL, objectId) == 1);
		} catch (SQLException e) {
			throw new CMSMFException(String.format("Failed to mark the dependency for [%s] as traversed", objectId), e);
		}
	}

	public boolean markTraversed(String objectId) throws CMSMFException {
		final Connection c;
		try {
			c = this.dataSource.getConnection();
		} catch (SQLException e) {
			throw new CMSMFException("Failed to connecto to the object store's database", e);
		}
		boolean ok = false;
		try {
			boolean ret = markTraversed(c, objectId);
			ok = true;
			return ret;
		} finally {
			if (ok) {
				DbUtils.commitAndCloseQuietly(c);
			} else {
				DbUtils.rollbackAndCloseQuietly(c);
			}
		}
	}
}