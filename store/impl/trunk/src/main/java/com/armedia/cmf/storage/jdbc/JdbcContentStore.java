package com.armedia.cmf.storage.jdbc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.dslocator.DataSourceDescriptor;

public class JdbcContentStore extends CmfContentStore<String, Connection, JdbcOperation> {

	private static final String PROPERTY_TABLE = "cmf_content_info";
	private static final String SCHEMA_CHANGE_LOG = "db.changelog.content.xml";

	private final boolean managedTransactions;
	private final DataSourceDescriptor<?> dataSourceDescriptor;
	private final DataSource dataSource;
	private final JdbcStorePropertyManager propertyManager;

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
	protected boolean isSupported(String locator) {
		return false;
	}

	@Override
	public boolean isSupportsFileAccess() {
		return false;
	}

	@Override
	protected CmfContentStore<String, Connection, JdbcOperation>.Handle constructHandle(CmfObject<?> object,
		String qualifier, String locator) {
		return null;
	}

	@Override
	protected String doCalculateLocator(CmfAttributeTranslator<?> translator, CmfObject<?> object, String qualifier) {
		return null;
	}

	@Override
	protected File doGetFile(String locator) {
		return null;
	}

	@Override
	protected InputStream doOpenInput(String locator) throws IOException {
		return null;
	}

	@Override
	protected OutputStream doOpenOutput(String locator) throws IOException {
		return null;
	}

	@Override
	protected boolean doIsExists(String locator) {
		return false;
	}

	@Override
	protected long doGetStreamSize(String locator) {
		return 0;
	}

	@Override
	protected void doClearAllStreams() {
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