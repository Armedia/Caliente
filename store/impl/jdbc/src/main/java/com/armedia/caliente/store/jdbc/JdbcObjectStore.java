/**
 *
 */

package com.armedia.caliente.store.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import javax.sql.DataSource;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfNameFixer;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectHandler;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectSpec;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfOperationException;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.caliente.store.CmfValueSerializer;
import com.armedia.caliente.store.tools.DefaultCmfObjectHandler;
import com.armedia.caliente.store.tools.MimeTools;
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

		Collection<Object[]> attributeParameters = new ArrayList<>();
		Collection<Object[]> attributeValueParameters = new ArrayList<>();
		Collection<Object[]> propertyParameters = new ArrayList<>();
		Collection<Object[]> propertyValueParameters = new ArrayList<>();
		Collection<Object[]> parentParameters = new ArrayList<>();
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
			final Map<String, String> encodedNames = new HashMap<>();
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
				object.getDependencyTier(), object.getHistoryId(), object.isHistoryCurrent(), object.getProductName(),
				object.getProductVersion());
			qr.insert(c, translateQuery(JdbcDialect.Query.INSERT_ALT_NAME), JdbcTools.HANDLER_NULL, objectId,
				object.getName());
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
	protected <V> CmfObject<V> loadHeadObject(JdbcOperation operation, CmfTypeMapper typeMapper,
		CmfAttributeTranslator<V> translator, CmfObject<V> sample) throws CmfStorageException {
		final Connection connection = operation.getConnection();
		final CmfType type = sample.getType();
		try {
			PreparedStatement objectPS = null;
			PreparedStatement attributePS = null;
			PreparedStatement attributeValuePS = null;
			PreparedStatement propertyPS = null;
			PreparedStatement propertyValuePS = null;
			PreparedStatement parentsPS = null;
			try {
				objectPS = connection
					.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_OBJECT_HISTORY_CURRENT_BY_HISTORY_ID));
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

				objectPS.setString(1, sample.getType().name());
				objectPS.setString(2, sample.getHistoryId());
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
							throw new CmfStorageException(
								"Exception raised while loading objects - ObjectHandler did not handle the exception",
								e);
						}
						return obj;
					}
					return null;
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

	@Override
	protected <V> int loadObjects(JdbcOperation operation, CmfAttributeTranslator<V> translator, final CmfType type,
		Collection<String> ids, CmfObjectHandler<V> handler) throws CmfStorageException {

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
					objectPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_OBJECTS));
				} else {
					limitByIDs = true;
					objectPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_OBJECTS_BY_ID));
				}

				attributePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ATTRIBUTES));
				attributeValuePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ATTRIBUTE_VALUES));
				propertyPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PROPERTIES));
				propertyValuePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PROPERTY_VALUES));
				parentsPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PARENT_IDS));

				ResultSet objectRS = null;
				ResultSet attributeRS = null;
				ResultSet propertyRS = null;
				ResultSet parentsRS = null;
				ResultSet valueRS = null;

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
				Integer currentTier = null;
				String currentHistory = null;
				boolean ok = false;
				int ret = 0;
				try {
					while (objectRS.next()) {
						final CmfObject<V> obj;
						try {
							final int objNum = objectRS.getInt("object_number");
							final int tierId = objectRS.getInt("tier_id");
							String historyId = objectRS.getString("history_id");
							if ((historyId == null) || objectRS.wasNull()) {
								historyId = String.format("%08x", objNum);
							}

							// Do the tier checking
							if (!Tools.equals(currentTier, tierId)) {
								if (currentTier != null) {
									if (this.log.isDebugEnabled()) {
										this.log.debug(String.format("CLOSE TIER: %d", currentTier));
									}
									if (!handler.endTier(true)) {
										this.log.warn(String.format("%s tier [%d] requested processing cancellation",
											type.name(), tierId));
										currentTier = null;
										break;
									}
								}

								if (this.log.isDebugEnabled()) {
									this.log.debug(String.format("NEW TIER: %s", tierId));
								}
								if (!handler.newTier(tierId)) {
									this.log.warn(String.format("%s tier [%s] skipped", type.name(), tierId));
									continue;
								}
								currentTier = tierId;
							}

							// Do the history checking
							if (!Tools.equals(currentHistory, historyId)) {
								if (currentHistory != null) {
									if (this.log.isDebugEnabled()) {
										this.log.debug(String.format("CLOSE HISTORY: %s", currentHistory));
									}
									if (!handler.endHistory(true)) {
										this.log.warn(String.format("%s history [%s] requested processing cancellation",
											type.name(), historyId));
										currentHistory = null;
										break;
									}
								}

								if (this.log.isDebugEnabled()) {
									this.log.debug(String.format("NEW HISTORY: %s", historyId));
								}
								if (!handler.newHistory(historyId)) {
									this.log.warn(String.format("%s history [%s] skipped", type.name(), historyId));
									continue;
								}
								currentHistory = historyId;
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
							// TODO: WTF?!?! Why is it detecting this when it's clearly impossible?
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
					if (currentHistory != null) {
						try {
							handler.endHistory(ok);
						} catch (CmfStorageException e) {
							this.log.error(
								String.format("Exception caught attempting to close the pending history [%s] (ok=%s)",
									currentHistory, ok),
								e);
						} finally {
							currentHistory = null;
						}
					}
					if (currentTier != null) {
						try {
							handler.endTier(ok);
						} catch (CmfStorageException e) {
							this.log.error(
								String.format("Exception caught attempting to close the pending tier [%d] (ok=%s)",
									currentTier, ok),
								e);
						} finally {
							currentTier = null;
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
	protected <V> int fixObjectNames(final JdbcOperation operation, CmfAttributeTranslator<V> translator,
		final CmfNameFixer<V> nameFixer, CmfType type, Set<String> ids) throws CmfStorageException {
		final AtomicInteger result = new AtomicInteger(0);
		CmfType[] types = {
			type
		};
		if (type == null) {
			types = CmfType.values();
		}
		for (CmfType currentType : types) {
			if (!nameFixer.supportsType(currentType)) {
				continue;
			}

			loadObjects(operation, translator, currentType, ids, new CmfObjectHandler<V>() {
				@Override
				public boolean newTier(int tierNumber) throws CmfStorageException {
					return true;
				}

				@Override
				public boolean newHistory(String historyId) throws CmfStorageException {
					return true;
				}

				@Override
				public boolean handleObject(CmfObject<V> obj) throws CmfStorageException {
					final String oldName = obj.getName();
					final String newName = nameFixer.fixName(obj);
					if ((newName != null) && !Tools.equals(oldName, newName)) {
						renameObject(operation, obj, newName);
						result.incrementAndGet();
						try {
							nameFixer.nameFixed(obj, oldName, newName);
						} catch (Exception e) {
							// Just log it
							JdbcObjectStore.this.log.warn(String.format(
								"Exception caught while invoking the nameFixed() callback for %s [%s](%s)",
								obj.getType().name(), obj.getLabel(), obj.getId()), e);
						}
					}
					return true;
				}

				@Override
				public boolean handleException(Exception e) {
					return false;
				}

				@Override
				public boolean endHistory(boolean ok) throws CmfStorageException {
					return true;
				}

				@Override
				public boolean endTier(boolean ok) throws CmfStorageException {
					return true;
				}
			});

		}
		return result.get();
	}

	@Override
	protected <V> void renameObject(final JdbcOperation operation, final CmfObject<V> object, final String newName)
		throws CmfStorageException {
		final Connection connection = operation.getConnection();
		final QueryRunner qr = JdbcTools.getQueryRunner();
		final String objectId = JdbcTools.composeDatabaseId(object);
		try {
			int updateCount = qr.update(connection, translateQuery(JdbcDialect.Query.UPDATE_ALT_NAME), newName,
				objectId, object.getName());
			if (updateCount != 1) { throw new CmfStorageException(String.format(
				"Failed to update the name for %s [%s](%s) from [%s] to [%s] - updated %d records, expected exactly 1",
				object.getType(), object.getLabel(), object.getId(), object.getName(), newName, updateCount)); }
		} catch (SQLException e) {
			throw new CmfStorageException(String.format("Failed to change the name for %s [%s](%s) from [%s] to [%s]",
				object.getType(), object.getLabel(), object.getId(), object.getName(), newName), e);
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

	@Override
	protected StoreStatus getStoreStatus(JdbcOperation operation, final CmfObjectRef target)
		throws CmfStorageException {
		final Connection c = operation.getConnection();
		final QueryRunner qr = JdbcTools.getQueryRunner();
		final String dbId = JdbcTools.composeDatabaseId(target);
		try {
			// Get the result string from the EXPORT_PLAN
			/*
			boolean exists = qr.query(c, translateQuery(JdbcDialect.Query.CHECK_IF_OBJECT_EXISTS),
				JdbcTools.HANDLER_EXISTS, dbId, type.name());
			*/
			// If it does exist, then this one MUST return exactly one result.
			String result = qr.query(c, translateQuery(JdbcDialect.Query.GET_EXPORT_RESULT),
				new ResultSetHandler<String>() {
					@Override
					public String handle(ResultSet rs) throws SQLException {
						if (rs.next()) {
							String v = rs.getString(1);
							return (rs.wasNull() ? null : v);
						}
						throw new SQLException(
							String.format("Failed to locate the export result for %s", target.getShortLabel()));
					}
				}, dbId, target.getType().name());
			if (result == null) { return null; }
			return StoreStatus.valueOf(result);
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to check whether object %s was already serialized", target.getShortLabel()), e);
		}
	}

	@Override
	protected <V> boolean markStoreStatus(JdbcOperation operation, CmfObjectRef target, StoreStatus status,
		String message) throws CmfStorageException {
		final Connection c = operation.getConnection();
		QueryRunner qr = JdbcTools.getQueryRunner();
		final String dbid = JdbcTools.composeDatabaseId(target);
		try {
			// No existing status, so we can continue
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("ATTEMPTING TO SET THE EXPORT RESULT TO [%s] FOR %s", status.name(),
					target.getShortLabel()));
			}
			int result = qr.update(c, translateQuery(JdbcDialect.Query.UPDATE_EXPORT_RESULT), status.name(), message,
				target.getType().name(), dbid);
			if (result == 1) {
				if (this.log.isDebugEnabled()) {
					this.log.debug(
						String.format("SET THE EXPORT RESULT TO [%s] FOR %s", status.name(), target.getShortLabel()));
				}
				return true;
			}

			if (result > 1) { throw new CmfStorageException(
				String.format("REFERENTIAL INTEGRITY IS NOT BEING ENFORCED: %d PLAN records for %s", result,
					target.getShortLabel())); }

			// If we've come this far, then we failed to set the status. This may or may not be a
			// problem...
			StoreStatus existing = getStoreStatus(operation, target);
			if (existing != null) {
				// If the existing status was already set, this isn't really a problem - we just
				// report the discrepancy if necessary, and return false...then move on
				if (existing != status) {
					this.log.warn(String.format("FAILED TO SET THE RESULT TO [%s] for %s (%d updated, existing=%s)",
						status.name(), target.getShortLabel(), result, existing.name()));
				}
				return false;
			}

			// HUH?? WTF?!!? This means that we failed to update the record because it
			// doesn't exist...
			// TODO: Should we, instead of failing, insert the record? That might prove problematic
			throw new CmfStorageException(
				String.format("FAILED TO SET THE RESULT TO [%s] for %s - NO EXISTING RECORD TO STORE", status.name(),
					target.getShortLabel(), result));
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to set the storage status to [%s] for %s", status.name(), target.getShortLabel()),
				e);
		}
	}

	private Map<CmfType, Long> getStoredObjectTypes(Connection c) throws CmfStorageException {
		try {
			return new QueryRunner().query(c, translateQuery(JdbcDialect.Query.LOAD_OBJECT_TYPES),
				new ResultSetHandler<Map<CmfType, Long>>() {
					@Override
					public Map<CmfType, Long> handle(ResultSet rs) throws SQLException {
						Map<CmfType, Long> ret = new EnumMap<>(CmfType.class);
						while (rs.next()) {
							String t = rs.getString(1);
							if ((t == null) || rs.wasNull()) {
								JdbcObjectStore.this.log.warn(String.format("NULL TYPE STORED IN DATABASE: [%s]", t));
								continue;
							}
							try {
								ret.put(CmfType.decodeString(t), rs.getLong(2));
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
	protected Map<CmfType, Long> getStoredObjectTypes(JdbcOperation operation) throws CmfStorageException {
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
		final Map<CmfType, Set<String>> ret = new EnumMap<>(CmfType.class);
		ResultSetHandler<Void> h = new ResultSetHandler<Void>() {
			@Override
			public Void handle(ResultSet rs) throws SQLException {
				CmfType currentType = null;
				Set<String> names = null;
				while (rs.next()) {
					final CmfType newType = CmfType.decodeString(rs.getString("object_type"));
					if (newType != currentType) {
						names = new TreeSet<>();
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
		final Set<String> ret = new TreeSet<>();
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
		final Map<String, String> ret = new HashMap<>();
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

	private Set<String> getCmfTables(Connection c) throws SQLException {
		DatabaseMetaData dmd = c.getMetaData();
		ResultSet rs = null;
		Set<String> tableNames = new TreeSet<>();
		try {
			rs = dmd.getTables(null, null, null, new String[] {
				"TABLE"
			});
			while (rs.next()) {
				String tn = rs.getString("TABLE_NAME");
				if (tn.toLowerCase().startsWith("cmf_")) {
					tableNames.add(tn);
				}
			}
			return tableNames;
		} finally {
			DbUtils.closeQuietly(rs);
		}
	}

	protected boolean optimizedClearAllObjects(JdbcOperation operation) throws CmfStorageException {
		/*
		String[] tables = {
			"CMF_CONTENT_PROPERTY", "CMF_PROPERTY_VALUE", "CMF_ATTRIBUTE_VALUE", "CMF_MAPPER", "CMF_CONTENT",
			"CMF_PROPERTY", "CMF_ATTRIBUTE", "CMF_OBJECT", "CMF_EXPORT_PLAN", "CMF_INFO"
		};
		*/
		final Connection c = operation.getConnection();
		try {
			final boolean referentialIntegrityOff = disableReferentialIntegrity(operation);
			if (!referentialIntegrityOff) {
				// If we couldn't turn off referential integrity, then we couldn't perform
				// the optimized clear
				return false;
			}
			Set<String> tables = getCmfTables(c);
			Statement s = c.createStatement();
			try {
				final String sqlFmt = translateQuery(JdbcDialect.Query.TRUNCATE_TABLE_FMT);
				for (String t : tables) {
					s.executeUpdate(String.format(sqlFmt, t));
				}
			} finally {
				enableReferentialIntegrity(operation);
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
		QueryRunner qr = JdbcTools.getQueryRunner();
		for (String tableName : getCmfTables(c)) {
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
		String name = objectRS.getString("new_name");
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
		int tierId = objectRS.getInt("tier_id");
		String historyId = objectRS.getString("history_id");
		boolean historyCurrent = objectRS.getBoolean("history_current");
		String label = objectRS.getString("object_label");
		String subtype = objectRS.getString("object_subtype");
		String productName = objectRS.getString("product_name");
		String productVersion = objectRS.getString("product_version");

		// Load the parent IDs
		List<CmfObjectRef> parentIds = new ArrayList<>();
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

		return new CmfObject<>(translator, type, id, name, parentIds, searchKey, tierId, historyId, historyCurrent,
			label, subtype, productName, productVersion, number);
	}

	private <V> CmfProperty<V> loadProperty(CmfType objectType, CmfAttributeTranslator<V> translator, ResultSet rs)
		throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = rs.getString("name");
		CmfDataType type = translator.decodeValue(rs.getString("data_type"));
		boolean repeating = rs.getBoolean("repeating") && !rs.wasNull();
		return new CmfProperty<>(name, type, repeating);
	}

	private <V> CmfAttribute<V> loadAttribute(CmfType objectType, CmfAttributeTranslator<V> translator, ResultSet rs)
		throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = translator.decodeAttributeName(objectType, rs.getString("name"));
		CmfDataType type = translator.decodeValue(rs.getString("data_type"));
		boolean repeating = rs.getBoolean("repeating") && !rs.wasNull();
		return new CmfAttribute<>(name, type, repeating);
	}

	private <V> void loadValues(CmfValueCodec<V> codec, CmfValueSerializer deserializer, ResultSet rs,
		CmfProperty<V> property) throws SQLException, CmfStorageException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		List<V> values = new LinkedList<>();
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
		List<CmfAttribute<V>> attributes = new LinkedList<>();
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		while (rs.next()) {
			attributes.add(loadAttribute(obj.getType(), translator, rs));
		}
		obj.setAttributes(attributes);
	}

	private <V> void loadProperties(CmfAttributeTranslator<V> translator, ResultSet rs, CmfObject<V> obj)
		throws SQLException {
		List<CmfProperty<V>> properties = new LinkedList<>();
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
	protected boolean lockForStorage(JdbcOperation operation, CmfObjectRef target, CmfObjectRef referrent)
		throws CmfStorageException {
		// TODO: Add support for re-entrancy by only allowing a lock to be re-obtained if the same
		// object is being locked while being referenced by the same referrent. A "none" referrent
		// (null referrent values) is supported.
		final Connection c = operation.getConnection();
		QueryRunner qr = JdbcTools.getQueryRunner();
		final String dbid = JdbcTools.composeDatabaseId(target);
		Savepoint savePoint = null;
		try {
			savePoint = c.setSavepoint();
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("ATTEMPTING TO PERSIST DEPENDENCY %s", target.getShortLabel()));
			}
			qr.insert(c, translateQuery(JdbcDialect.Query.INSERT_EXPORT_PLAN), JdbcTools.HANDLER_NULL,
				target.getType().name(), dbid);
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("PERSISTED DEPENDENCY %s", target.getShortLabel()));
			}
			savePoint = commitSavepoint(c, savePoint);
			return true;
		} catch (SQLException e) {
			if (this.dialect.isDuplicateKeyException(e)) {
				// We're good! With the use of savepoints, the transaction will remain valid and
				// thus we'll be OK to continue using the transaction in other connections
				rollbackSavepoint(c, savePoint);
				if (this.log.isTraceEnabled()) {
					this.log.trace(String.format("DUPLICATE DEPENDENCY %s", target.getShortLabel()));
				}
				return false;
			}
			throw new CmfStorageException(String.format("Failed to persist the dependency %s", target.getShortLabel()),
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
		List<Object[]> contents = new ArrayList<>();
		List<Object[]> properties = new ArrayList<>();
		Object[] cArr = new Object[9];
		Object[] pArr = new Object[6];
		int pos = 0;
		for (CmfContentInfo i : content) {
			// First, the content record...
			cArr[0] = objectId;
			cArr[1] = i.getRenditionIdentifier();
			cArr[2] = i.getRenditionPage();
			cArr[3] = i.getModifier();
			cArr[4] = i.getExtension();
			cArr[5] = pos++;
			cArr[6] = i.getLength();
			cArr[7] = Tools.toString(Tools.coalesce(i.getMimeType(), MimeTools.DEFAULT_MIME_TYPE));
			cArr[8] = i.getFileName();
			contents.add(cArr.clone());

			// Then, the properties...
			pArr[0] = objectId;
			pArr[1] = i.getRenditionIdentifier();
			pArr[2] = i.getRenditionPage();
			pArr[3] = i.getModifier();
			for (String s : i.getPropertyNames()) {
				if (s == null) {
					continue;
				}
				pArr[4] = s;
				pArr[5] = i.getProperty(s);
				if (pArr[5] == null) {
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
					final Map<String, String> ret = new TreeMap<>();
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
					final List<CmfContentInfo> ret = new ArrayList<>();
					final QueryRunner qr = new QueryRunner();
					while (rs.next()) {
						final CmfContentInfo info = new CmfContentInfo(rs.getString("rendition_id"),
							rs.getInt("rendition_page"), rs.getString("modifier"));
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
							info.getRenditionIdentifier(), info.getRenditionPage(), info.getModifier());
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
	protected void resetAltNames(JdbcOperation operation) throws CmfStorageException {
		final Connection c = operation.getConnection();
		QueryRunner qr = JdbcTools.getQueryRunner();
		try {
			int count = qr.update(c, translateQuery(JdbcDialect.Query.RESET_ALT_NAME));
			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Reset %d alternate name mappings", count));
			}
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to reset the alt names table", e);
		}
	}

	@Override
	protected <V> Collection<CmfObject<V>> getObjectsWithFileNameCollisions(JdbcOperation operation,
		CmfAttributeTranslator<V> translator) throws CmfStorageException {

		final Connection c = operation.getConnection();
		final Map<CmfType, Collection<String>> bucket = new EnumMap<>(CmfType.class);
		final QueryRunner qr = JdbcTools.getQueryRunner();
		final AtomicInteger counter = new AtomicInteger(0);
		try {
			qr.query(c, translateQuery(JdbcDialect.Query.LOAD_NAME_COLLISIONS), new ResultSetHandler<Void>() {
				@Override
				public Void handle(ResultSet rs) throws SQLException {
					while (rs.next()) {
						final String objectId = rs.getString("object_id");
						if (rs.wasNull()) {
							continue;
						}
						CmfObjectRef ref = JdbcTools.decodeDatabaseId(objectId);
						Collection<String> c = bucket.get(ref.getType());
						if (c == null) {
							c = new HashSet<>();
							bucket.put(ref.getType(), c);
						}
						c.add(ref.getId());
						counter.incrementAndGet();
					}
					return null;
				}
			});
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to load the name collision records", e);
		}

		final List<CmfObject<V>> ret = new ArrayList<>(counter.get());
		for (CmfType type : bucket.keySet()) {
			Collection<String> ids = bucket.get(type);
			loadObjects(operation, translator, type, ids, new DefaultCmfObjectHandler<V>() {
				@Override
				public boolean handleObject(CmfObject<V> dataObject) throws CmfStorageException {
					ret.add(dataObject);
					return super.handleObject(dataObject);
				}
			});
		}

		Collections.sort(ret, new Comparator<CmfObject<V>>() {
			@Override
			public int compare(CmfObject<V> a, CmfObject<V> b) {
				if (a == b) { return 0; }
				if (a == null) { return -1; }
				if (b == null) { return 1; }

				int aP = a.getParentReferences().size();
				int bP = b.getParentReferences().size();
				if (aP != bP) { return Tools.compare(aP, bP); }

				long aN = a.getNumber();
				long bN = b.getNumber();

				// This is reverse-ordered, earliest object last
				if (aN != bN) { return (Tools.compare(aN, bN) * -1); }
				return 0;
			}
		});

		return ret;
	}

	protected boolean disableReferentialIntegrity(JdbcOperation operation) throws CmfStorageException {
		String sql = translateOptionalQuery(JdbcDialect.Query.DISABLE_REFERENTIAL_INTEGRITY);
		if (sql == null) { return false; }
		Connection c = operation.getConnection();
		Statement s = null;
		Savepoint savePoint = null;
		try {
			s = c.createStatement();
			savePoint = c.setSavepoint();
			s.execute(sql);
			savePoint = commitSavepoint(c, savePoint);
			return true;
		} catch (SQLException e) {
			rollbackSavepoint(c, savePoint);
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

	@Override
	protected void clearTargetCache(JdbcOperation operation) throws CmfStorageException {
		Connection c = operation.getConnection();
		try {
			JdbcTools.getQueryRunner().update(c, translateQuery(JdbcDialect.Query.CLEAR_TARGET_CACHE));
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to clear the target cache", e);
		}
	}

	@Override
	protected void cacheTargets(JdbcOperation operation, Collection<CmfObjectSpec> targets) throws CmfStorageException {
		Object[] arr = new Object[3];
		Collection<Object[]> cacheTargets = new ArrayList<>(targets.size());
		for (CmfObjectSpec spec : targets) {
			arr[0] = (spec.getType() != null ? spec.getType().name() : null);
			arr[1] = spec.getId();
			arr[2] = spec.getSearchKey();
			cacheTargets.add(arr.clone());
		}
		QueryRunner qr = JdbcTools.getQueryRunner();
		try {
			qr.insertBatch(operation.getConnection(), translateQuery(JdbcDialect.Query.INSERT_CACHE_TARGET),
				JdbcTools.HANDLER_NULL, cacheTargets.toArray(JdbcTools.NO_PARAMS));
		} catch (SQLException e) {
			throw new CmfStorageException("Exception caught inserting the cache targets", e);
		}
	}

	private static final class CachedTargetState {
		private final Statement statement;
		private final ResultSet results;
		private Boolean nextAvailable = null;

		private CachedTargetState(ResultSet results) throws SQLException {
			this.results = results;
			this.statement = results.getStatement();
		}

		private boolean hasNext() throws SQLException {
			if (this.nextAvailable == null) {
				// Need to fetch the next row
				this.nextAvailable = this.results.next();
			}
			return this.nextAvailable.booleanValue();
		}

		private static CachedTargetState convert(Object o) throws CmfStorageException {
			if (o == null) { throw new CmfStorageException("No object to check against"); }
			if (!CachedTargetState.class
				.isInstance(o)) { throw new CmfStorageException("Invalid state - not a ResultSet"); }
			return CachedTargetState.class.cast(o);
		}

		private void close() {
			DbUtils.closeQuietly(this.results);
			DbUtils.closeQuietly(this.statement);
		}
	}

	@Override
	protected Object getCachedTargets(JdbcOperation operation) throws CmfStorageException {
		try {
			return new CachedTargetState(operation.getConnection()
				.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ALL_CACHE_TARGETS)).executeQuery());
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to fetch all the cached targets", e);
		}
	}

	@Override
	protected boolean hasNextCachedTarget(Object state) throws CmfStorageException {
		try {
			return CachedTargetState.convert(state).hasNext();
		} catch (SQLException e) {
			throw new CmfStorageException(
				"Failed to retrieve the statement associated with the cached targets result set", e);
		}
	}

	@Override
	protected CmfObjectSpec getNextCachedTarget(Object state) throws CmfStorageException {
		CachedTargetState cacheState = CachedTargetState.convert(state);
		try {
			if (!cacheState.hasNext()) { throw new NoSuchElementException("No row to retrieve"); }
			// If we're here, then next() has already been called and returned true, so we can
			// safely begin to read off the ResultSet
			final ResultSet rs = cacheState.results;
			String type = rs.getString("object_type");
			if (rs.wasNull()) {
				type = null;
			}
			String id = rs.getString("object_id");
			if (rs.wasNull()) {
				id = null;
			}
			String searchKey = rs.getString("search_key");
			if (rs.wasNull()) {
				searchKey = null;
			}
			try {
				return new CmfObjectSpec(type != null ? CmfType.valueOf(type) : null, id, searchKey);
			} catch (Exception e) {
				throw new CmfStorageException(String.format("Illegal CmfType value [%s]", type), e);
			} finally {
				// Mark the row as fetched so next() needs to be invoked again
				cacheState.nextAvailable = null;
			}
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to retrieve the next cached target in the given result set", e);
		}
	}

	@Override
	protected void closeCachedTargets(Object state) throws CmfStorageException {
		CachedTargetState.convert(state).close();
	}

	private void rollbackSavepoint(Connection c, Savepoint savePoint) {
		if (savePoint == null) { return; }
		try {
			c.rollback(savePoint);
		} catch (SQLException e) {
			this.log.trace("Failed to roll back to the established SavePoint", e);
		}
	}

	private Savepoint commitSavepoint(Connection c, Savepoint savePoint) {
		if (savePoint == null) { return null; }
		try {
			c.releaseSavepoint(savePoint);
			return null;
		} catch (SQLException e) {
			this.log.trace("Failed to roll back to the established SavePoint", e);
			return savePoint;
		}
	}
}