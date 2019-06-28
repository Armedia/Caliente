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