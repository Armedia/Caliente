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
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

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

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cms.CmsAttribute;
import com.delta.cmsmf.cms.CmsAttributeMapper;
import com.delta.cmsmf.cms.CmsAttributeMapper.Mapping;
import com.delta.cmsmf.cms.CmsDataType;
import com.delta.cmsmf.cms.CmsDependencyManager;
import com.delta.cmsmf.cms.CmsExportContext;
import com.delta.cmsmf.cms.CmsObject;
import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.cms.CmsProperty;
import com.delta.cmsmf.cms.CmsTransferContext;
import com.delta.cmsmf.cms.UnsupportedObjectTypeException;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmsObjectStore {

	public static interface ObjectHandler {

		/**
		 * <p>
		 * Signal the beginning of a new batch, with the given ID. Returns {@code true} if the batch
		 * should be processed, {@code false} if it should be skipped. If the batch is skipped,
		 * Neither {@link #closeBatch(boolean)} nor {@link #handle(CmsObject)} will be invoked.
		 * </p>
		 *
		 * @param batchId
		 * @return {@code true} if the batch should be processed, {@code false} if it should be
		 *         skipped
		 * @throws CMSMFException
		 */
		public boolean newBatch(String batchId) throws CMSMFException;

		/**
		 * <p>
		 * Handle the given object instance in the context of the currently-open batch
		 * </p>
		 *
		 * @param dataObject
		 * @throws CMSMFException
		 */
		public void handle(CmsObject<?> dataObject) throws CMSMFException;

		/**
		 * <p>
		 * Close the current batch, returning {@code true} if processing should continue with the
		 * next batch, or {@code false} otherwise.
		 * </p>
		 *
		 * @param ok
		 *            {@code true} if processing should continue with the next batch, or
		 *            {@code false} otherwise
		 * @return {@code true} if processing should continue with the next batch, or {@code false}
		 *         otherwise
		 * @throws CMSMFException
		 */
		public boolean closeBatch(boolean ok) throws CMSMFException;
	}

	private static final Object[][] NO_PARAMS = new Object[0][0];

	private static final String CHECK_IF_OBJECT_EXISTS_SQL = "select object_id from dctm_object where object_id = ?";

	private static final String INSERT_OBJECT_SQL = "insert into dctm_object (object_id, object_type, object_subtype, object_label, batch_id) values (?, ?, ?, ?, ?)";
	private static final String INSERT_ATTRIBUTE_SQL = "insert into dctm_attribute (object_id, name, id, data_type, length, qualifiable, repeating) values (?, ?, ?, ?, ?, ?, ?)";
	private static final String INSERT_ATTRIBUTE_VALUE_SQL = "insert into dctm_attribute_value (object_id, name, value_number, data) values (?, ?, ?, ?)";
	private static final String INSERT_PROPERTY_SQL = "insert into dctm_property (object_id, name, data_type, repeating) values (?, ?, ?, ?)";
	private static final String INSERT_PROPERTY_VALUE_SQL = "insert into dctm_property_value (object_id, name, value_number, data) values (?, ?, ?, ?)";

	private static final String QUERY_EXPORT_PLAN_DUPE_SQL = "select * from dctm_export_plan where object_id = ?";
	private static final String INSERT_EXPORT_PLAN_SQL = "insert into dctm_export_plan (object_type, object_id) values (?, ?)";
	private static final String MARK_EXPORT_PLAN_TRAVERSED_SQL = "update dctm_export_plan set traversed = true where object_id = ? and traversed = false";

	private static final String CLEAR_ALL_MAPPINGS_SQL = "truncate table dctm_mapper";
	private static final String FIND_EXACT_MAPPING_SQL = "select target_value from dctm_mapper where object_type = ? and name = ? and source_value = ? and target_value = ?";
	private static final String FIND_TARGET_MAPPING_SQL = "select target_value from dctm_mapper where object_type = ? and name = ? and source_value = ?";
	private static final String FIND_SOURCE_MAPPING_SQL = "select source_value from dctm_mapper where object_type = ? and name = ? and target_value = ?";
	private static final String INSERT_MAPPING_SQL = "insert into dctm_mapper (object_type, name, source_value, target_value) values (?, ?, ?, ?)";
	private static final String DELETE_TARGET_MAPPING_SQL = "delete from dctm_mapper where object_type = ? and name = ? and source_value = ?";
	private static final String DELETE_SOURCE_MAPPING_SQL = "delete from dctm_mapper where object_type = ? and name = ? and target_value = ?";
	private static final String DELETE_BOTH_MAPPINGS_SQL = "delete from dctm_mapper where object_type = ? and name = ? and not (source_value = ? and target_value = ?) and (source_value = ? or target_value = ?)";

	private static final String LOAD_OBJECT_TYPES_SQL = //
	"   select object_type, count(*) as total " + //
		" from dctm_object " + //
		"group by object_type " + // ;
		"having total > 0 " + //
		"order by object_type ";

	private static final String LOAD_OBJECTS_SQL = //
	"    select * " + //
		"  from dctm_object " + //
		" where object_type = ? " + //
		" order by batch_id, object_number";

	private static final String LOAD_OBJECTS_BY_ID_ANY_SQL = //
	"    select * " + //
		"  from dctm_object " + //
		" where object_type = ? " + //
		"   and object_id = any ( ? ) " + //
		" order by batch_id, object_number";

	private static final String LOAD_OBJECTS_BY_ID_IN_SQL = //
	"    select o.* " + //
		"  from dctm_object o, table(x varchar=?) t " + //
		" where o.object_type = ? " + //
		"   and o.object_id = t.x " + //
		" order by o.batch_id, o.object_number";

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

	protected final Logger log = Logger.getLogger(getClass());

	private class Mapper extends CmsAttributeMapper {
		@Override
		protected Mapping createMapping(CmsObjectType objectType, String mappingName, String sourceValue,
			String targetValue) {
			return CmsObjectStore.this.createMapping(objectType, mappingName, sourceValue, targetValue);
		}

		@Override
		public Mapping getTargetMapping(CmsObjectType objectType, String mappingName, String sourceValue) {
			return CmsObjectStore.this.getTargetMapping(objectType, mappingName, sourceValue);
		}

		@Override
		public Mapping getSourceMapping(CmsObjectType objectType, String mappingName, String targetValue) {
			return CmsObjectStore.this.getSourceMapping(objectType, mappingName, targetValue);
		}

		private Mapping constructMapping(CmsObjectType objectType, String mappingName, String sourceValue,
			String targetValue) {
			return super.newMapping(objectType, mappingName, sourceValue, targetValue);
		}
	}

	private final Mapper mapper = new Mapper();

	private final DataSource dataSource;

	public CmsObjectStore(DataSource dataSource, boolean writeMode) throws CMSMFException {
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
			if (writeMode) {
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
				liquibase.update((String) null);
			} else {
				liquibase.validate();
			}
			ok = true;
		} catch (DatabaseException e) {
			throw new CMSMFException("Failed to generate the SQL schema", e);
		} catch (LiquibaseException e) {
			if (writeMode) {
				throw new CMSMFException("Failed to generate the SQL schema", e);
			} else {
				throw new CMSMFException("The SQL schema is of the wrong version or structure", e);
			}
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

	private boolean serializeObject(Connection c, CmsObject<?> object, CmsTransferContext ctx) throws DfException,
		CMSMFException {
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
			persistDependency(c, object.getType(), object.getId(), ctx);
			final String sql = "select * from dctm_export_plan where traversed = false and object_id = ? and not exists ( select o.object_id from dctm_object o where o.object_id = dctm_export_plan.object_id ) for update";
			lockPS = c.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			lockPS.setString(1, objectId);
			lockRS = lockPS.executeQuery();
			if (!lockRS.next()) {
				// Already serialized...
				return false;
			}

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
			qr.insert(c, CmsObjectStore.INSERT_OBJECT_SQL, CmsObjectStore.HANDLER_NULL, objectId, objectType.name(),
				object.getSubtype(), object.getLabel(), object.getBatchId());
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

	public boolean serializeObject(CmsObject<?> object, final CmsTransferContext ctx) throws DfException,
		CMSMFException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to serialize"); }
		if (ctx == null) { throw new IllegalArgumentException("Must provide a context to serialize with"); }
		boolean ok = false;
		Connection c = null;
		try {
			c = this.dataSource.getConnection();
			// First, make sure no "left behind" garbage gets committed
			c.rollback();
			c.setAutoCommit(false);
			boolean ret = serializeObject(c, object, ctx);
			ok = true;
			return ret;
		} catch (SQLException e) {
			throw new CMSMFException(String.format("Failed to serialize %s", object), e);
		} finally {
			if (ok) {
				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("Committing insert transaction for [%s](%s)", object.getLabel(),
						object.getId()));
				}
				DbUtils.commitAndCloseQuietly(c);
			} else {
				this.log.warn(String.format("Rolling back insert transaction for [%s](%s)", object.getLabel(),
					object.getId()));
				DbUtils.rollbackAndCloseQuietly(c);
			}
		}
	}

	public void deserializeObjects(Class<? extends CmsObject<?>> klass, ObjectHandler handler) throws CMSMFException {
		deserializeObjects(klass, null, handler);
	}

	public void deserializeObjects(Class<? extends CmsObject<?>> klass, Set<String> ids, ObjectHandler handler)
		throws CMSMFException {
		if (klass == null) { throw new IllegalArgumentException("Must provide an object class to deserialize"); }
		if (handler == null) { throw new IllegalArgumentException(
			"Must provide an object handler to handle the deserialized objects"); }
		Connection objConn = null;
		Connection attConn = null;

		final CmsObjectType type = CmsObjectType.decodeFromClass(klass);

		// If we're retrieving by IDs and no IDs have been given, don't waste time or resources
		if ((ids != null) && ids.isEmpty()) { return; }

		try {
			objConn = this.dataSource.getConnection();
			attConn = this.dataSource.getConnection();

			PreparedStatement objPS = null;
			PreparedStatement attPS = null;
			PreparedStatement valPS = null;
			PreparedStatement propPS = null;
			PreparedStatement pvalPS = null;
			try {
				boolean limitByIDs = false;
				boolean useSqlArray = false;
				if (ids == null) {
					objPS = objConn.prepareStatement(CmsObjectStore.LOAD_OBJECTS_SQL);
				} else {
					limitByIDs = true;
					try {
						objPS = objConn.prepareStatement(CmsObjectStore.LOAD_OBJECTS_BY_ID_ANY_SQL);
						useSqlArray = true;
					} catch (SQLException e) {
						objPS = objConn.prepareStatement(CmsObjectStore.LOAD_OBJECTS_BY_ID_IN_SQL);
					}
				}

				attPS = attConn.prepareStatement(CmsObjectStore.LOAD_ATTRIBUTES_SQL);
				valPS = attConn.prepareStatement(CmsObjectStore.LOAD_ATTRIBUTE_VALUES_SQL);
				propPS = attConn.prepareStatement(CmsObjectStore.LOAD_PROPERTIES_SQL);
				pvalPS = attConn.prepareStatement(CmsObjectStore.LOAD_PROPERTY_VALUES_SQL);

				ResultSet objRS = null;
				ResultSet attRS = null;
				ResultSet propRS = null;
				ResultSet valRS = null;

				if (!limitByIDs) {
					objPS.setString(1, type.name());
				} else {
					if (useSqlArray) {
						objPS.setString(1, type.name());
						objPS.setArray(2, objConn.createArrayOf("text", ids.toArray()));
					} else {
						objPS.setObject(1, ids.toArray());
						objPS.setString(2, type.name());
					}
				}
				objRS = objPS.executeQuery();
				String currentBatch = null;
				boolean ok = false;
				try {
					while (objRS.next()) {
						final int objNum = objRS.getInt("object_number");
						// If batching is not required, then we simply use the object number
						// as the batch ID, to ensure that object_number remains the sole
						// ordering factor
						final String batchId = (type.isBatchingSupported() ? objRS.getString("batch_id") : String
							.valueOf(objNum));
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

						final String objId = objRS.getString("object_id");
						final String objLabel = objRS.getString("object_label");
						final CmsObjectType objType = CmsObjectType.valueOf(objRS.getString("object_type"));

						// Make sure we're returning the right thing
						if (objType != type) { throw new CMSMFException(
							String
								.format(
									"Deserialization failure with object #%d [%s](ID=%s) - got type [%s] but expected type [%s]",
									objNum, objLabel, objId, objType.name(), type.name())); }

						if (!klass.isAssignableFrom(objType.getCmsObjectClass())) { throw new CMSMFException(
							String
								.format(
									"Deserialization failure with %s object #%d [%s](ID=%s) - class [%s] is not assignable as [%s]",
									objType.name(), objNum, objLabel, objId, objType.getDeclaringClass()
										.getCanonicalName(), klass.getCanonicalName())); }

						if (this.log.isInfoEnabled()) {
							this.log.info(String.format("De-serializing %s object #%d [%s](%s)", type, objNum,
								objLabel, objId));
						}
						CmsObject<?> obj = objType.newInstance();
						obj.load(objRS);
						if (this.log.isTraceEnabled()) {
							this.log.trace(String.format("De-serialized %s object #%d: %s", type, objNum, obj));
						} else if (this.log.isDebugEnabled()) {
							this.log.debug(String.format("De-serialized %s object #%d [%s](%s)", type, objNum,
								objLabel, objId));
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

						obj.loadCompleted();

						try {
							handler.handle(obj);
						} catch (Exception e) {
							throw new CMSMFException(String.format(
								"Failed to properly import %s object [%s](%s) (#%d)", type, obj.getLabel(),
								obj.getId(), objNum), e);
						}
					}
					ok = true;
				} finally {
					// Make sure we clean up after ourselves
					if (currentBatch != null) {
						try {
							handler.closeBatch(ok);
						} catch (CMSMFException e) {
							this.log.error(String
								.format("Exception caught attempting to close the pending batch [%s] (ok=%s)",
									currentBatch, ok), e);
						}
					}
					DbUtils.closeQuietly(objRS);
				}
			} finally {
				DbUtils.closeQuietly(pvalPS);
				DbUtils.closeQuietly(propPS);
				DbUtils.closeQuietly(valPS);
				DbUtils.closeQuietly(attPS);
				DbUtils.closeQuietly(objPS);
			}
		} catch (SQLException e) {
			throw new CMSMFException(
				String.format("Exception raised trying to deserialize objects of type [%s]", type), e);
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
	private Mapping createMapping(Connection c, CmsObjectType type, String name, String sourceValue, String targetValue)
		throws SQLException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if ((sourceValue == null) && (targetValue == null)) { throw new IllegalArgumentException(
			"Must provide a source or target value to search against"); }
		final QueryRunner qr = new QueryRunner();

		if ((targetValue == null) || (sourceValue == null)) {
			// Delete instead
			final String sql = (targetValue == null ? CmsObjectStore.DELETE_TARGET_MAPPING_SQL
				: CmsObjectStore.DELETE_SOURCE_MAPPING_SQL);
			final String refValue = (targetValue == null ? sourceValue : targetValue);
			int count = qr.update(c, sql, type.name(), name, refValue);
			if (count > 0) {
				this.log.info(String.format("Deleted the mappings [%s/%s/%s->%s] : %d", type, name, sourceValue,
					targetValue, count));
			}
			return null;
		}

		// This delete will clear out any potential conflicts in one fell swoop, while also allowing
		// us to potentially avoid re-creating mappings that are already there.
		int deleteCount = qr.update(c, CmsObjectStore.DELETE_BOTH_MAPPINGS_SQL, type.name(), name, sourceValue,
			targetValue, sourceValue, targetValue);

		// First, check to see if the exact mapping we're looking to create already exists...
		// If the deleteCount is 0, then that means that either there was no mapping there,
		// or the exact mapping we wanted was already there. So if the deleteCount is not 0,
		// we're already good to go on the insert. Otherwise, we have to check for an
		// existing, identical mapping
		if ((deleteCount > 0)
			|| !qr.query(c, CmsObjectStore.FIND_EXACT_MAPPING_SQL, CmsObjectStore.HANDLER_EXISTS, type.name(), name,
				sourceValue, targetValue)) {
			// New mapping...so...we need to delete anything that points to this source, and
			// anything that points to this target, since we don't accept duplicates on either
			// column

			// Now, add the new mapping...
			qr.insert(c, CmsObjectStore.INSERT_MAPPING_SQL, CmsObjectStore.HANDLER_NULL, type.name(), name,
				sourceValue, targetValue);
			this.log
				.info(String.format("Established the mapping [%s/%s/%s->%s]", type, name, sourceValue, targetValue));
		} else if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("The mapping [%s/%s/%s->%s] already exists", type, name, sourceValue,
				targetValue));
		}
		Mapping ret = this.mapper.constructMapping(type, name, sourceValue, targetValue);
		return ret;
	}

	private Mapping createMapping(CmsObjectType type, String name, String sourceValue, String targetValue) {
		boolean ok = false;
		Connection c = null;
		try {
			c = this.dataSource.getConnection();
			c.setAutoCommit(false);
			Mapping ret = createMapping(c, type, name, sourceValue, targetValue);
			ok = true;
			return ret;
		} catch (SQLException e) {
			throw new RuntimeException(String.format("Failed to create the mapping for [%s/%s/%s->%s]", type, name,
				sourceValue, targetValue), e);
		} finally {
			if (ok) {
				DbUtils.commitAndCloseQuietly(c);
			} else {
				DbUtils.rollbackAndCloseQuietly(c);
			}
		}
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
	public String getTargetMappingValue(CmsObjectType type, String name, String sourceValue) {
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
	public String getSourceMappingValue(CmsObjectType type, String name, String targetValue) {
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
			throw new CMSMFException("Failed to connect to the object store's database", e);
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

	protected boolean persistDependency(Connection c, CmsObjectType type, String id, final CmsTransferContext ctx)
		throws CMSMFException {
		QueryRunner qr = CmsObjectStore.getQueryRunner();
		try {
			if (qr.query(c, CmsObjectStore.QUERY_EXPORT_PLAN_DUPE_SQL, CmsObjectStore.HANDLER_EXISTS, id)) {
				// Duplicate dependency...we skip it
				if (this.log.isTraceEnabled()) {
					this.log.trace(String.format("DUPLICATE DEPENDENCY [%s::%s]", type.name(), id));
				}
				return false;
			}
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("PERSISTING DEPENDENCY [%s::%s]", type.name(), id));
			}
			qr.insert(c, CmsObjectStore.INSERT_EXPORT_PLAN_SQL, CmsObjectStore.HANDLER_NULL, type.name(), id);
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("PERSISTED DEPENDENCY [%s::%s]", type.name(), id));
			}
			return true;
		} catch (SQLException e) {
			throw new CMSMFException(String.format("Failed to persist the dependency [%s::%s]", type.name(), id), e);
		}
	}

	protected boolean persistDependency(CmsObjectType type, String id, final CmsTransferContext ctx)
		throws CMSMFException {
		if (type == null) { throw new IllegalArgumentException("Must provide a type to persist a dependency"); }
		if (id == null) { throw new IllegalArgumentException("Must provide an ID for the dependency to be persisted"); }
		final Connection c;
		try {
			c = this.dataSource.getConnection();
		} catch (SQLException e) {
			throw new CMSMFException("Failed to connect to the object store's database", e);
		}
		boolean ok = false;
		try {
			c.setAutoCommit(false);
			boolean ret = persistDependency(c, type, id, ctx);
			ok = true;
			return ret;
		} catch (SQLException e) {
			throw new CMSMFException(String.format("Failed to register the dependency [%s::%s]", type, id), e);
		} finally {
			if (ok) {
				DbUtils.commitAndCloseQuietly(c);
			} else {
				DbUtils.rollbackAndCloseQuietly(c);
			}
		}
	}

	public CmsObject<?> persistDfObject(IDfPersistentObject dfObject, final CmsExportContext ctx) throws DfException,
		CMSMFException {
		final String objectId = dfObject.getObjectId().getId();
		final CmsObjectType type;
		try {
			type = CmsObjectType.decodeType(dfObject);
		} catch (UnsupportedObjectTypeException e) {
			if (this.log.isDebugEnabled()) {
				this.log.warn(e.getMessage());
			}
			ctx.objectSkipped(null, objectId);
			return null;
		}
		ctx.objectExportStarted(type, objectId);
		try {
			CmsObject<?> ret = doPersistDfObject(dfObject, ctx);
			if (ret == null) {
				ctx.objectSkipped(type, objectId);
			} else {
				ctx.objectExportCompleted(ret);
			}
			return ret;
		} catch (DfException e) {
			ctx.objectExportFailed(type, objectId, e);
			throw new DfException(e);
		} catch (CMSMFException e) {
			ctx.objectExportFailed(type, objectId, e);
			throw new CMSMFException(e);
		} catch (Throwable t) {
			// Catch anything else
			ctx.objectExportFailed(type, objectId, t);
			throw RuntimeException.class.cast(t);
		}
	}

	private CmsObject<?> doPersistDfObject(IDfPersistentObject dfObject, final CmsExportContext ctx)
		throws DfException, CMSMFException {
		final CmsObjectType objectType;
		try {
			objectType = CmsObjectType.decodeType(dfObject);
		} catch (UnsupportedObjectTypeException e) {
			if (this.log.isDebugEnabled()) {
				this.log.warn(e.getMessage());
			}
			return null;
		}
		final String objectId = dfObject.getObjectId().getId();
		// If it's already serialized, we skip it
		try {
			persistDependency(objectType, objectId, ctx);
		} catch (CMSMFException e) {
			// Check again...maybe it was a PK violation...
			if (!persistDependency(objectType, objectId, ctx)) { return null; }
			// It wasn't... raise an error
			throw new CMSMFException(String.format(
				"Exception caught while trying to create the mutex lock for [%s::%s]", objectType.name(), objectId), e);
		}
		if (isSerialized(objectId)) { return null; }

		// Not already serialized, so we do the deed.
		CmsObject<?> obj = objectType.newInstance();
		if (!obj.loadFromCMS(dfObject)) {
			// The object is not supported
			return null;
		}
		// We try to traverse its dependencies
		CmsDependencyManager dependencyManager = new CmsDependencyManager() {
			@Override
			protected boolean persistDependency(Dependency dependency) throws CMSMFException {
				return CmsObjectStore.this.persistDependency(dependency.getType(), dependency.getId(), ctx);
			}

			@Override
			public Boolean persistDfObject(IDfPersistentObject dfObject) throws DfException, CMSMFException {
				return (CmsObjectStore.this.persistDfObject(dfObject, ctx) != null);
			}
		};

		obj.persistRequirements(dfObject, ctx, dependencyManager);
		// If somehow it got serialized underneath us (perhaps by another thread), we skip it
		if (!serializeObject(obj, ctx)) { return null; }
		obj.persistDependents(dfObject, ctx, dependencyManager);
		// markTraversed(obj.getId());
		return obj;
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
			throw new CMSMFException("Failed to connect to the object store's database", e);
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

	private Mapping getTargetMapping(CmsObjectType objectType, String mappingName, String sourceValue) {
		String mappedValue = getMappedValue(true, objectType, mappingName, sourceValue);
		if (mappedValue == null) { return null; }
		return this.mapper.constructMapping(objectType, mappingName, sourceValue, mappedValue);
	}

	private Mapping getSourceMapping(CmsObjectType objectType, String mappingName, String targetValue) {
		String mappedValue = getMappedValue(false, objectType, mappingName, targetValue);
		if (mappedValue == null) { return null; }
		return this.mapper.constructMapping(objectType, mappingName, mappedValue, targetValue);
	}

	private Map<CmsObjectType, Integer> getStoredObjectTypes(Connection c) throws CMSMFException {
		QueryRunner qr = new QueryRunner();
		try {
			return qr.query(c, CmsObjectStore.LOAD_OBJECT_TYPES_SQL,
				new ResultSetHandler<Map<CmsObjectType, Integer>>() {
					@Override
					public Map<CmsObjectType, Integer> handle(ResultSet rs) throws SQLException {
						Map<CmsObjectType, Integer> ret = new EnumMap<CmsObjectType, Integer>(CmsObjectType.class);
						while (rs.next()) {
							String t = rs.getString("object_type");
							if ((t == null) || rs.wasNull()) {
								CmsObjectStore.this.log.warn(String.format("NULL TYPE STORED IN DATABASE: [%s]", t));
								continue;
							}
							try {
								ret.put(CmsObjectType.valueOf(t), rs.getInt("total"));
							} catch (IllegalArgumentException e) {
								CmsObjectStore.this.log.warn(String.format("UNSUPPORTED TYPE STORED IN DATABASE: [%s]",
									t));
								continue;
							}
						}
						return ret;
					}
				});
		} catch (SQLException e) {
			throw new CMSMFException("Failed to retrieve the stored object types", e);
		}
	}

	public Map<CmsObjectType, Integer> getStoredObjectTypes() throws CMSMFException {
		final Connection c;
		try {
			c = this.dataSource.getConnection();
		} catch (SQLException e) {
			throw new CMSMFException("Failed to connect to the object store's database", e);
		}
		try {
			return getStoredObjectTypes(c);
		} finally {
			DbUtils.rollbackAndCloseQuietly(c);
		}
	}

	public CmsAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

	private int clearAllMappings(Connection c) throws CMSMFException {
		QueryRunner qr = new QueryRunner();
		try {
			return qr.update(c, CmsObjectStore.CLEAR_ALL_MAPPINGS_SQL);
		} catch (SQLException e) {
			throw new CMSMFException("Failed to clear all the stored mappings", e);
		}
	}

	public int clearAllMappings() throws CMSMFException {
		final Connection c;
		try {
			c = this.dataSource.getConnection();
		} catch (SQLException e) {
			throw new CMSMFException("Failed to connect to the object store's database", e);
		}
		try {
			return clearAllMappings(c);
		} finally {
			DbUtils.rollbackAndCloseQuietly(c);
		}
	}
}