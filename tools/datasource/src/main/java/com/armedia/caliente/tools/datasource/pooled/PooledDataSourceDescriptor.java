/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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

package com.armedia.caliente.tools.datasource.pooled;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;

/**
 *
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