package com.armedia.caliente.tools.datasource;

import javax.sql.DataSource;

public class DataSourceDescriptor<DS extends DataSource> {
	private final DS dataSource;
	private final boolean managedTransactions;

	public DataSourceDescriptor(DS dataSource, boolean managedTransactions) {
		this.dataSource = dataSource;
		this.managedTransactions = managedTransactions;
	}

	public final DS getDataSource() {
		return this.dataSource;
	}

	public final boolean isManagedTransactions() {
		return this.managedTransactions;
	}

	public void close() {
		// By default, do nothing...
	}
}