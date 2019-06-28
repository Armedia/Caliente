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
package com.armedia.caliente.tools.dfc;

/**
 * The Class Constant. This class contains various constant values used throughout the CMSMF
 * application.
 *
 *
 */
public class DfcConstant {

	/** The string value that is stored in user type attribute for inline users. */
	public static final String USER_SOURCE_INLINE_PASSWORD = "inline password";

	/**
	 * The "sql-neutral" date and time pattern that will be fed for SQL updates to date attributes
	 */
	public static final String SQL_DATETIME_PATTERN = "yyyy-mm-dd hh:mi:ss";

	/**
	 * The "sql-neutral" date and time pattern that will be fed for SQL updates to date attributes
	 */
	public static final String JAVA_SQL_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

	/** The date and time pattern used in oracle sql query. */
	public static final String ORACLE_DATETIME_PATTERN = "YYYY-MM-DD HH24:MI:SS";

	public static final int MSSQL_DATETIME_PATTERN = 120;

	public static final String RUN_NOW = "run_now";

	public static final String EXACT_ID = "EXACT_ID";

	public static final String VERSION_LABEL = "VERSION_LABEL";
}