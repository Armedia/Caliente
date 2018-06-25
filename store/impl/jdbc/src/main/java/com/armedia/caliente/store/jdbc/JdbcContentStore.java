package com.armedia.caliente.store.jdbc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfOperationException;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.dslocator.DataSourceDescriptor;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class JdbcContentStore extends CmfContentStore<JdbcContentLocator, Connection, JdbcOperation> {

	private static final String PROPERTY_TABLE = "cmf_content_info";
	private static final String SCHEMA_CHANGE_LOG = "content.changelog.xml";

	private static final ResultSetHandler<Long> HANDLER_LENGTH = new ResultSetHandler<Long>() {
		@Override
		public Long handle(ResultSet rs) throws SQLException {
			if (!rs.next()) { return null; }
			long l = rs.getLong("length");
			if (rs.wasNull()) { return null; }
			return l;
		}
	};

	private class Input extends InputStream {
		private final JdbcContentLocator locator;
		private Blob blob = null;
		private InputStream stream = null;
		private JdbcOperation operation = null;
		private boolean tx = false;
		private boolean finished = false;
		private PreparedStatement ps = null;
		private ResultSet rs = null;

		private Input(JdbcContentLocator locator) throws CmfStorageException, SQLException {
			this.locator = locator;
			boolean ok = false;
			try {
				this.operation = beginConcurrentInvocation();
				this.tx = this.operation.begin();

				this.ps = this.operation.getConnection().prepareStatement(translateQuery(JdbcDialect.Query.GET_STREAM));
				this.ps.setString(1, this.locator.getObjectId());
				this.ps.setString(2, this.locator.getInfo().getRenditionIdentifier());
				this.ps.setInt(3, this.locator.getInfo().getRenditionPage());
				this.rs = this.ps.executeQuery();
				if (!this.rs.next()) { throw new CmfStorageException(
					String.format("No data stream found for locator [%s]", this.locator)); }

				this.blob = this.rs.getBlob("data");
				if (this.rs.wasNull()) { throw new CmfStorageException(
					String.format("The data stream for locator [%s] was NULL", this.locator)); }
				this.stream = this.blob.getBinaryStream();
				ok = true;
			} finally {
				if (!ok) {
					JdbcTools.closeQuietly(this.rs);
					this.rs = null;
					JdbcTools.closeQuietly(this.ps);
					this.ps = null;
					if (this.operation != null) {
						try {
							close();
						} catch (IOException e) {
							throw new CmfStorageException(e);
						}
					}
				}
			}
		}

		private void assertOpen() throws IOException {
			if (this.finished) { throw new IOException(
				String.format("The InputStream for locator [%s] has already been closed", this.locator)); }
		}

		private void assertOpenRT() {
			try {
				assertOpen();
			} catch (IOException e) {
				throw new RuntimeException(
					String.format("The InputStream for locator [%s] has already been closed", this.locator));
			}
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			assertOpen();
			return this.stream.read(b, off, len);
		}

		@Override
		public int read() throws IOException {
			assertOpen();
			return this.stream.read();
		}

		@Override
		public long skip(long n) throws IOException {
			assertOpen();
			return this.stream.skip(n);
		}

		@Override
		public int available() throws IOException {
			assertOpen();
			return this.stream.available();
		}

		@Override
		public synchronized void mark(int readlimit) {
			assertOpenRT();
			this.stream.mark(readlimit);
		}

		@Override
		public boolean markSupported() {
			assertOpenRT();
			return this.stream.markSupported();
		}

		@Override
		public synchronized void reset() throws IOException {
			assertOpen();
			this.stream.reset();
		}

		@Override
		public void close() throws IOException {
			assertOpenRT();
			this.finished = true;
			IOUtils.closeQuietly(this.stream);
			JdbcTools.closeQuietly(this.rs);
			JdbcTools.closeQuietly(this.ps);
			try {
				if (this.tx) {
					try {
						this.operation.rollback();
					} catch (CmfOperationException e) {
						JdbcContentStore.this.log.warn(
							String.format("Failed to rollback the transaction for closing the stream for locator [%s]",
								this.locator),
							e);
					}
				}
			} finally {
				endConcurrentInvocation(this.operation);
			}
		}
	}

	private final boolean managedTransactions;
	private final DataSourceDescriptor<?> dataSourceDescriptor;
	private final DataSource dataSource;
	private final JdbcStorePropertyManager propertyManager;
	private final JdbcDialect dialect;
	private final Map<JdbcDialect.Query, String> queries;
	private final CfgTools cfg;

	private class JdbcHandle extends Handle {

		protected JdbcHandle(CmfObject<?> object, CmfContentStream info, JdbcContentLocator locator) {
			super(object, info, locator);
		}

	}

	public JdbcContentStore(DataSourceDescriptor<?> dataSourceDescriptor, boolean updateSchema, boolean cleanData,
		CfgTools cfg) throws CmfStorageException {
		if (dataSourceDescriptor == null) { throw new IllegalArgumentException(
			"Must provide a valid DataSource instance"); }
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

		try {
			try {
				this.dialect = JdbcDialect.getDialect(c.getMetaData());
			} catch (SQLException e) {
				throw new CmfStorageException("Failed to initialize the query resolver", e);
			}

			this.propertyManager = new JdbcStorePropertyManager(JdbcContentStore.PROPERTY_TABLE);

			JdbcOperation op = new JdbcOperation(c, this.managedTransactions);
			boolean ok = false;
			op.begin();
			try {
				JdbcSchemaManager.prepareSchema(JdbcContentStore.SCHEMA_CHANGE_LOG, op, updateSchema, cleanData,
					this.managedTransactions, new JdbcSchemaManager.Callback() {
						@Override
						public void cleanData(JdbcOperation op) throws CmfStorageException {
							clearAllProperties(op);
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

			Map<JdbcDialect.Query, String> queries = new EnumMap<>(JdbcDialect.Query.class);
			for (JdbcDialect.Query q : JdbcDialect.Query.values()) {
				String v = this.dialect.translateQuery(q);
				if (StringUtils.isEmpty(v)) {
					continue;
				}
				queries.put(q, v);
			}
			this.queries = Tools.freezeMap(queries);
		} finally {
			JdbcTools.closeQuietly(c);
		}
	}

	@Override
	public File getStoreLocation() {
		return Tools.canonicalize(new File(this.cfg.getString("dir.content")));
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
	protected File doGetRootLocation() {
		return null;
	}

	@Override
	protected JdbcHandle constructHandle(CmfObject<?> object, CmfContentStream info, JdbcContentLocator locator) {
		return new JdbcHandle(object, info, locator);
	}

	@Override
	protected <T> JdbcContentLocator doCalculateLocator(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		return new JdbcContentLocator(object.getId(), info);
	}

	@Override
	protected InputStream openInput(JdbcOperation operation, JdbcContentLocator locator) throws CmfStorageException {
		if (!isExists(operation, locator)) { return null; }
		try {
			return new Input(locator);
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to open an input stream to the content at locator [%s]", locator), e);
		}
	}

	@Override
	protected long setContents(JdbcOperation operation, JdbcContentLocator locator, InputStream in)
		throws CmfStorageException {
		// TODO: Modify to support large files by segmenting into multiple insert+update combos
		// such that each subsequent query adds an additional "large" chunk to the overall BLOB
		// in the DB
		final Connection c = operation.getConnection();
		try {
			final Blob blob = c.createBlob();
			try {
				OutputStream out = blob.setBinaryStream(1);
				try {
					IOUtils.copy(in, out);
				} catch (IOException e) {
					throw new CmfStorageException(String
						.format("Failed to copy the content from the given input stream for locator [%s]", locator), e);
				} finally {
					IOUtils.closeQuietly(out);
				}
				QueryRunner qr = JdbcTools.getQueryRunner();
				CmfContentStream info = locator.getInfo();
				qr.update(c, translateQuery(JdbcDialect.Query.DELETE_STREAM), locator.getObjectId(),
					info.getRenditionIdentifier(), info.getRenditionPage());
				qr.insert(c, translateQuery(JdbcDialect.Query.INSERT_STREAM), JdbcTools.HANDLER_NULL,
					locator.getObjectId(), info.getRenditionIdentifier(), info.getRenditionPage(), blob.length(), blob);
				return blob.length();
			} finally {
				blob.free();
			}
		} catch (SQLException e) {
			throw new CmfStorageException(String.format("DB error setting the content for locator [%s]", locator), e);
		}
	}

	@Override
	protected boolean isExists(JdbcOperation operation, JdbcContentLocator locator) throws CmfStorageException {
		try {
			CmfContentStream info = locator.getInfo();
			return JdbcTools.getQueryRunner().query(operation.getConnection(),
				translateQuery(JdbcDialect.Query.CHECK_IF_CONTENT_EXISTS), JdbcTools.HANDLER_EXISTS,
				locator.getObjectId(), info.getRenditionIdentifier(), info.getRenditionPage());
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to check whether a stream exists for locator [%s]", locator), e);
		}
	}

	@Override
	protected long getStreamSize(JdbcOperation operation, JdbcContentLocator locator) throws CmfStorageException {
		try {
			CmfContentStream info = locator.getInfo();
			return JdbcTools.getQueryRunner().query(operation.getConnection(),
				translateQuery(JdbcDialect.Query.GET_STREAM_LENGTH), JdbcContentStore.HANDLER_LENGTH,
				locator.getObjectId(), info.getRenditionIdentifier(), info.getRenditionPage());
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to check whether a stream exists for locator [%s]", locator), e);
		}
	}

	@Override
	protected void clearAllStreams(JdbcOperation operation) throws CmfStorageException {
		try {
			JdbcTools.getQueryRunner().update(operation.getConnection(),
				translateQuery(JdbcDialect.Query.DELETE_ALL_STREAMS));
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
	protected void clearAllProperties(JdbcOperation operation) throws CmfStorageException {
		this.propertyManager.clearAllProperties(operation);
	}

	protected String translateOptionalQuery(JdbcDialect.Query query) {
		return this.queries.get(query);
	}

	protected String translateQuery(JdbcDialect.Query query) {
		String q = translateOptionalQuery(query);
		if (q == null) { throw new IllegalStateException(String.format("Required query [%s] is missing", query)); }
		return q;
	}
}