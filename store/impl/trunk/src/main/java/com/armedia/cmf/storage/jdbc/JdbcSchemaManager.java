package com.armedia.cmf.storage.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.CmfStorageException;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

class JdbcSchemaManager {

	private static final Logger LOG = LoggerFactory.getLogger(JdbcSchemaManager.class);

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
			if (!managedTx) {
				c.commit();
			}
			ok = true;
		} catch (DatabaseException e) {
			throw new CmfStorageException(String.format(
				"Failed to find a supported database for the given connection (changeLog = [%s])", changeLog), e);
		} catch (LiquibaseException e) {
			String fmt = (updateSchema ? "Failed to generate/update the SQL schema from changeLog [%s]"
				: "The SQL schema in changeLog [%s] is of the wrong version or structure");
			throw new CmfStorageException(String.format(fmt, changeLog), e);
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to commit the changes to the database from changelog [%s]", changeLog), e);
		} finally {
			if (!managedTx && !ok) {
				try {
					c.rollback();
				} catch (SQLException e) {
					JdbcSchemaManager.LOG.error("Rollback failed", e);
				}
			}
		}
	}
}