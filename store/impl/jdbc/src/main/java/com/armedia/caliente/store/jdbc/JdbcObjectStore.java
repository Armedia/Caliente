/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
/**
 *
 */

package com.armedia.caliente.store.jdbc;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfNameFixer;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectHandler;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfOperationException;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfRequirementInfo;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueSerializer;
import com.armedia.caliente.store.tools.MimeTools;
import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.TriConsumer;

/**
 *
 *
 */
public class JdbcObjectStore extends CmfObjectStore<JdbcOperation> {

	private static final String PROPERTY_TABLE = "cmf_info";
	private static final String SCHEMA_CHANGE_LOG = "metadata.changelog.xml";

	private static final String NULL = "{NULL-VALUE}";

	private final boolean managedTransactions;
	private final DataSourceDescriptor<?> dataSourceDescriptor;
	private final DataSource dataSource;
	private final Connection lastConnection;
	private final JdbcStorePropertyManager propertyManager;
	private final JdbcDialect dialect;
	private final Map<JdbcDialect.Query, String> queries;
	private final CfgTools cfg;

	public JdbcObjectStore(CmfStore<?> parent, DataSourceDescriptor<?> dataSourceDescriptor, boolean updateSchema,
		boolean cleanData, CfgTools cfg) throws CmfStorageException {
		super(parent, JdbcOperation.class, true);
		if (dataSourceDescriptor == null) {
			throw new IllegalArgumentException("Must provide a valid DataSource instance");
		}
		this.dataSourceDescriptor = dataSourceDescriptor;
		this.managedTransactions = dataSourceDescriptor.isManagedTransactions();
		this.dataSource = dataSourceDescriptor.getDataSource();
		this.cfg = cfg;

		Connection c = null;
		try {
			c = this.dataSource.getConnection();
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to get a SQL Connection to validate the schema", e);
		}

		boolean closeOnFinally = true;
		try {
			try {
				this.dialect = JdbcDialect.getDialect(c.getMetaData());
			} catch (SQLException e) {
				throw new CmfStorageException("Failed to initialize the query resolver", e);
			}

			if (this.dialect.isShutdownOnLastConnectionClose()) {
				this.lastConnection = c;
				closeOnFinally = false;
			} else {
				this.lastConnection = null;
			}

			Map<JdbcDialect.Query, String> queries = new EnumMap<>(JdbcDialect.Query.class);
			for (JdbcDialect.Query q : JdbcDialect.Query.values()) {
				String v = this.dialect.translateQuery(q);
				if (StringUtils.isEmpty(v)) {
					continue;
				}
				queries.put(q, v);
			}
			this.queries = Tools.freezeMap(queries);

			this.propertyManager = new JdbcStorePropertyManager(JdbcObjectStore.PROPERTY_TABLE);

			JdbcOperation op = new JdbcOperation(c, this.managedTransactions, true);
			boolean ok = false;
			op.begin();
			try {
				JdbcSchemaManager.prepareSchema(JdbcObjectStore.SCHEMA_CHANGE_LOG, op, updateSchema,
					this.managedTransactions, (o) -> {
						if (cleanData) {
							clearAllProperties(o);
							clearAllObjects(o);
						}
						clearAttributeMappings(o);
						clearBulkObjectLoaderFilter(o);
					});
				op.commit();
				ok = true;
			} finally {
				if (!ok) {
					try {
						op.rollback();
					} catch (CmfOperationException e) {
						this.log.warn("Rollback failed during schema preparation (dialect = {})", this.dialect, e);
					}
				}
			}
		} finally {
			// Only close the last connection if it's not null
			if (closeOnFinally) {
				JdbcTools.closeQuietly(c);
			}
		}
	}

	@Override
	public File getStoreLocation() {
		return Tools.canonicalize(new File(this.cfg.getString("dir.metadata")));
	}

	protected final DataSourceDescriptor<?> getDataSourceDescriptor() {
		return this.dataSourceDescriptor;
	}

	@Override
	protected Long storeObject(JdbcOperation operation, CmfObject<CmfValue> object) throws CmfStorageException {
		final Connection c = operation.getConnection();
		final CmfObject.Archetype objectType = object.getType();
		final String objectId = JdbcTools.composeDatabaseId(object);

		Collection<Object[]> attributeParameters = new ArrayList<>();
		Collection<Object[]> attributeValueParameters = new ArrayList<>();
		Collection<Object[]> propertyParameters = new ArrayList<>();
		Collection<Object[]> propertyValueParameters = new ArrayList<>();
		Collection<Object[]> parentParameters = new ArrayList<>();
		Collection<Object[]> secondariesParameters = new ArrayList<>();
		Object[] attData = new Object[7];
		Object[] attValue = new Object[5];
		Object[] propData = new Object[4];
		Object[] parentData = new Object[3];
		Object[] secondariesData = new Object[3];

		try {
			QueryRunner qr = JdbcTools.getQueryRunner();

			// First, the secondary subtypes
			secondariesData[0] = objectId;
			int i = 0;
			for (String s : object.getSecondarySubtypes()) {
				s = StringUtils.strip(s);
				if (StringUtils.isEmpty(s)) {
					continue;
				}
				secondariesData[1] = i;
				secondariesData[2] = s;
				secondariesParameters.add(secondariesData.clone());
				i++;
			}

			// Then, set up the parents
			parentData[0] = objectId;
			i = 0;
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
			for (final CmfAttribute<CmfValue> attribute : object.getAttributes()) {
				final String name = attribute.getName();
				final String duplicate = encodedNames.put(name, attribute.getName());
				if (duplicate != null) {
					this.log.warn(
						"Duplicate encoded attribute name [{}] resulted from encoding [{}] (previous encoding came from [{}])",
						name, attribute.getName(), duplicate);
					continue;
				}
				final boolean multivalued = attribute.isMultivalued();
				final String type = attribute.getType().name();

				attData[1] = name;
				attData[2] = name;
				attData[3] = type;
				attData[6] = multivalued;

				// Insert the attribute
				attributeParameters.add(attData.clone());

				if (attribute.getValueCount() <= 0) {
					continue;
				}

				attValue[1] = name; // This never changes inside this next loop
				int v = 0;
				// No special treatment, simply dump out all the values
				final CmfValueSerializer serializer = CmfValueSerializer.get(attribute.getType());
				if (serializer != null) {
					for (CmfValue value : attribute) {
						attValue[2] = v;
						if ((value != null) && !value.isNull()) {
							attValue[3] = false;
							try {
								attValue[4] = serializer.serialize(value);
							} catch (ParseException e) {
								throw new CmfStorageException(
									String.format("Failed to encode value #%d for attribute [%s::%s]: %s", v,
										attValue[0], attValue[1], value),
									e);
							}
						} else {
							attValue[3] = true;
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
			for (final CmfProperty<CmfValue> property : object.getProperties()) {
				final String name = property.getName();
				final String duplicate = encodedNames.put(name, property.getName());
				if (duplicate != null) {
					this.log.warn(
						"Duplicate encoded property name [{}] resulted from encoding [{}] (previous encoding came from [{}])",
						name, property.getName(), duplicate);
					continue;
				}
				final String type = property.getType().name();

				propData[1] = name;
				propData[2] = type;
				propData[3] = property.isMultivalued();

				// Insert the attribute
				propertyParameters.add(propData.clone());

				attValue[1] = name; // This never changes inside this next loop
				int v = 0;
				// No special treatment, simply dump out all the values
				final CmfValueSerializer serializer = CmfValueSerializer.get(property.getType());
				if (serializer != null) {
					for (CmfValue value : property) {
						attValue[2] = v;
						attValue[3] = value.isNull();
						if (!value.isNull()) {
							try {
								attValue[4] = serializer.serialize(value);
							} catch (ParseException e) {
								throw new CmfStorageException(
									String.format("Failed to encode value #%d for property [%s::%s]: %s", v,
										attValue[0], attValue[1], value),
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
				object.getDependencyTier(), object.getHistoryId(), object.isHistoryCurrent());
			qr.insertBatch(c, translateQuery(JdbcDialect.Query.INSERT_OBJECT_SECONDARIES), JdbcTools.HANDLER_NULL,
				secondariesParameters.toArray(JdbcTools.NO_PARAMS));
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
			this.log.debug("Stored object #{}: {}", ret, object);
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
	protected CmfObject<CmfValue> loadHeadObject(JdbcOperation operation, CmfObject.Archetype type, String historyId)
		throws CmfStorageException {
		final Connection connection = operation.getConnection();
		try {
			PreparedStatement objectPS = null;
			PreparedStatement secondariesPS = null;
			PreparedStatement parentsPS = null;
			PreparedStatement attributePS = null;
			PreparedStatement attributeValuePS = null;
			PreparedStatement propertyPS = null;
			PreparedStatement propertyValuePS = null;
			try {
				objectPS = connection
					.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_OBJECT_HISTORY_CURRENT_BY_HISTORY_ID));
				secondariesPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_SECONDARIES));
				parentsPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PARENT_IDS));
				attributePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ATTRIBUTES));
				attributeValuePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ATTRIBUTE_VALUES));
				propertyPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PROPERTIES));
				propertyValuePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PROPERTY_VALUES));

				ResultSet objectRS = null;
				ResultSet secondariesRS = null;
				ResultSet parentsRS = null;
				ResultSet attributeRS = null;
				ResultSet propertyRS = null;
				ResultSet valueRS = null;

				objectPS.setString(1, type.name());
				objectPS.setString(2, historyId);
				objectRS = objectPS.executeQuery();
				try {
					while (objectRS.next()) {
						final CmfObject<CmfValue> obj;
						try {
							final int objNum = objectRS.getInt("object_number");
							final String objId = objectRS.getString("object_id");
							final String objLabel = objectRS.getString("object_label");

							this.log.info("De-serializing {} object #{} [{}]({})", type, objNum, objLabel, objId);

							secondariesPS.setString(1, objId);
							secondariesRS = secondariesPS.executeQuery();

							parentsPS.setString(1, objId);
							parentsRS = parentsPS.executeQuery();

							obj = loadObject(objectRS, parentsRS, secondariesRS);
							if (this.log.isTraceEnabled()) {
								this.log.trace("De-serialized {} object #{}: {}", type, objNum, obj);
							} else {
								this.log.debug("De-serialized {} object #{} [{}]({})", type, objNum, objLabel, objId);
							}

							attributePS.clearParameters();
							attributePS.setString(1, objId);
							attributeRS = attributePS.executeQuery();
							try {
								loadAttributes(attributeRS, obj);
							} finally {
								JdbcTools.closeQuietly(attributeRS);
							}

							attributeValuePS.clearParameters();
							attributeValuePS.setString(1, objId);
							for (CmfAttribute<CmfValue> att : obj.getAttributes()) {
								attributeValuePS.setString(2, att.getName());
								valueRS = attributeValuePS.executeQuery();
								try {
									loadValues(CmfValueSerializer.get(att.getType()), valueRS, att);
								} finally {
									JdbcTools.closeQuietly(valueRS);
								}
							}

							propertyPS.clearParameters();
							propertyPS.setString(1, objId);
							propertyRS = propertyPS.executeQuery();
							try {
								loadProperties(propertyRS, obj);
							} finally {
								JdbcTools.closeQuietly(propertyRS);
							}

							propertyValuePS.clearParameters();
							propertyValuePS.setString(1, objId);
							for (CmfProperty<CmfValue> prop : obj.getProperties()) {
								propertyValuePS.setString(2, prop.getName());
								valueRS = propertyValuePS.executeQuery();
								try {
									loadValues(CmfValueSerializer.get(prop.getType()), valueRS, prop);
								} finally {
									JdbcTools.closeQuietly(valueRS);
								}
							}
						} catch (SQLException e) {
							throw handlerExceptionUnhandled(e);
						}
						return obj;
					}
					return null;
				} finally {
					JdbcTools.closeQuietly(parentsRS);
					JdbcTools.closeQuietly(secondariesRS);
					JdbcTools.closeQuietly(objectRS);
				}
			} finally {
				JdbcTools.closeQuietly(propertyValuePS);
				JdbcTools.closeQuietly(propertyPS);
				JdbcTools.closeQuietly(attributeValuePS);
				JdbcTools.closeQuietly(attributePS);
				JdbcTools.closeQuietly(parentsPS);
				JdbcTools.closeQuietly(secondariesPS);
				JdbcTools.closeQuietly(objectPS);
			}
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Exception raised trying to deserialize objects of type [%s]", type), e);
		}
	}

	@Override
	protected CmfObject<CmfValue> loadLatestObject(JdbcOperation operation, CmfObject.Archetype type, String historyId)
		throws CmfStorageException {
		final Connection connection = operation.getConnection();
		try {
			PreparedStatement objectPS = null;
			PreparedStatement secondariesPS = null;
			PreparedStatement parentsPS = null;
			PreparedStatement attributePS = null;
			PreparedStatement attributeValuePS = null;
			PreparedStatement propertyPS = null;
			PreparedStatement propertyValuePS = null;
			try {
				objectPS = connection
					.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_OBJECT_HISTORY_LATEST_BY_HISTORY_ID));
				secondariesPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_SECONDARIES));
				parentsPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PARENT_IDS));
				attributePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ATTRIBUTES));
				attributeValuePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ATTRIBUTE_VALUES));
				propertyPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PROPERTIES));
				propertyValuePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PROPERTY_VALUES));

				ResultSet objectRS = null;
				ResultSet secondariesRS = null;
				ResultSet parentsRS = null;
				ResultSet attributeRS = null;
				ResultSet propertyRS = null;
				ResultSet valueRS = null;

				objectPS.setString(1, type.name());
				objectPS.setString(2, historyId);
				objectRS = objectPS.executeQuery();
				try {
					while (objectRS.next()) {
						final CmfObject<CmfValue> obj;
						try {
							final int objNum = objectRS.getInt("object_number");
							final String objId = objectRS.getString("object_id");
							final String objLabel = objectRS.getString("object_label");

							this.log.info("De-serializing {} object #{} [{}]({})", type, objNum, objLabel, objId);

							secondariesPS.setString(1, objId);
							secondariesRS = secondariesPS.executeQuery();

							parentsPS.setString(1, objId);
							parentsRS = parentsPS.executeQuery();

							obj = loadObject(objectRS, parentsRS, secondariesRS);
							if (this.log.isTraceEnabled()) {
								this.log.trace("De-serialized {} object #{}: {}", type, objNum, obj);
							} else {
								this.log.debug("De-serialized {} object #{} [{}]({})", type, objNum, objLabel, objId);
							}

							attributePS.clearParameters();
							attributePS.setString(1, objId);
							attributeRS = attributePS.executeQuery();
							try {
								loadAttributes(attributeRS, obj);
							} finally {
								JdbcTools.closeQuietly(attributeRS);
							}

							attributeValuePS.clearParameters();
							attributeValuePS.setString(1, objId);
							for (CmfAttribute<CmfValue> att : obj.getAttributes()) {
								attributeValuePS.setString(2, att.getName());
								valueRS = attributeValuePS.executeQuery();
								try {
									loadValues(CmfValueSerializer.get(att.getType()), valueRS, att);
								} finally {
									JdbcTools.closeQuietly(valueRS);
								}
							}

							propertyPS.clearParameters();
							propertyPS.setString(1, objId);
							propertyRS = propertyPS.executeQuery();
							try {
								loadProperties(propertyRS, obj);
							} finally {
								JdbcTools.closeQuietly(propertyRS);
							}

							propertyValuePS.clearParameters();
							propertyValuePS.setString(1, objId);
							for (CmfProperty<CmfValue> prop : obj.getProperties()) {
								propertyValuePS.setString(2, prop.getName());
								valueRS = propertyValuePS.executeQuery();
								try {
									loadValues(CmfValueSerializer.get(prop.getType()), valueRS, prop);
								} finally {
									JdbcTools.closeQuietly(valueRS);
								}
							}
						} catch (SQLException e) {
							throw handlerExceptionUnhandled(e);
						}
						return obj;
					}
					return null;
				} finally {
					JdbcTools.closeQuietly(parentsRS);
					JdbcTools.closeQuietly(secondariesRS);
					JdbcTools.closeQuietly(objectRS);
				}
			} finally {
				JdbcTools.closeQuietly(propertyValuePS);
				JdbcTools.closeQuietly(propertyPS);
				JdbcTools.closeQuietly(attributeValuePS);
				JdbcTools.closeQuietly(attributePS);
				JdbcTools.closeQuietly(parentsPS);
				JdbcTools.closeQuietly(secondariesPS);
				JdbcTools.closeQuietly(objectPS);
			}
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Exception raised trying to deserialize objects of type [%s]", type), e);
		}
	}

	@Override
	protected int loadObjects(JdbcOperation operation, final CmfObject.Archetype type, Collection<String> ids,
		CmfObjectHandler<CmfValue> handler) throws CmfStorageException {
		return loadObjects(operation, type, handler, ids, false);
	}

	protected int loadObjects(JdbcOperation operation, final CmfObject.Archetype type,
		CmfObjectHandler<CmfValue> handler, Collection<String> ids, boolean useHistoryIds) throws CmfStorageException {
		// If we're retrieving by IDs and no IDs have been given, don't waste time or resources
		if ((ids != null) && ids.isEmpty()) { return 0; }

		Connection connection = operation.getConnection();
		try {
			PreparedStatement objectPS = null;
			PreparedStatement secondariesPS = null;
			PreparedStatement parentsPS = null;
			PreparedStatement attributePS = null;
			PreparedStatement attributeValuePS = null;
			PreparedStatement propertyPS = null;
			PreparedStatement propertyValuePS = null;
			try {
				boolean limitByIDs = false;
				if (ids == null) {
					if (isObjectFilterActive()) {
						objectPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_FILTERED_OBJECTS));
					} else {
						objectPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_OBJECTS));
					}
				} else {
					limitByIDs = true;
					final JdbcDialect.Query query = (useHistoryIds //
						? JdbcDialect.Query.LOAD_OBJECTS_BY_HISTORY_ID
						: JdbcDialect.Query.LOAD_OBJECTS_BY_ID //
					);
					objectPS = connection.prepareStatement(translateQuery(query));
				}

				secondariesPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_SECONDARIES));
				parentsPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PARENT_IDS));
				attributePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ATTRIBUTES));
				attributeValuePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_ATTRIBUTE_VALUES));
				propertyPS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PROPERTIES));
				propertyValuePS = connection.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_PROPERTY_VALUES));

				ResultSet objectRS = null;
				ResultSet secondariesRS = null;
				ResultSet parentsRS = null;
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
					if (this.dialect.isSupportsArrays()) {
						objectPS.setString(1, type.name());
						objectPS.setArray(2, connection.createArrayOf("varchar", arr));
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
						final CmfObject<CmfValue> obj;
						try {
							final int objNum = objectRS.getInt("object_number");
							final int tierId = objectRS.getInt("tier_id");
							String historyId = objectRS.getString("history_id");
							if ((historyId == null) || objectRS.wasNull()) {
								historyId = String.format("%08x", objNum);
							}

							// Do the history checking
							if (!Objects.equals(currentHistory, historyId)) {
								if (currentHistory != null) {
									this.log.debug("CLOSE HISTORY: {}", currentHistory);
									if (!handler.endHistory(currentHistory, true)) {
										this.log.warn("{} history [{}] requested processing cancellation", type.name(),
											historyId);
										currentHistory = null;
										break;
									}
								}

								// Do the tier checking
								if (!Objects.equals(currentTier, tierId)) {
									if (currentTier != null) {
										this.log.debug("CLOSE TIER: {}", currentTier);
										if (!handler.endTier(currentTier, true)) {
											this.log.warn("{} tier [{}] requested processing cancellation", type.name(),
												tierId);
											currentTier = null;
											break;
										}
									}

									this.log.debug("NEW TIER: {}", tierId);
									if (!handler.newTier(tierId)) {
										this.log.warn("{} tier [{}] skipped", type.name(), tierId);
										continue;
									}
									currentTier = tierId;
								}

								this.log.debug("NEW HISTORY: {}", historyId);
								if (!handler.newHistory(historyId)) {
									this.log.warn("{} history [{}] skipped", type.name(), historyId);
									continue;
								}
								currentHistory = historyId;
							}

							final String objId = objectRS.getString("object_id");
							final String objLabel = objectRS.getString("object_label");

							this.log.info("De-serializing {} object #{} [{}]({})", type, objNum, objLabel, objId);

							secondariesPS.setString(1, objId);
							secondariesRS = secondariesPS.executeQuery();

							parentsPS.setString(1, objId);
							parentsRS = parentsPS.executeQuery();

							obj = loadObject(objectRS, parentsRS, secondariesRS);
							if (this.log.isTraceEnabled()) {
								this.log.trace("De-serialized {} object #{}: {}", type, objNum, obj);
							} else {
								this.log.debug("De-serialized {} object #{} [{}]({})", type, objNum, objLabel, objId);
							}

							attributePS.clearParameters();
							attributePS.setString(1, objId);
							attributeRS = attributePS.executeQuery();
							try {
								loadAttributes(attributeRS, obj);
							} finally {
								JdbcTools.closeQuietly(attributeRS);
							}

							attributeValuePS.clearParameters();
							attributeValuePS.setString(1, objId);
							for (CmfAttribute<CmfValue> att : obj.getAttributes()) {
								// We need to re-encode, since that's the value that will be
								// referenced in the DB
								attributeValuePS.setString(2, att.getName());
								valueRS = attributeValuePS.executeQuery();
								try {
									loadValues(CmfValueSerializer.get(att.getType()), valueRS, att);
								} finally {
									JdbcTools.closeQuietly(valueRS);
								}
							}

							propertyPS.clearParameters();
							propertyPS.setString(1, objId);
							propertyRS = propertyPS.executeQuery();
							try {
								loadProperties(propertyRS, obj);
							} finally {
								JdbcTools.closeQuietly(propertyRS);
							}

							propertyValuePS.clearParameters();
							propertyValuePS.setString(1, objId);
							for (CmfProperty<CmfValue> prop : obj.getProperties()) {
								// We need to re-encode, since that's the value that will be
								// referenced in the DB
								propertyValuePS.setString(2, prop.getName());
								valueRS = propertyValuePS.executeQuery();
								try {
									loadValues(CmfValueSerializer.get(prop.getType()), valueRS, prop);
								} finally {
									JdbcTools.closeQuietly(valueRS);
								}
							}
						} catch (SQLException e) {
							// TODO: WTF?!?! Why is it detecting this when it's clearly impossible?
							if (!handler.handleException(e)) { throw handlerExceptionUnhandled(e); }
							continue;
						} finally {
							// Just in case...
							JdbcTools.closeQuietly(valueRS);
						}

						try {
							if (!handler.handleObject(obj)) {
								this.log.debug("ObjectHandler requested load loop break on object: {}", obj);
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
							handler.endHistory(currentHistory, ok);
						} catch (CmfStorageException e) {
							this.log.error("Exception caught attempting to close the pending history [{}] (ok={})",
								currentHistory, ok, e);
						} finally {
							currentHistory = null;
						}
					}
					if (currentTier != null) {
						try {
							handler.endTier(currentTier, ok);
						} catch (CmfStorageException e) {
							this.log.error("Exception caught attempting to close the pending tier [{}] (ok={})",
								currentTier, ok, e);
						} finally {
							currentTier = null;
						}
					}
					JdbcTools.closeQuietly(parentsRS);
					JdbcTools.closeQuietly(secondariesRS);
					JdbcTools.closeQuietly(objectRS);
				}
			} finally {
				JdbcTools.closeQuietly(propertyValuePS);
				JdbcTools.closeQuietly(propertyPS);
				JdbcTools.closeQuietly(attributeValuePS);
				JdbcTools.closeQuietly(attributePS);
				JdbcTools.closeQuietly(parentsPS);
				JdbcTools.closeQuietly(secondariesPS);
				JdbcTools.closeQuietly(objectPS);
			}
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Exception raised trying to deserialize objects of type [%s]", type), e);
		}
	}

	private CmfStorageException handlerExceptionUnhandled(SQLException e) throws CmfStorageException {
		return new CmfStorageException(
			"Exception raised while loading objects - ObjectHandler did not handle the exception", e);
	}

	@Override
	protected int fixObjectNames(final JdbcOperation operation, final CmfNameFixer<CmfValue> nameFixer,
		CmfObject.Archetype type, Set<String> historyIds) throws CmfStorageException {
		final AtomicInteger result = new AtomicInteger(0);
		CmfObject.Archetype[] types = {
			type
		};
		if (type == null) {
			types = CmfObject.Archetype.values();
		}
		for (CmfObject.Archetype currentType : types) {
			if (!nameFixer.supportsType(currentType)) {
				continue;
			}

			loadObjects(operation, currentType, new CmfObjectHandler<CmfValue>() {
				@Override
				public boolean newTier(int tierNumber) throws CmfStorageException {
					return true;
				}

				@Override
				public boolean newHistory(String historyId) throws CmfStorageException {
					return true;
				}

				@Override
				public boolean handleObject(CmfObject<CmfValue> obj) throws CmfStorageException {
					final String oldName = obj.getName();
					final String newName = nameFixer.fixName(obj);
					if (!StringUtils.isEmpty(newName) && !Objects.equals(oldName, newName)) {
						renameObject(operation, obj, newName);
						result.incrementAndGet();
						try {
							nameFixer.nameFixed(obj, oldName, newName);
						} catch (Exception e) {
							// Just log it
							JdbcObjectStore.this.log.warn(
								"Exception caught while invoking the nameFixed() callback for {}", obj.getDescription(),
								e);
							if (nameFixer.handleException(e)) { return false; }
						}
					}
					return true;
				}

				@Override
				public boolean handleException(Exception e) {
					return false;
				}

				@Override
				public boolean endHistory(String historyId, boolean ok) throws CmfStorageException {
					return true;
				}

				@Override
				public boolean endTier(int tierNum, boolean ok) throws CmfStorageException {
					return true;
				}
			}, historyIds, true);

		}
		return result.get();
	}

	@Override
	protected void scanObjectTree(JdbcOperation operation,
		final TriConsumer<CmfObjectRef, CmfObjectRef, String> scanner) throws CmfStorageException {
		try {
			JdbcTools.getQueryRunner().query(operation.getConnection(),
				translateQuery(JdbcDialect.Query.SCAN_OBJECT_TREE), (rs) -> {
					while (rs.next()) {
						String objectId = rs.getString("object_id");
						if (rs.wasNull()) {
							continue;
						}
						String parentId = rs.getString("parent_id");
						if (rs.wasNull()) {
							continue;
						}
						String name = rs.getString("name");
						if (rs.wasNull()) {
							continue;
						}
						CmfObjectRef parent = JdbcTools.decodeDatabaseId(parentId);
						CmfObjectRef child = JdbcTools.decodeDatabaseId(objectId);
						try {
							scanner.accept(parent, child, name);
						} catch (Exception e) {
							throw new SQLException("Failed to scan through the object tree due to a scanner exception",
								e);
						}
					}
					return null;
				});
		} catch (SQLException e) {
			throw new CmfStorageException("Exception raised while scanning the object tree", e);
		}
	}

	@Override
	protected <VALUE> void renameObject(final JdbcOperation operation, final CmfObject<VALUE> object,
		final String newName) throws CmfStorageException {
		final Connection c = operation.getConnection();
		final QueryRunner qr = JdbcTools.getQueryRunner();
		final String objectId = JdbcTools.composeDatabaseId(object);
		try {
			qr.update(c, translateQuery(JdbcDialect.Query.UPSERT_ALT_NAME), objectId, newName);
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to insert a rename record for the %s %s from [%s] to [%s]",
					object.getType().name(), object.getId(), object.getName(), newName),
				e);
		}

	}

	/**
	 * <p>
	 * Assigns the given targetId as the new ID for the object with the given source ID
	 * </p>
	 *
	 */
	protected void createMapping(Connection c, CmfObject.Archetype type, String name, String sourceValue,
		String targetValue) throws SQLException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		final QueryRunner qr = JdbcTools.getQueryRunner();

		if ((targetValue == null) || (sourceValue == null)) {
			// Delete instead
			final String sql = (targetValue == null ? translateQuery(JdbcDialect.Query.DELETE_TARGET_MAPPING)
				: translateQuery(JdbcDialect.Query.DELETE_SOURCE_MAPPING));
			final String refValue = (targetValue == null ? sourceValue : targetValue);
			int count = qr.update(c, sql, type.name(), name, refValue);
			if (count > 0) {
				this.log.info("Deleted the mappings [{}/{}/{}->{}] : %d", type, name, sourceValue, targetValue, count);
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
			this.log.info("Established the mapping [{}/{}/{}->{}]", type, name, sourceValue, targetValue);
		} else if (this.log.isDebugEnabled()) {
			this.log.debug("The mapping [{}/{}/{}->{}] already exists", type, name, sourceValue, targetValue);
		}
	}

	@Override
	protected void createMapping(JdbcOperation operation, CmfObject.Archetype type, String name, String sourceValue,
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
	protected Collection<String> getMapping(JdbcOperation operation, boolean source, CmfObject.Archetype type,
		String name, String value) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (value == null) { throw new IllegalArgumentException("Must provide a value to search against"); }
		try {
			return JdbcTools.getQueryRunner().query(operation.getConnection(),
				translateQuery(source ? JdbcDialect.Query.FIND_TARGET_MAPPING : JdbcDialect.Query.FIND_SOURCE_MAPPING),
				(rs) -> {
					Collection<String> results = null;
					while (rs.next()) {
						String str = rs.getString(1);
						if ((str != null) && !rs.wasNull()) {
							if (results == null) {
								results = new ArrayList<>();
							}
							results.add(str);
						}
					}
					return results;
				}, type.name(), name, value);
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
			// If it does exist, then this one MUST return exactly one result.
			String result = qr.query(c, translateQuery(JdbcDialect.Query.GET_EXPORT_RESULT), (rs) -> {
				// If there is no record, then by definition there is no store status
				if (!rs.next()) { return null; }
				// If there's a record, the value may still be null...so check for it
				String v = rs.getString(1);
				return (rs.wasNull() ? null : v);
			}, dbId, target.getType().name());
			if (result == null) { return null; }
			return StoreStatus.valueOf(result);
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to check whether object %s was already serialized", target.getShortLabel()), e);
		}
	}

	@Override
	protected boolean markStoreStatus(JdbcOperation operation, CmfObjectRef target, StoreStatus status, String message)
		throws CmfStorageException {
		// First things first, we make sure the record exists. It doesn't matter if this operation
		// fails because we're only doing this to make sure there's something to update. This is
		// important in order to track failures that happen outside of the scope of an object being
		// exported - such as when preparing the session or checking for transaction availability
		lockForStorage(operation, target, null);

		final Connection c = operation.getConnection();
		QueryRunner qr = JdbcTools.getQueryRunner();
		final String dbid = JdbcTools.composeDatabaseId(target);
		try {
			// No existing status, so we can continue
			if (this.log.isTraceEnabled()) {
				this.log.trace("ATTEMPTING TO SET THE EXPORT RESULT TO [{}] FOR {}", status.name(),
					target.getShortLabel());
			}
			int result = qr.update(c, translateQuery(JdbcDialect.Query.UPDATE_EXPORT_RESULT), status.name(), message,
				target.getType().name(), dbid);
			if (result == 1) {
				if (this.log.isDebugEnabled()) {
					this.log.debug("SET THE EXPORT RESULT TO [{}] FOR {}", status.name(), target.getShortLabel());
				}
				return true;
			}

			if (result > 1) {
				throw new CmfStorageException(
					String.format("REFERENTIAL INTEGRITY IS NOT BEING ENFORCED: %d PLAN records for %s", result,
						target.getShortLabel()));
			}

			// If we've come this far, then we failed to set the status. This may or may not be a
			// problem...
			StoreStatus existing = getStoreStatus(operation, target);
			if (existing != null) {
				// If the existing status was already set, this isn't really a problem - we just
				// report the discrepancy if necessary, and return false...then move on
				if (existing != status) {
					this.log.warn("FAILED TO SET THE RESULT TO [{}] for {} ({} updated, existing={})", status.name(),
						target.getShortLabel(), result, existing.name());
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

	private Map<CmfObject.Archetype, Long> getStoredObjectTypes(Connection c) throws CmfStorageException {
		try {
			return JdbcTools.getQueryRunner().query(c, translateQuery(JdbcDialect.Query.LOAD_OBJECT_TYPES), (rs) -> {
				Map<CmfObject.Archetype, Long> ret = new EnumMap<>(CmfObject.Archetype.class);
				while (rs.next()) {
					String t = rs.getString(1);
					if ((t == null) || rs.wasNull()) {
						JdbcObjectStore.this.log.warn("NULL TYPE STORED IN DATABASE: [{}]", t);
						continue;
					}
					try {
						ret.put(CmfObject.Archetype.valueOf(t), rs.getLong(2));
					} catch (IllegalArgumentException e) {
						JdbcObjectStore.this.log.warn("UNSUPPORTED TYPE STORED IN DATABASE: [{}]", t);
						continue;
					}
				}
				return ret;
			});
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to retrieve the stored object types", e);
		}
	}

	@Override
	protected Map<CmfObject.Archetype, Long> getStoredObjectTypes(JdbcOperation operation) throws CmfStorageException {
		return getStoredObjectTypes(operation.getConnection());
	}

	private int clearAttributeMappings(Connection c) throws CmfStorageException {
		try {
			return JdbcTools.getQueryRunner().update(c, translateQuery(JdbcDialect.Query.CLEAR_ALL_MAPPINGS));
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to clear all the stored mappings", e);
		}
	}

	@Override
	protected int clearAttributeMappings(JdbcOperation operation) throws CmfStorageException {
		return clearAttributeMappings(operation.getConnection());
	}

	@Override
	protected Map<CmfObject.Archetype, Set<String>> getAvailableMappings(JdbcOperation operation)
		throws CmfStorageException {
		final Map<CmfObject.Archetype, Set<String>> ret = new EnumMap<>(CmfObject.Archetype.class);
		try {
			JdbcTools.getQueryRunner().query(operation.getConnection(),
				translateQuery(JdbcDialect.Query.LOAD_ALL_MAPPINGS), (rs) -> {
					CmfObject.Archetype currentType = null;
					Set<String> names = null;
					while (rs.next()) {
						final CmfObject.Archetype newType = CmfObject.Archetype.valueOf(rs.getString("object_type"));
						if (newType != currentType) {
							names = new TreeSet<>();
							ret.put(newType, names);
							currentType = newType;
						}
						names.add(rs.getString("name"));
					}
					return null;
				});
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to retrieve the declared mapping types and names", e);
		}
		return ret;
	}

	@Override
	protected Set<String> getAvailableMappings(JdbcOperation operation, CmfObject.Archetype type)
		throws CmfStorageException {
		final Set<String> ret = new TreeSet<>();
		try {
			JdbcTools.getQueryRunner().query(operation.getConnection(),
				translateQuery(JdbcDialect.Query.LOAD_TYPE_MAPPINGS), (rs) -> {
					while (rs.next()) {
						ret.add(rs.getString("name"));
					}
					return null;
				}, type.name());
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to retrieve the declared mapping names for type [%s]", type), e);
		}
		return ret;
	}

	@Override
	protected Map<String, String> getMappings(JdbcOperation operation, CmfObject.Archetype type, String name) {
		final Map<String, String> ret = new HashMap<>();
		try {
			JdbcTools.getQueryRunner().query(operation.getConnection(),
				translateQuery(JdbcDialect.Query.LOAD_TYPE_NAME_MAPPINGS), (rs) -> {
					while (rs.next()) {
						ret.put(rs.getString("source_value"), rs.getString("target_value"));
					}
					return null;
				}, type.name(), name);
		} catch (SQLException e) {
			throw new RuntimeException(
				String.format("Failed to retrieve the declared mappings for [%s::%s]", type, name), e);
		}
		return ret;
	}

	@Override
	protected final void clearAllObjects(JdbcOperation operation) throws CmfStorageException {
		// Allow for subclasses to implement optimized clearing operations
		if (optimizedClearAllObjects(operation)) {
			if (!this.dialect.isTruncateRestartsSequences()) {
				// If the truncation did not restart the sequences, do it manually
				resetSequences(operation.getConnection());
			}
			return;
		}
		// Can't do it quickly, so do it the hard way...
		try {
			clearAllObjects(operation.getConnection());
			resetSequences(operation.getConnection());
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
			JdbcTools.closeQuietly(rs);
		}
	}

	protected boolean truncateTables(JdbcOperation operation, String... tables) throws CmfStorageException {
		if ((tables == null) || (tables.length == 0)) { return false; }
		return truncateTables(operation, Arrays.asList(tables));
	}

	protected boolean truncateTables(JdbcOperation operation, Iterable<String> tables) throws CmfStorageException {
		if (tables == null) { return false; }
		final Connection c = operation.getConnection();
		Collection<String> truncated = new LinkedHashSet<>();
		Collection<String> remaining = new LinkedHashSet<>();
		for (String s : tables) {
			remaining.add(s);
		}
		try {
			final boolean bypassConstraints = this.dialect.isTruncateBypassesConstraints();
			final boolean constraintsDisabled;
			if (bypassConstraints) {
				constraintsDisabled = false;
			} else {
				constraintsDisabled = disableReferentialIntegrity(operation);
				if (!constraintsDisabled) {
					// If we couldn't turn off referential integrity, then we couldn't perform
					// the optimized clear
					return false;
				}
			}
			Statement s = c.createStatement();
			try {
				final String sqlFmt = translateQuery(JdbcDialect.Query.TRUNCATE_TABLE_FMT);
				for (String table : tables) {
					if (StringUtils.isBlank(table)) {
						continue;
					}
					if (!truncated.contains(table)) {
						s.executeUpdate(String.format(sqlFmt, table));
						truncated.add(table);
						remaining.remove(table);
					}
				}
				return true;
			} finally {
				if (constraintsDisabled) {
					enableReferentialIntegrity(operation);
				}
				DbUtils.close(s);
			}
		} catch (SQLException e) {
			if (this.log.isDebugEnabled()) {
				if (!truncated.isEmpty()) {
					this.log.info("Successfully truncated the tables {}", truncated);
				}
				if (!remaining.isEmpty()) {
					this.log.error("Failed to truncate the tables {}", remaining, e);
				}
			}
			return false;
		}
	}

	protected boolean optimizedClearAllObjects(JdbcOperation operation) throws CmfStorageException {
		final Connection c = operation.getConnection();
		Set<String> tables = null;
		try {
			tables = getCmfTables(c);
		} catch (SQLException e) {
			this.log.trace("Failed to retrieve the table list", e);
			return false;
		}
		return truncateTables(operation, tables);
	}

	private Set<String> listSequences(Connection c) throws SQLException {
		String sql = translateOptionalQuery(JdbcDialect.Query.LIST_SEQUENCES);
		if (sql == null) { return Collections.emptySet(); }
		QueryRunner qr = JdbcTools.getQueryRunner();
		return qr.query(c, sql, (rs) -> {
			Set<String> sequences = new TreeSet<>();
			while (rs.next()) {
				sequences.add(rs.getString(1));
			}
			return sequences;
		});
	}

	private void resetSequences(Connection c) throws CmfStorageException {
		// Find all the sequences, and reset them to their initial state
		try {
			String sql = translateOptionalQuery(JdbcDialect.Query.RESTART_SEQUENCE);
			if (sql == null) { return; }
			QueryRunner qr = JdbcTools.getQueryRunner();
			for (String sequence : listSequences(c)) {
				qr.update(c, String.format(sql, sequence));
			}
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to reset the sequences", e);
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

	private CmfObject<CmfValue> loadObject(ResultSet objectRS, ResultSet parentsRS, ResultSet secondariesRS)
		throws SQLException {
		if (objectRS == null) {
			throw new IllegalArgumentException("Must provide a ResultSet to load the structure from");
		}
		CmfObject.Archetype type = CmfObject.Archetype.valueOf(objectRS.getString("object_type"));
		String id = objectRS.getString("object_id");
		String name = objectRS.getString("object_name");
		String newName = objectRS.getString("new_name");
		if (!objectRS.wasNull()) {
			// If there's an alternate name, we use that instead
			name = newName;
		}
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

		Set<String> secondaries = new LinkedHashSet<>();
		while (secondariesRS.next()) {
			String secondary = secondariesRS.getString("name");
			if (secondariesRS.wasNull()) {
				continue;
			}
			secondaries.add(secondary);
		}
		if (secondaries.isEmpty()) {
			secondaries = Collections.emptySet();
		}

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

		return new CmfObject<>(CmfAttributeTranslator.CMFVALUE_TRANSLATOR, type, id, name, parentIds, searchKey, tierId,
			historyId, historyCurrent, label, subtype, secondaries, number);
	}

	private CmfProperty<CmfValue> loadProperty(CmfObject.Archetype objectType, ResultSet rs) throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = rs.getString("name");
		CmfValue.Type type = CmfValue.Type.valueOf(rs.getString("data_type"));
		boolean multivalued = rs.getBoolean("multivalued") && !rs.wasNull();
		return new CmfProperty<>(name, type, multivalued);
	}

	private <VALUE> CmfAttribute<CmfValue> loadAttribute(CmfObject.Archetype objectType, ResultSet rs)
		throws SQLException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the structure from"); }
		String name = rs.getString("name");
		CmfValue.Type type = CmfValue.Type.valueOf(rs.getString("data_type"));
		boolean multivalued = rs.getBoolean("multivalued") && !rs.wasNull();
		return new CmfAttribute<>(name, type, multivalued);
	}

	private void loadValues(CmfValueSerializer deserializer, ResultSet rs, CmfProperty<CmfValue> property)
		throws SQLException, CmfStorageException {
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		List<CmfValue> values = new LinkedList<>();
		while (rs.next()) {
			final boolean nullValue = rs.getBoolean("null_value");
			final String data = rs.getString("data");
			final CmfValue v;
			try {
				if (rs.wasNull() || (nullValue && JdbcObjectStore.NULL.equals(data))) {
					v = CmfValue.of(property.getType(), null);
				} else {
					v = deserializer.deserialize(data);
				}
			} catch (ParseException e) {
				throw new CmfStorageException(
					String.format("Failed to deserialize value [%s] as a %s", data, property.getType()), e);
			}
			values.add(v);
			if (!property.isMultivalued()) {
				break;
			}
		}
		property.setValues(values);
	}

	private <VALUE> void loadAttributes(ResultSet rs, CmfObject<CmfValue> obj) throws SQLException {
		List<CmfAttribute<CmfValue>> attributes = new LinkedList<>();
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		while (rs.next()) {
			attributes.add(loadAttribute(obj.getType(), rs));
		}
		obj.setAttributes(attributes);
	}

	private void loadProperties(ResultSet rs, CmfObject<CmfValue> obj) throws SQLException {
		List<CmfProperty<CmfValue>> properties = new LinkedList<>();
		if (rs == null) { throw new IllegalArgumentException("Must provide a ResultSet to load the values from"); }
		while (rs.next()) {
			properties.add(loadProperty(obj.getType(), rs));
		}
		obj.setProperties(properties);
	}

	@Override
	protected JdbcOperation newOperation(boolean exclusive) throws CmfStorageException {
		try {
			return new JdbcOperation(this.dataSource.getConnection(), this.managedTransactions, exclusive);
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to obtain a new connection from the datasource", e);
		}
	}

	@Override
	protected boolean lockHistory(JdbcOperation operation, CmfObject.Archetype type, String historyId, String lockId)
		throws CmfStorageException {
		final Connection c = operation.getConnection();
		QueryRunner qr = JdbcTools.getQueryRunner();
		Savepoint savePoint = null;
		try {
			savePoint = c.setSavepoint();
			this.log.trace("ATTEMPTING TO LOCK {} HISTORY {} WITH ID {}", type.name(), historyId, lockId);
			qr.insert(c, translateQuery(JdbcDialect.Query.INSERT_HISTORY_LOCK), JdbcTools.HANDLER_NULL, type.name(),
				historyId, lockId);
			if (this.log.isDebugEnabled()) {
				this.log.trace("LOCKED {} HISTORY {} WITH ID {}", type.name(), historyId, lockId);
			}
			savePoint = JdbcTools.commitSavepoint(c, savePoint);
			return true;
		} catch (SQLException e) {
			if (this.dialect.isDuplicateKeyException(e)) {
				// We're good! With the use of savepoints, the transaction will remain valid and
				// thus we'll be OK to continue using the transaction in other operations
				JdbcTools.rollbackSavepoint(c, savePoint);

				// Now, instead, we try to increase the counter
				try {
					int updated = qr.update(c, translateQuery(JdbcDialect.Query.UPDATE_HISTORY_LOCK_COUNTER),
						type.name(), historyId, lockId);
					if (updated == 1) {
						if (this.log.isTraceEnabled()) {
							this.log.trace("{} HISTORY {} IS LOCKED BY THIS SAME ID ({})", type.name(), historyId,
								lockId);
						}
						return true;
					}

					if (this.log.isTraceEnabled()) {
						this.log.trace("{} HISTORY {} IS ALREADY LOCKED WITH ANOTHER ID", type.name(), historyId);
					}
					return false;
				} catch (SQLException e2) {
					throw new CmfStorageException(String.format(
						"Could not verify the %s history lock for %s with ID %s", type.name(), historyId, lockId), e);
				}
			}
			throw new CmfStorageException(
				String.format("Failed to lock the %s history %s with ID %s", type.name(), historyId, lockId), e);
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
				this.log.trace("ATTEMPTING TO PERSIST DEPENDENCY {}", target.getShortLabel());
			}
			qr.insert(c, translateQuery(JdbcDialect.Query.INSERT_EXPORT_PLAN), JdbcTools.HANDLER_NULL,
				target.getType().name(), dbid);
			if (this.log.isDebugEnabled()) {
				this.log.debug("PERSISTED DEPENDENCY {}", target.getShortLabel());
			}
			savePoint = JdbcTools.commitSavepoint(c, savePoint);
			return true;
		} catch (SQLException e) {
			if (this.dialect.isDuplicateKeyException(e)) {
				// We're good! With the use of savepoints, the transaction will remain valid and
				// thus we'll be OK to continue using the transaction in other operations
				JdbcTools.rollbackSavepoint(c, savePoint);
				if (this.log.isTraceEnabled()) {
					this.log.trace("DUPLICATE DEPENDENCY {}", target.getShortLabel());
				}
				return false;
			}
			throw new CmfStorageException(
				String.format("Failed to persist the dependency %s (errorCode = %d, sqlState = '%s')",
					target.getShortLabel(), e.getErrorCode(), e.getSQLState()),
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
	protected void clearAllProperties(JdbcOperation operation) throws CmfStorageException {
		this.propertyManager.clearAllProperties(operation);
	}

	@Override
	protected <VALUE> void setContentStreams(JdbcOperation operation, CmfObject<VALUE> object,
		Collection<CmfContentStream> content) throws CmfStorageException {
		final Connection c = operation.getConnection();
		final String objectId = JdbcTools.composeDatabaseId(object);
		final QueryRunner qr = JdbcTools.getQueryRunner();

		// Step 1: Delete what's there
		try {
			qr.update(c, translateQuery(JdbcDialect.Query.DELETE_CONTENT), objectId);
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to delete the existing content records for %s", object.getDescription()), e);
		}

		// Step 2: prepare the new content records and properties
		List<Object[]> contents = new ArrayList<>();
		List<Object[]> properties = new ArrayList<>();
		Object[] cArr = new Object[10];
		Object[] pArr = new Object[4];
		for (CmfContentStream i : content) {
			// First, the content record...
			cArr[0] = objectId;
			cArr[1] = i.getIndex();
			cArr[2] = i.getRenditionIdentifier();
			cArr[3] = i.getRenditionPage();
			cArr[4] = i.getModifier();
			cArr[5] = i.getExtension();
			cArr[6] = i.getLength();
			cArr[7] = Tools.toString(Tools.coalesce(i.getMimeType(), MimeTools.DEFAULT_MIME_TYPE));
			cArr[8] = i.getFileName();
			cArr[9] = i.getLocator();
			contents.add(cArr.clone());

			// Then, the properties...
			pArr[0] = objectId;
			pArr[1] = i.getIndex();
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
			qr.insertBatch(c, translateQuery(JdbcDialect.Query.INSERT_CONTENT), JdbcTools.HANDLER_NULL,
				contents.toArray(JdbcTools.NO_PARAMS));
			qr.insertBatch(c, translateQuery(JdbcDialect.Query.INSERT_CONTENT_PROPERTY), JdbcTools.HANDLER_NULL,
				properties.toArray(JdbcTools.NO_PARAMS));
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to insert the new content records for %s", object.getDescription()), e);
		}
	}

	@Override
	protected <VALUE> List<CmfContentStream> getContentStreams(JdbcOperation operation, CmfObject<VALUE> object)
		throws CmfStorageException {
		final Connection c = operation.getConnection();
		final String objectId = JdbcTools.composeDatabaseId(object);
		final QueryRunner qr = JdbcTools.getQueryRunner();

		PreparedStatement cPS = null;
		PreparedStatement pPS = null;

		try {
			cPS = c.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_CONTENTS));
			pPS = c.prepareStatement(translateQuery(JdbcDialect.Query.LOAD_CONTENT_PROPERTIES));

			// This one will simply gather up the properties for each content record
			final ResultSetHandler<Map<String, String>> pHandler = (rs) -> {
				final Map<String, String> ret = new TreeMap<>();
				while (rs.next()) {
					ret.put(rs.getString("name"), rs.getString("value"));
				}
				return ret;
			};

			// This one will process each content record
			final ResultSetHandler<List<CmfContentStream>> cHandler = (rs) -> {
				final List<CmfContentStream> ret = new ArrayList<>();
				while (rs.next()) {
					final int contentNumber = rs.getInt("content_number");
					final CmfContentStream info = new CmfContentStream(object, contentNumber,
						rs.getString("rendition_id"), rs.getInt("rendition_page"), rs.getString("modifier"));
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
					str = rs.getString("locator");
					if ((str != null) && !rs.wasNull()) {
						info.setLocator(str);
					}

					Map<String, String> props = qr.query(c, translateQuery(JdbcDialect.Query.LOAD_CONTENT_PROPERTIES),
						pHandler, objectId, contentNumber);
					for (String s : props.keySet()) {
						String v = props.get(s);
						if ((s != null) && (v != null)) {
							info.setProperty(s, v);
						}
					}
					ret.add(info);
				}
				return ret;
			};

			return qr.query(c, translateQuery(JdbcDialect.Query.LOAD_CONTENTS), cHandler, objectId);
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to load the content records for %s", object.getDescription()), e);
		} finally {
			JdbcTools.closeQuietly(pPS);
			JdbcTools.closeQuietly(cPS);
		}
	}

	@Override
	protected void resetAltNames(JdbcOperation operation) throws CmfStorageException {
		// If the truncate table worked, do nothing else...
		if (truncateTables(operation, "CMF_ALT_NAME")) { return; }

		// No truncate? Ok...so...do it the hard way...
		final Connection c = operation.getConnection();
		QueryRunner qr = JdbcTools.getQueryRunner();
		try {
			int count = qr.update(c, translateQuery(JdbcDialect.Query.RESET_ALT_NAME));
			if (this.log.isDebugEnabled()) {
				this.log.debug("Reset {} alternate name mappings", count);
			}
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to reset the alt names table", e);
		}
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
			savePoint = JdbcTools.commitSavepoint(c, savePoint);
			return true;
		} catch (SQLException e) {
			JdbcTools.rollbackSavepoint(c, savePoint);
			this.log.trace("Failed to disable the referential integrity constraints", e);
			return false;
		} finally {
			JdbcTools.closeQuietly(s);
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
			JdbcTools.closeQuietly(s);
		}
	}

	protected String translateOptionalQuery(JdbcDialect.Query query) {
		return this.queries.get(query);
	}

	protected String translateQuery(JdbcDialect.Query query) {
		String q = translateOptionalQuery(query);
		if (q == null) { throw new IllegalStateException(String.format("Required query [%s] is missing", query)); }
		return q;
	}

	@Override
	protected Map<CmfObjectRef, String> getObjectNames(JdbcOperation operation, Collection<CmfObjectRef> refs,
		boolean latest) throws CmfStorageException {
		final Connection c = operation.getConnection();
		try {
			PreparedStatement ps = null;
			try {
				JdbcDialect.Query query = (latest ? JdbcDialect.Query.LOAD_OBJECT_NAMES_BY_ID_CURRENT
					: JdbcDialect.Query.LOAD_OBJECT_NAMES_BY_ID);
				ps = c.prepareStatement(translateQuery(query));
				ResultSet rs = null;
				try {
					Map<CmfObjectRef, String> ret = new HashMap<>();
					if (refs.isEmpty()) { return ret; }

					// Process the IDs
					Object[] arr = refs.toArray();
					for (int i = 0; i < arr.length; i++) {
						arr[i] = JdbcTools.composeDatabaseId(CmfObjectRef.class.cast(arr[i]));
					}
					if (this.dialect.isSupportsArrays()) {
						ps.setArray(1, c.createArrayOf("varchar", arr));
					} else {
						ps.setObject(1, arr);
					}

					rs = ps.executeQuery();
					while (rs.next()) {
						String id = rs.getString("object_id");
						if (rs.wasNull()) {
							continue;
						}

						String name = rs.getString("new_name");
						if (rs.wasNull()) {
							name = rs.getString("object_name");
						}

						CmfObjectRef ref = JdbcTools.decodeDatabaseId(id);
						ret.put(ref, name);
					}

					// Make sure all requested objects are referenced
					for (CmfObjectRef ref : refs) {
						if (!ret.containsKey(ref)) {
							ret.put(ref, null);
						}
					}

					return ret;
				} finally {
					DbUtils.closeQuietly(rs);
				}
			} finally {
				DbUtils.closeQuietly(ps);
			}
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to retrieve the next cached target in the given result set", e);
		}
	}

	private Collection<CmfObjectRef> getTreeRelations(JdbcOperation operation, CmfObjectRef object,
		JdbcDialect.Query query) throws CmfStorageException {
		final Connection c = operation.getConnection();
		final QueryRunner qr = JdbcTools.getQueryRunner();
		try {
			final String referenceId = JdbcTools.composeDatabaseId(object);
			return qr.query(c, translateQuery(query), (rs) -> {
				Collection<CmfObjectRef> ret = new LinkedList<>();
				while (rs.next()) {
					String id = rs.getString(1);
					if (rs.wasNull()) {
						continue;
					}
					ret.add(JdbcTools.decodeDatabaseId(id));
				}
				return Tools.freezeCollection(ret);
			}, referenceId);
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to retrieve the object's %s for %s", query.name(), object.getShortLabel()), e);
		}
	}

	@Override
	protected Collection<CmfObjectRef> getContainers(JdbcOperation operation, CmfObjectRef object)
		throws CmfStorageException {
		return getTreeRelations(operation, object, JdbcDialect.Query.LOAD_CONTAINERS);
	}

	@Override
	protected Collection<CmfObjectRef> getContainedObjects(JdbcOperation operation, CmfObjectRef object)
		throws CmfStorageException {
		return getTreeRelations(operation, object, JdbcDialect.Query.LOAD_CONTAINED_OBJECTS);
	}

	@Override
	protected <T extends Enum<T>> Collection<CmfRequirementInfo<T>> getRequirementInfo(JdbcOperation operation,
		final Class<T> statusClass, CmfObjectRef object) throws CmfStorageException {
		final Connection c = operation.getConnection();
		final QueryRunner qr = JdbcTools.getQueryRunner();
		try {
			final String objectId = JdbcTools.composeDatabaseId(object);
			return qr.query(c, translateQuery(JdbcDialect.Query.LOAD_IMPORT_PLAN), (rs) -> {
				Collection<CmfRequirementInfo<T>> ret = new LinkedList<>();
				while (rs.next()) {
					String requirementId = rs.getString("requirement_id");
					if (rs.wasNull()) {
						continue;
					}
					String statusStr = rs.getString("status");
					if (rs.wasNull()) {
						statusStr = null;
					}
					String info = rs.getString("info");
					if (rs.wasNull()) {
						info = null;
					}

					T status = null;
					if ((statusClass != null) && (statusStr != null)) {
						try {
							status = Enum.valueOf(statusClass, statusStr);
						} catch (IllegalArgumentException e) {
							throw new SQLException(String.format("Illegal enum value [%s] for type %s", statusStr,
								statusClass.getCanonicalName()));
						}
					} else {
						// If there is no status, or no status class, no info is returned
						info = null;
					}

					ret.add(new CmfRequirementInfo<>(JdbcTools.decodeDatabaseId(requirementId), status, info));
				}
				return Tools.freezeCollection(ret);
			}, objectId);
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to retrieve the object's requirement info for %s", object.getShortLabel()), e);
		}
	}

	@Override
	protected boolean addRequirement(JdbcOperation operation, CmfObjectRef object, CmfObjectRef requirement)
		throws CmfStorageException {
		final Connection c = operation.getConnection();
		final QueryRunner qr = JdbcTools.getQueryRunner();
		Savepoint savePoint = null;
		try {
			final String objectId = JdbcTools.composeDatabaseId(object);
			final String requirementId = JdbcTools.composeDatabaseId(requirement);
			savePoint = c.setSavepoint();
			qr.insert(c, translateQuery(JdbcDialect.Query.INSERT_REQUIREMENT), JdbcTools.HANDLER_NULL, objectId,
				requirementId);
			savePoint = JdbcTools.commitSavepoint(c, savePoint);
			return true;
		} catch (SQLException e) {
			if (this.dialect.isDuplicateKeyException(e)) {
				// We're good! With the use of savepoints, the transaction will remain valid and
				// thus we'll be OK to continue using the transaction in other connections
				JdbcTools.rollbackSavepoint(c, savePoint);
				return false;
			}
			throw new CmfStorageException(String.format("Failed to add the %s object's requirement info for %s",
				object.getShortLabel(), requirement.getShortLabel()), e);
		}
	}

	@Override
	protected <T extends Enum<T>> CmfRequirementInfo<T> setImportStatus(JdbcOperation operation, CmfObjectRef object,
		T status, String info) throws CmfStorageException {
		final Connection c = operation.getConnection();
		final QueryRunner qr = JdbcTools.getQueryRunner();
		Savepoint savePoint = null;
		try {
			savePoint = c.setSavepoint();
			final String objectId = JdbcTools.composeDatabaseId(object);
			final String statusStr = status.name();
			qr.insert(c, translateQuery(JdbcDialect.Query.INSERT_IMPORT_PLAN), JdbcTools.HANDLER_NULL, objectId,
				statusStr, info);
			savePoint = JdbcTools.commitSavepoint(c, savePoint);
			return new CmfRequirementInfo<>(object, status, info);
		} catch (SQLException e) {
			if (this.dialect.isDuplicateKeyException(e)) {
				// We're good! With the use of savepoints, the transaction will remain valid and
				// thus we'll be OK to continue using the transaction in other connections
				JdbcTools.rollbackSavepoint(c, savePoint);
				return null;
			}
			throw new CmfStorageException(
				String.format("Failed to persist import plan information for %s", object.getShortLabel()), e);
		}
	}

	@Override
	protected void clearImportPlan(JdbcOperation operation) throws CmfStorageException {
		if (truncateTables(operation, "CMF_IMPORT_PLAN")) { return; }
		final Connection c = operation.getConnection();
		final QueryRunner qr = JdbcTools.getQueryRunner();
		try {
			qr.update(c, translateQuery(JdbcDialect.Query.CLEAR_IMPORT_PLAN));
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to clear the requirement info", e);
		}
	}

	@Override
	protected boolean doClose(boolean cleanupIfEmpty) {
		if (this.lastConnection != null) {
			try {
				final String sql = translateQuery(JdbcDialect.Query.SHUTDOWN_DB);
				if (!StringUtils.isBlank(sql)) {
					final QueryRunner qr = JdbcTools.getQueryRunner();
					try {
						qr.update(this.lastConnection, sql);
					} catch (SQLException e) {
						this.log.error("Failed to close the last connection and shutdown the DataSource", e);
					}
				}
			} finally {
				JdbcTools.closeQuietly(this.lastConnection);
			}
		}
		return true;
	}

	@Override
	protected void clearBulkObjectLoaderFilter(JdbcOperation operation) throws CmfStorageException {
		if (truncateTables(operation, "CMF_OBJECT_FILTER")) { return; }
		final Connection c = operation.getConnection();
		final QueryRunner qr = JdbcTools.getQueryRunner();
		try {
			qr.update(c, translateQuery(JdbcDialect.Query.CLEAR_LOADER_FILTER));
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to clear the bulk loader filter", e);
		}
	}

	@Override
	protected boolean addBulkObjectLoaderFilterEntry(JdbcOperation operation, CmfObjectRef entry)
		throws CmfStorageException {
		final Connection c = operation.getConnection();
		final QueryRunner qr = JdbcTools.getQueryRunner();
		Savepoint savePoint = null;
		try {
			savePoint = c.setSavepoint();
			qr.insert(c, translateQuery(JdbcDialect.Query.INSERT_LOADER_FILTER), JdbcTools.HANDLER_NULL,
				JdbcTools.composeDatabaseId(entry));
			savePoint = JdbcTools.commitSavepoint(c, savePoint);
			return true;
		} catch (SQLException e) {
			if (this.dialect.isDuplicateKeyException(e) || this.dialect.isForeignKeyMissingException(e)) {
				// We're good! With the use of savepoints, the transaction will remain valid and
				// thus we'll be OK to continue using the transaction in other connections
				JdbcTools.rollbackSavepoint(c, savePoint);
				return false;
			}
			throw new CmfStorageException(String.format("Failed to persist the bulk loader filter entry for {%s-%s}",
				entry.getType().name(), entry.getId()), e);
		}
	}

	@Override
	protected Map<CmfObject.Archetype, Set<CmfObjectRef>> getObjectFilter(JdbcOperation operation)
		throws CmfStorageException {
		final Connection c = operation.getConnection();
		final QueryRunner qr = JdbcTools.getQueryRunner();
		try {
			final Map<CmfObject.Archetype, Set<CmfObjectRef>> map = new EnumMap<>(CmfObject.Archetype.class);
			qr.query(c, translateQuery(JdbcDialect.Query.LOAD_FILTER), (rs) -> {
				while (rs.next()) {
					String v = rs.getString(1);
					// Defend against nulls/empties
					if (StringUtils.isEmpty(v) || rs.wasNull()) {
						continue;
					}
					CmfObjectRef ref = JdbcTools.decodeDatabaseId(v);
					Set<CmfObjectRef> set = map.get(ref.getType());
					if (set == null) {
						set = new LinkedHashSet<>();
						map.put(ref.getType(), set);
					}
					set.add(ref);
				}
				return null;
			});
			return map;
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to load the object filter data", e);
		}
	}

	@Override
	protected void clearAllProperties(JdbcOperation operation, String prefix) throws CmfStorageException {
		this.propertyManager.clearAllProperties(operation, prefix);
	}

	@Override
	protected Set<String> getPropertyNames(JdbcOperation operation, String prefix) throws CmfStorageException {
		return this.propertyManager.getPropertyNames(operation, prefix);
	}
}