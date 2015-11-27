package com.armedia.cmf.storage.jdbc;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.dbutils.ResultSetHandler;

import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.dslocator.DataSourceDescriptor;

public class JdbcContentStore extends CmfContentStore<JdbcContentLocator, Connection, JdbcOperation> {

	private static final String PROPERTY_TABLE = "cmf_content_info";
	private static final String SCHEMA_CHANGE_LOG = "db.changelog.content.xml";

	private static final String DELETE_ALL_STREAMS_SQL = "delete from cmf_content_stream";
	private static final String CHECK_EXISTS_SQL = "select object_id from cmf_content_stream where object_id = ? and qualifier = ?";
	private static final String GET_STREAM_LENGTH_SQL = "select length from cmf_content_stream where object_id = ? and qualifier = ?";

	private static final ResultSetHandler<Long> HANDLER_LENGTH = new ResultSetHandler<Long>() {
		@Override
		public Long handle(ResultSet rs) throws SQLException {
			if (!rs.next()) { return null; }
			long l = rs.getLong("length");
			if (rs.wasNull()) { return null; }
			return l;
		}
	};

	private final boolean managedTransactions;
	private final DataSourceDescriptor<?> dataSourceDescriptor;
	private final DataSource dataSource;
	private final JdbcStorePropertyManager propertyManager;

	private class JdbcHandle extends Handle {

		protected JdbcHandle(CmfObject<?> object, String qualifier, JdbcContentLocator locator) {
			super(object, qualifier, locator);
		}

	}

	public JdbcContentStore(DataSourceDescriptor<?> dataSourceDescriptor, boolean updateSchema, boolean cleanData)
		throws CmfStorageException {
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

		this.propertyManager = new JdbcStorePropertyManager(JdbcContentStore.PROPERTY_TABLE);

		JdbcSchemaManager.prepareSchema(JdbcContentStore.SCHEMA_CHANGE_LOG, c, updateSchema, cleanData,
			this.managedTransactions, new JdbcSchemaManager.Callback() {
				@Override
				public void cleanData(JdbcOperation op) throws CmfStorageException {
					clearProperties(op);
				}
			});
	}

	protected final DataSourceDescriptor<?> getDataSourceDescriptor() {
		return this.dataSourceDescriptor;
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
	protected boolean isSupported(JdbcContentLocator locator) {
		return (locator != null);
	}

	@Override
	public boolean isSupportsFileAccess() {
		return false;
	}

	@Override
	protected JdbcHandle constructHandle(CmfObject<?> object, String qualifier, JdbcContentLocator locator) {
		return new JdbcHandle(object, qualifier, locator);
	}

	@Override
	protected JdbcContentLocator doCalculateLocator(CmfAttributeTranslator<?> translator, CmfObject<?> object,
		String qualifier) {
		return new JdbcContentLocator(object.getId(), qualifier);
	}

	@Override
	protected InputStream openInput(JdbcOperation operation, JdbcContentLocator locator) throws CmfStorageException {
		return null;
	}

	@Override
	protected OutputStream openOutput(JdbcOperation operation, JdbcContentLocator locator) throws CmfStorageException {
		return null;
	}

	@Override
	protected boolean isExists(JdbcOperation operation, JdbcContentLocator locator) throws CmfStorageException {
		try {
			return JdbcTools.getQueryRunner().query(operation.getConnection(), JdbcContentStore.CHECK_EXISTS_SQL,
				JdbcTools.HANDLER_EXISTS, locator.getObjectId(), locator.getQualifier());
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to check whether a stream exists for locator [%s]", locator), e);
		}
	}

	@Override
	protected long getStreamSize(JdbcOperation operation, JdbcContentLocator locator) throws CmfStorageException {
		try {
			return JdbcTools.getQueryRunner().query(operation.getConnection(), JdbcContentStore.GET_STREAM_LENGTH_SQL,
				JdbcContentStore.HANDLER_LENGTH, locator.getObjectId(), locator.getQualifier());
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to check whether a stream exists for locator [%s]", locator), e);
		}
	}

	@Override
	protected void clearAllStreams(JdbcOperation operation) throws CmfStorageException {
		try {
			JdbcTools.getQueryRunner().update(operation.getConnection(), JdbcContentStore.DELETE_ALL_STREAMS_SQL);
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to delete all content streams", e);
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
}