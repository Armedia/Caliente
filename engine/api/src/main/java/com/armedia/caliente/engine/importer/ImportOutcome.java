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

public class ImportOutcome {

	public static final ImportOutcome SKIPPED = new ImportOutcome(ImportResult.SKIPPED);
	public static final ImportOutcome FAILED = new ImportOutcome(ImportResult.FAILED);
	public static final ImportOutcome IGNORED = new ImportOutcome(ImportResult.IGNORED);
	public static final ImportOutcome DUPLICATE = new ImportOutcome(ImportResult.DUPLICATE);

	private final ImportResult result;
	private final String newId;
	private final String newLabel;

	private ImportOutcome(ImportResult result) {
		this(result, null, null);
	}

	/**
	 * @param result
	 * @param newId
	 * @param newLabel
	 */
	public ImportOutcome(ImportResult result, String newId, String newLabel) {
		this.result = result;
		this.newId = newId;
		this.newLabel = newLabel;
	}

	public final ImportResult getResult() {
		return this.result;
	}

	public final String getNewId() {
		return this.newId;
	}

	public final String getNewLabel() {
		return this.newLabel;
	}

	@Override
	public String toString() {
		return String.format("ImportOutcome [result=%s, newId=%s, newLabel=%s]", this.result, this.newId,
			this.newLabel);
	}
}