package com.armedia.caliente.store.jdbc;

import com.armedia.caliente.store.CmfStorageException;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

class JdbcSchemaManager {

	@FunctionalInterface
	static interface SchemaPreparation {
		void prepareSchema(JdbcOperation op) throws CmfStorageException;
	}

	private JdbcSchemaManager() {
	}

	static void prepareSchema(String changeLog, JdbcOperation op, boolean updateSchema, boolean managedTx,
		SchemaPreparation... schemaPreparations) throws CmfStorageException {
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

		if (schemaPreparations != null) {
			for (SchemaPreparation p : schemaPreparations) {
				if (p != null) {
					p.prepareSchema(op);
				}
			}
		}
	}
}