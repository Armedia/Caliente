/**
 *
 */

package com.armedia.cmf.storage.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfNameFixer;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectHandler;
import com.armedia.cmf.storage.CmfObjectRef;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfOperationException;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueCodec;
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

	private final boolean managedTransactions;
	private final DataSourceDescriptor<?> dataSourceDescriptor;
	private final DataSource dataSource;
	private final JdbcStorePropertyManager propertyManager;
	private final JdbcDialect dialect;

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

		try {
			try {
				this.dialect = JdbcDialect.getDialect(c.getMetaData());
			} catch (SQLException e) {
				throw new CmfStorageException("Failed to initialize the query resolver", e);
			}

			this.propertyManager = new JdbcStorePropertyManager(JdbcObjectStore.PROPERTY_TABLE);

			JdbcOperation op = new JdbcOperation(c, this.managedTransactions);
			boolean ok = false;
			op.begin();
			try {
				JdbcSchemaManager.prepareSchema(JdbcObjectStore.SCHEMA_CHANGE_LOG, op, updateSchema, cleanData,
					this.managedTransactions, new JdbcSchemaManager.Callback() {
						@Override
						public void cleanData(JdbcOperation op) throws CmfStorageException {
							clearProperties(op);
							clearAllObjects(op);
							clearAttributeMappings(op);
						}
					});
				op.commit();
				ok = true;
			} finally {
				if (!ok) {
					try {
						op.rollback();
					} catch (CmfOperationException e) {
						this.log.warn(
							String.format("Rollback failed during schema preparation (dialect = %s)", this.dialect), e);
					}
				}
			}
		} finally {
			DbUtils.closeQuietly(c);
		}
	}

	protected final DataSourceDescriptor<?> getDataSourceDescriptor() {
		return this.dataSourceDescriptor;
	}

	@Override
	protected <V> Long storeObject(JdbcOperation operation, CmfObject<V> object, CmfAttributeTranslator<V> translator)
		throws CmfStorageException {
		final Connection c = operation.getConnection();
		final CmfType objectType = object.getType();
		final String objectId = JdbcTools.composeDatabaseId(object);

		Collection<Object[]> attributeParameters = new ArrayList<Object[]>();
		Collection<Object[]> attributeValueParameters = new ArrayList<Object[]>();
		Collection<Object[]> propertyParameters = new ArrayList<Object[]>();
		Collection<Object[]> propertyValueParameters = new ArrayList<Object[]>();
		Collection<Object[]> parentParameters = new ArrayList<Object[]>();
		Object[] attData = new Object[7];
		Object[] attValue = new Object[5];
		Object[] propData = new Object[4];
		Object[] parentData = new Object[3];

		try {
			QueryRunner qr = JdbcTools.getQueryRunner();

			// First, set up the parents
			parentData[0] = objectId;
			int i = 0;
			for (CmfObjectRef parent : object.getParentReferences()) {
				parentData[1] = i;
				parentData[2] = JdbcTools.composeDatabaseId(parent);
				parentParameters.add(parentData.clone());
				i++;
			}

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
								throw new CmfStorageException(
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
								throw new CmfStorageException(
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
			final Long ret = qr.insert(c, translateQuery(JdbcDialect.Query.INSERT_OBJECT),
				this.dialect.getObjectNumberHandler(), objectId, object.getName(), object.getSearchKey(),
				objectType.name(), Tools.coalesce(object.getSubtype(), objectType.name()), object.getLabel(),
				object.getBatchId(), object.isBatchHead(), object.getProductName(), object.getProductVersion());
			if (object.isBatchHead()) {
				// Small optimization, to accelerate queries
				qr.insert(c, translateQuery(JdbcDialect.Query.INSERT_ALT_NAME), JdbcTools.HANDLER_NULL, objectId,
					object.getName());
			}
			qr.insertBatch(c, translateQuery(JdbcDialect.Query.INSERT_OBJECT_PARENTS), JdbcTools.HANDLER_NULL,
				parentParameters.toArray(JdbcTools.NO_PARAMS));
			qr.insertBatch(c, translateQuery(JdbcDialect.Query.INSERT_ATTRIBUTE), JdbcTools.HANDLER_NULL,
				attributeParameters.toArray(JdbcTools.NO_PARAMS));
			qr.insertBatch(c, translateQuery(JdbcDialect.Query.INSERT_ATTRIBUTE_VALUE), JdbcTools.HANDLER_NULL,
				attributeValueParameters.toArray(JdbcTools.NO_PARAMS));
			qr.insertBatch(c, translateQuery(JdbcDialect.Query.INSERT_PROPERTY), JdbcTools.HANDLER_NULL,
				propertyParameters.toArray(JdbcTools.NO_PARAMS));
			qr.insertBatch(c, translateQuery(JdbcDialect.Query.INSERT_PROPERTY_VALUE), JdbcTools.HANDLER_NULL,
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
			parentParameters.clear();
			parentParameters = null;
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
			parentData = null;
		}
	}

	@Override
	protected <V> int loadObjects(JdbcOperation operation, CmfAttributeTranslator<V> translator, final CmfType type,
		Collection<String> ids, CmfObjectHandler<V> handler, boolean batching) throws CmfStorageException {

		// If we're retrieving by IDs and no IDs have been given, don't waste time or resources
		if ((ids != null) && ids.isEmpty()) { return 0; }

		Connection connection = operation.getConnection();
		try {
			PreparedStatement objectPS = null;
			PreparedStatement attributePS = null;
			PreparedStatement attributeValuePS = null;
			PreparedStatement propertyPS = null;
			PreparedStatement propertyValuePS = null;
			PreparedStatement parentsPS = null;
			try {
				boolean limitByIDs = false;
				if (ids == null) {
					objectPS = connection.prepareStatement(translateQuery(
						batching ? JdbcDialect.Query.LOAD_OBJECTS_BATCHED : JdbcDialect.Query.LOAD_OBJECTS));
				} else {
					limitByIDs = true;
					objectPS = connection.prepareStatement(translateQuery(batching
						? JdbcDialect.Query.LOAD_OBJECTS_BY_ID_BATCHED : JdbcDialect.Query.LOAD_OBJECTS_BY_ID));
				}

				attributePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ATTRIBUTES));
				attributeValuePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ATTRIBUTE_VALUES));
				propertyPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PROPERTIES));
				propertyValuePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PROPERTY_VALUES));
				parentsPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PARENT_IDS));

				ResultSet objectRS = null;
				ResultSet attributeRS = null;
				ResultSet propertyRS = null;
				ResultSet valueRS = null;
				ResultSet parentsRS = null;

				if (!limitByIDs) {
					objectPS.setString(1, type.name());
				} else {
					// Process the IDs
					Object[] arr = ids.toArray();
					for (int i = 0; i < arr.length; i++) {
						arr[i] = JdbcTools.composeDatabaseId(type, arr[i].toString());
					}
					if (this.dialect.isSupportsArrays()) {
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

							parentsPS.setString(1, objId);
							parentsRS = parentsPS.executeQuery();

							obj = loadObject(translator, objectRS, parentsRS);
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
					DbUtils.closeQuietly(parentsRS);
				}
			} finally {
				DbUtils.closeQuietly(parentsPS);
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

	@Override
	protected <V> void fixObjectNames(JdbcOperation operation, CmfAttributeTranslator<V> translator, final CmfType type,
		Collection<String> ids, CmfNameFixer<V> nameFixer) throws CmfStorageException {

		// If we're retrieving by IDs and no IDs have been given, don't waste time or resources
		if ((ids != null) && ids.isEmpty()) { return; }

		Connection connection = operation.getConnection();
		try {
			PreparedStatement objectPS = null;
			PreparedStatement attributePS = null;
			PreparedStatement attributeValuePS = null;
			PreparedStatement propertyPS = null;
			PreparedStatement propertyValuePS = null;
			PreparedStatement parentsPS = null;
			try {
				boolean limitByIDs = false;
				final String query;
				if (ids == null) {
					query = translateQuery(JdbcDialect.Query.LOAD_OBJECTS_HEAD);
				} else {
					limitByIDs = true;
					query = translateQuery(JdbcDialect.Query.LOAD_OBJECTS_BY_ID_HEAD);
				}
				objectPS = connection.prepareStatement(query);

				attributePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ATTRIBUTES));
				attributeValuePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ATTRIBUTE_VALUES));
				propertyPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PROPERTIES));
				propertyValuePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PROPERTY_VALUES));
				parentsPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PARENT_IDS));

				ResultSet objectRS = null;
				ResultSet attributeRS = null;
				ResultSet propertyRS = null;
				ResultSet valueRS = null;
				ResultSet parentsRS = null;

				QueryRunner qr = null;

				if (!limitByIDs) {
					objectPS.setString(1, type.name());
				} else {
					// Process the IDs
					Object[] arr = ids.toArray();
					for (int i = 0; i < arr.length; i++) {
						arr[i] = JdbcTools.composeDatabaseId(type, arr[i].toString());
					}
					if (this.dialect.isSupportsArrays()) {
						objectPS.setString(1, type.name());
						objectPS.setArray(2, connection.createArrayOf("text", arr));
					} else {
						objectPS.setObject(1, arr);
						objectPS.setString(2, type.name());
					}
				}
				objectRS = objectPS.executeQuery();
				try {
					while (objectRS.next()) {
						final CmfObject<V> obj;
						try {
							final int objNum = objectRS.getInt("object_number");

							final String objId = objectRS.getString("object_id");
							final String objLabel = objectRS.getString("object_label");

							if (this.log.isInfoEnabled()) {
								this.log.info(String.format("De-serializing %s object #%d [%s](%s)", type, objNum,
									objLabel, objId));
							}

							parentsPS.setString(1, objId);
							parentsRS = parentsPS.executeQuery();

							obj = loadObject(translator, objectRS, parentsRS);
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
							if (!nameFixer.handleException(e)) { throw new CmfStorageException(
								"Exception raised while loading objects - NameFixer did not handle the exception", e); }
							continue;
						}

						String newName = nameFixer.fixName(obj);
						if ((newName != null) && !Tools.equals(newName, obj.getName())) {
							// Update the name in the alt_names table
							if (qr == null) {
								qr = new QueryRunner();
							}
							CmfObjectRef ref = new CmfObjectRef(obj);
							int updateCount = qr.update(connection, translateQuery(JdbcDialect.Query.UPDATE_ALT_NAME),
								newName, JdbcTools.composeDatabaseId(ref), obj.getName());
							if (updateCount != 1) {
								//
								throw new CmfStorageException(String.format(
									"Failed to update the name for %s [%s](%s) from [%s] to [%s] - updated %d records, expected exactly 1",
									obj.getType(), obj.getLabel(), obj.getId(), obj.getName(), newName, updateCount));
							}
						}
					}
				} finally {
					DbUtils.closeQuietly(objectRS);
					DbUtils.closeQuietly(parentsRS);
				}
			} finally {
				DbUtils.closeQuietly(parentsPS);
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
			final String sql = (targetValue == null ? translateQuery(JdbcDialect.Query.DELETE_TARGET_MAPPING)
				: translateQuery(JdbcDialect.Query.DELETE_SOURCE_MAPPING));
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
		int deleteCount = qr.update(c, translateQuery(JdbcDialect.Query.DELETE_BOTH_MAPPINGS), type.name(), name,
			sourceValue, targetValue, sourceValue, targetValue);

		// First, check to see if the exact mapping we're looking to create already exists...
		// If the deleteCount is 0, then that means that either there was no mapping there,
		// or the exact mapping we wanted was already there. So if the deleteCount is not 0,
		// we're already good to go on the insert. Otherwise, we have to check for an
		// existing, identical mapping
		if ((deleteCount > 0) || !qr.query(c, translateQuery(JdbcDialect.Query.FIND_EXACT_MAPPING),
			JdbcTools.HANDLER_EXISTS, type.name(), name, sourceValue, targetValue)) {
			// New mapping...so...we need to delete anything that points to this source, and
			// anything that points to this target, since we don't accept duplicates on either
			// column

			// Now, add the new mapping...
			qr.insert(c, translateQuery(JdbcDialect.Query.INSERT_MAPPING), JdbcTools.HANDLER_NULL, type.name(), name,
				sourceValue, targetValue);
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
		final String sql = translateQuery(
			source ? JdbcDialect.Query.FIND_TARGET_MAPPING : JdbcDialect.Query.FIND_SOURCE_MAPPING);
		try {
			return qr.query(sql, h, type.name(), name, value);
		} catch (SQLException e) {
			throw new CmfStorageException(String.format("Failed to retrieve the %s mapping for [%s::%s(%s)]",
				source ? "source" : "target", type, name, value), e);
		}
	}

	private boolean isStored(Connection c, CmfType type, String objectId) throws SQLException {
		return JdbcTools.getQueryRunner().query(c, translateQuery(JdbcDialect.Query.CHECK_IF_OBJECT_EXISTS),
			JdbcTools.HANDLER_EXISTS, JdbcTools.composeDatabaseId(type, objectId), type.name());
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
			return new QueryRunner().query(c, translateQuery(JdbcDialect.Query.LOAD_OBJECT_TYPES),
				new ResultSetHandler<Map<CmfType, Integer>>() {
					@Override
					public Map<CmfType, Integer> handle(ResultSet rs) throws SQLException {
						Map<CmfType, Integer> ret = new EnumMap<CmfType, Integer>(CmfType.class);
						while (rs.next()) {
							String t = rs.getString(1);
							if ((t == null) || rs.wasNull()) {
								JdbcObjectStore.this.log.warn(String.format("NULL TYPE STORED IN DATABASE: [%s]", t));
								continue;
							}
							try {
								ret.put(CmfType.decodeString(t), rs.getInt(2));
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
			return new QueryRunner().update(c, translateQuery(JdbcDialect.Query.CLEAR_ALL_MAPPINGS));
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
			new QueryRunner().query(operation.getConnection(), translateQuery(JdbcDialect.Query.LOAD_ALL_MAPPINGS), h);
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
			new QueryRunner().query(operation.getConnection(), translateQuery(JdbcDialect.Query.LOAD_TYPE_MAPPINGS), h,
				type.name());
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
			new QueryRunner().query(operation.getConnection(),
				translateQuery(JdbcDialect.Query.LOAD_TYPE_NAME_MAPPINGS), h, type.name(), name);
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
			"CMF_CONTENT_PROPERTY", "CMF_PROPERTY_VALUE", "CMF_ATTRIBUTE_VALUE", "CMF_MAPPER", "CMF_CONTENT",
			"CMF_PROPERTY", "CMF_ATTRIBUTE", "CMF_OBJECT", "CMF_EXPORT_PLAN", "CMF_INFO"
		};
		final Connection c = operation.getConnection();
		try {
			Statement s = c.createStatement();
			final boolean referentialIntegrityOff = disableReferentialIntegrity(operation);
			try {
				final String sqlFmt = translateQuery(JdbcDialect.Query.TRUNCATE_TABLE_FMT);
				for (String t : tables) {
					s.executeUpdate(String.format(sqlFmt, t));
				}
			} finally {
				if (referentialIntegrityOff) {
					enableReferentialIntegrity(operation);
				}
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

	private <V> CmfObject<V> loadObject(CmfAttributeTranslator<V> translator, ResultSet objectRS, ResultSet parentsRS)
		throws SQLException {
		if (objectRS == null) { throw new IllegalArgumentException(
			"Must provide a ResultSet to load the structure from"); }
		CmfType type = CmfType.decodeString(objectRS.getString("object_type"));
		String id = objectRS.getString("object_id");
		String name = objectRS.getString("object_name");
		Matcher m = JdbcTools.OBJECT_ID_PARSER.matcher(id);
		if (m.matches()) {
			id = m.group(2);
		}
		String searchKey = objectRS.getString("search_key");
		if (objectRS.wasNull()) {
			searchKey = id;
		}
		Long number = objectRS.getLong("object_number");
		if (objectRS.wasNull()) {
			number = null;
		}
		String batchId = objectRS.getString("batch_id");
		boolean batchHead = objectRS.getBoolean("batch_head");
		String label = objectRS.getString("object_label");
		String subtype = objectRS.getString("object_subtype");
		String productName = objectRS.getString("product_name");
		String productVersion = objectRS.getString("product_version");

		// Load the parent IDs
		List<CmfObjectRef> parentIds = new ArrayList<CmfObjectRef>();
		while (parentsRS.next()) {
			String parentId = parentsRS.getString("parent_id");
			if (parentsRS.wasNull()) {
				continue;
			}
			parentIds.add(JdbcTools.decodeDatabaseId(parentId));
		}
		if (parentIds.isEmpty()) {
			parentIds = Collections.emptyList();
		}

		return new CmfObject<V>(translator, type, id, name, parentIds, searchKey, batchId, batchHead, label, subtype,
			productName, productVersion, number);
	}

	private <V> CmfProperty<V> loadProperty(CmfType objectType, CmfAttributeTranslator<V> translator, ResultSet rs)
		throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = rs.getString("name");
		CmfDataType type = translator.decodeValue(rs.getString("data_type"));
		boolean repeating = rs.getBoolean("repeating") && !rs.wasNull();
		return new CmfProperty<V>(name, type, repeating);
	}

	private <V> CmfAttribute<V> loadAttribute(CmfType objectType, CmfAttributeTranslator<V> translator, ResultSet rs)
		throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = translator.decodeAttributeName(objectType, rs.getString("name"));
		CmfDataType type = translator.decodeValue(rs.getString("data_type"));
		boolean repeating = rs.getBoolean("repeating") && !rs.wasNull();
		return new CmfAttribute<V>(name, type, repeating);
	}

	private <V> void loadValues(CmfValueCodec<V> codec, CmfValueSerializer deserializer, ResultSet rs,
		CmfProperty<V> property) throws SQLException, CmfStorageException {
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
				throw new CmfStorageException(
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
		throws SQLException {
		List<CmfAttribute<V>> attributes = new LinkedList<CmfAttribute<V>>();
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		while (rs.next()) {
			attributes.add(loadAttribute(obj.getType(), translator, rs));
		}
		obj.setAttributes(attributes);
	}

	private <V> void loadProperties(CmfAttributeTranslator<V> translator, ResultSet rs, CmfObject<V> obj)
		throws SQLException {
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
			qr.insert(c, translateQuery(JdbcDialect.Query.INSERT_EXPORT_PLAN), JdbcTools.HANDLER_NULL, type.name(),
				dbid);
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("PERSISTED DEPENDENCY [%s::%s]", type.name(), id));
			}
			return true;
		} catch (SQLException e) {
			if (this.dialect.isDuplicateKeyException(e)) {
				// We're good...ish... PostgreSQL will have invalidated the transaction, which can
				// bring problems for other operations within this transaction
				if (this.log.isTraceEnabled()) {
					this.log.trace(String.format("DUPLICATE DEPENDENCY [%s::%s]", type.name(), id));
				}
				return false;
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
			qr.update(c, translateQuery(JdbcDialect.Query.DELETE_CONTENT), objectId);
		} catch (SQLException e) {
			throw new CmfStorageException(String.format("Failed to delete the existing content records for %s [%s](%s)",
				object.getType(), object.getLabel(), object.getId()), e);
		}

		// Step 2: prepare the new content records and properties
		List<Object[]> contents = new ArrayList<Object[]>();
		List<Object[]> properties = new ArrayList<Object[]>();
		Object[] cArr = new Object[8];
		Object[] pArr = new Object[5];
		int pos = 0;
		for (CmfContentInfo i : content) {
			// First, the content record...
			cArr[0] = objectId;
			cArr[1] = i.getRenditionIdentifier();
			cArr[2] = i.getRenditionPage();
			cArr[3] = i.getExtension();
			cArr[4] = pos++;
			cArr[5] = i.getLength();
			cArr[6] = Tools.toString(Tools.coalesce(i.getMimeType(), MimeTools.DEFAULT_MIME_TYPE));
			cArr[7] = i.getFileName();
			contents.add(cArr.clone());

			// Then, the properties...
			pArr[0] = objectId;
			pArr[1] = i.getRenditionIdentifier();
			pArr[2] = i.getRenditionPage();
			for (String s : i.getPropertyNames()) {
				if (s == null) {
					continue;
				}
				pArr[3] = s;
				pArr[4] = i.getProperty(s);
				if (pArr[4] == null) {
					continue;
				}
				properties.add(pArr.clone());
			}
		}

		// Step 3: execute the batch inserts
		try {
			qr.insertBatch(c, translateQuery(JdbcDialect.Query.INSERT_CONTENT), JdbcTools.HANDLER_NULL,
				contents.toArray(JdbcTools.NO_PARAMS));
			qr.insertBatch(c, translateQuery(JdbcDialect.Query.INSERT_CONTENT_PROPERTY), JdbcTools.HANDLER_NULL,
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
			cPS = c.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_CONTENTS));
			pPS = c.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_CONTENT_PROPERTIES));

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
						final CmfContentInfo info = new CmfContentInfo(rs.getString("rendition_id"),
							rs.getInt("rendition_page"));
						info.setLength(rs.getLong("stream_length"));
						String ext = rs.getString("extension");
						if (rs.wasNull() || StringUtils.isEmpty(ext)) {
							ext = null;
						}
						info.setExtension(ext);
						String str = rs.getString("mime_type");
						if ((str != null) && !rs.wasNull()) {
							info.setMimeType(MimeTools.resolveMimeType(str));
						}
						str = rs.getString("file_name");
						if ((str != null) && !rs.wasNull()) {
							info.setFileName(str);
						}

						Map<String, String> props = qr.query(c,
							translateQuery(JdbcDialect.Query.LOAD_CONTENT_PROPERTIES), pHandler, objectId,
							info.getRenditionIdentifier(), info.getRenditionPage());
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

			return new QueryRunner().query(c, translateQuery(JdbcDialect.Query.LOAD_CONTENTS), cHandler, objectId);
		} catch (SQLException e) {
			throw new CmfStorageException(String.format("Failed to load the content records for %s [%s](%s)",
				object.getType(), object.getLabel(), object.getId()), e);
		} finally {
			DbUtils.closeQuietly(pPS);
			DbUtils.closeQuietly(cPS);
		}
	}

	@Override
	public final <V> String getFirstUniqueName(JdbcOperation operation, CmfObject<V> object, String... names)
		throws CmfStorageException {
		final Connection c = operation.getConnection();
		final String objectId = JdbcTools.composeDatabaseId(object);

		PreparedStatement parentPS = null;
		String winner = null;
		try {
			parentPS = c.prepareStatement(translateQuery(JdbcDialect.Query.CHECK_FOR_NAME_COLLISIONS));
			nextName: for (String tentativeName : names) {
				parentPS.clearParameters();
				parentPS.setString(2, tentativeName);

				// Check to see if this tentative name is unique within each parent. Uniqueness
				// is a "funny" thing: a name is unique if and only if the object ID for the lowest
				// object number for a given parent, belongs to the object being checked against,
				// for every parent that this test is applied to
				winner = tentativeName;
				nextParent: for (CmfObjectRef r : object.getParentReferences()) {
					// Set the parent
					parentPS.setString(1, JdbcTools.composeDatabaseId(r));
					ResultSet rs = parentPS.executeQuery();
					try {
						// If there were no hits, then we try the next parent right away!
						if (!rs.next()) {
							continue nextParent;
						}

						// There's a hit - is it this same object? If it's not, then it means
						// that another object has priority over this name, and so we must
						// try again.
						String otherId = rs.getString("object_id");
						if (rs.wasNull()) {
							continue;
						}
						if (Tools.equals(otherId, objectId)) {
							// Ok so we check out on this parent...
							continue nextParent;
						}
						continue nextName;
					} finally {
						DbUtils.closeQuietly(rs);
					}
				}
			}
			return winner;
		} catch (SQLException e) {
			throw new CmfStorageException(String.format("Failed to perform the name collision checks for %s [%s](%s)",
				object.getType(), object.getLabel(), object.getId()), e);
		} finally {
			DbUtils.closeQuietly(parentPS);
		}
	}

	protected boolean disableReferentialIntegrity(JdbcOperation operation) throws CmfStorageException {
		String sql = translateOptionalQuery(JdbcDialect.Query.DISABLE_REFERENTIAL_INTEGRITY);
		if (sql == null) { return false; }
		Connection c = operation.getConnection();
		Statement s = null;
		try {
			s = c.createStatement();
			s.execute(sql);
			return true;
		} catch (SQLException e) {
			this.log.trace("Failed to disable the referential integrity constraints", e);
			return false;
		} finally {
			DbUtils.closeQuietly(s);
		}
	}

	protected void enableReferentialIntegrity(JdbcOperation operation) throws CmfStorageException {
		Connection c = operation.getConnection();
		String sql = translateOptionalQuery(JdbcDialect.Query.ENABLE_REFERENTIAL_INTEGRITY);
		if (sql == null) { return; }
		Statement s = null;
		try {
			s = c.createStatement();
			s.execute(sql);
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to re-enable the referential integrity constraints", e);
		} finally {
			DbUtils.closeQuietly(s);
		}
	}

	protected String translateOptionalQuery(JdbcDialect.Query query) {
		return this.dialect.translateQuery(query, false);
	}

	protected String translateQuery(JdbcDialect.Query query) {
		return this.dialect.translateQuery(query, true);
	}
}