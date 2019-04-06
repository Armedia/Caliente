/**
 *
 */

package com.armedia.caliente.tools.datasource.pooled;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;

/**
 * @author diego
 *
 */
public class PooledDataSourceDescriptor extends DataSourceDescriptor<BasicDataSource> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final AtomicBoolean openFlag = new AtomicBoolean();
	private final String driver;
	private final String url;
	private final BasicDataSource dataSource;
	private final Thread shutdown = new Thread() {
		@Override
		public void run() {
			doClose();
		}
	};

	PooledDataSourceDescriptor(BasicDataSource dataSource, boolean managedTransactions) {
		super(dataSource, managedTransactions);
		this.driver = dataSource.getDriverClassName();
		this.url = dataSource.getUrl();
		this.dataSource = dataSource;

		Runtime.getRuntime().addShutdownHook(this.shutdown);
		this.openFlag.set(true);
	}

	private boolean doClose() {
		if (PooledDataSourceDescriptor.this.openFlag.compareAndSet(true, false)) {
			try {
				this.dataSource.close();
			} catch (SQLException e) {
				// Log the error
				String msg = String.format("Failed to close the JDBC connection pool for [%s]/[%s]",
					PooledDataSourceDescriptor.this.driver, PooledDataSourceDescriptor.this.url);
				if (PooledDataSourceDescriptor.this.log.isDebugEnabled()) {
					PooledDataSourceDescriptor.this.log.error(msg, e);
				} else {
					PooledDataSourceDescriptor.this.log.error(msg);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void close() {
		if (doClose()) {
			Runtime.getRuntime().removeShutdownHook(this.shutdown);
		}
	}
}