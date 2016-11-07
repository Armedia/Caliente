package com.armedia.cmf.storage.jdbc;

import com.armedia.cmf.storage.CmfStorageException;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

class JdbcSchemaManager {

	static interface Callback {
		void cleanData(JdbcOperation op) throws CmfStorageException;
	}

	private JdbcSchemaManager() {
	}

	static void prepareSchema(String changeLog, JdbcOperation op, boolean updateSchema, boolean cleanData,
		boolean managedTx, Callback callback) throws CmfStorageException {
		try {
			Database database = DatabaseFactory.getInstance()
				.findCorrectDatabaseImplementation(new JdbcConnection(op.getConnection()));
			Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database);
			if (updateSchema) {
				liquibase.update((String) null);
			} else {
				liquibase.validate();
			}
		} catch (DatabaseException e) {
			throw new CmfStorageException(String.format(
				"Failed to find a supported database for the given connection (changeLog = [%s])", changeLog), e);
		} catch (LiquibaseException e) {
			String fmt = (updateSchema ? "Failed to generate/update the SQL schema from changeLog [%s]"
				: "The SQL schema in changeLog [%s] is of the wrong version or structure");
			throw new CmfStorageException(String.format(fmt, changeLog), e);
		}

		if (cleanData && (callback != null)) {
			callback.cleanData(op);
		}
	}
}