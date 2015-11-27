package com.armedia.cmf.storage.jdbc;

import java.sql.Connection;

import org.apache.commons.dbutils.DbUtils;

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

	static void prepareSchema(String changeLog, Connection c, boolean updateSchema, boolean cleanData,
		boolean managedTx, Callback callback) throws CmfStorageException {
		boolean ok = false;
		try {
			Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
			Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database);
			if (updateSchema) {
				liquibase.update((String) null);
			} else {
				liquibase.validate();
			}
			JdbcOperation op = new JdbcOperation(c, managedTx);
			if (cleanData && (callback != null)) {
				callback.cleanData(op);
			}
			ok = true;
		} catch (DatabaseException e) {
			throw new CmfStorageException(String.format(
				"Failed to find a supported database for the given connection (changeLog = [%s])", changeLog), e);
		} catch (LiquibaseException e) {
			if (updateSchema) {
				throw new CmfStorageException(
					String.format("Failed to generate/update the SQL schema from changeLog [%s]", changeLog), e);
			} else {
				throw new CmfStorageException(
					String.format("The SQL schema in changeLog [%s] is of the wrong version or structure", changeLog),
					e);
			}
		} finally {
			if (managedTx) {
				// We're not owning the transaction
				DbUtils.closeQuietly(c);
			} else if (ok) {
				DbUtils.commitAndCloseQuietly(c);
			} else {
				DbUtils.rollbackAndCloseQuietly(c);
			}
		}
	}
}