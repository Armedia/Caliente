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
package com.armedia.caliente.engine.importer;

public interface ImportStrategy {

	/**
	 * <p>
	 * Returns {@code true} if the strategy is to ignore this import, or {@code false} if this
	 * import should be processed normally.
	 * </p>
	 *
	 * @return {@code true} if the strategy is to ignore this import, or {@code false} if this
	 *         import should be processed normally.
	 */
	public boolean isIgnored();

	/**
	 * <p>
	 * Returns {@code true} if parallelization is supported, {@code false} otherwise.
	 * </p>
	 *
	 * @return {@code true} if parallelization is supported, {@code false} otherwise.
	 */
	public boolean isParallelCapable();

	/**
	 * <p>
	 * Returns {@code true} if batches should be failed after the first failure, or {@code false} if
	 * processing should continue regardless.
	 * </p>
	 *
	 * @return {@code true} if batches should be failed after the first failure, or {@code false} if
	 *         processing should continue regardless
	 */
	public boolean isFailBatchOnError();

	/**
	 * <p>
	 * Returns {@code true} if this object type supports transactions, {@code false} otherwise.
	 * </p>
	 *
	 * @return {@code true} if this object type supports transactions, {@code false} otherwise.
	 */
	public boolean isSupportsTransactions();
}