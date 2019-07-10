/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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

package com.armedia.caliente.engine.dfc.exporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dfc.UnsupportedDctmObjectTypeException;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.commons.utilities.CloseableIterator;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.common.DfException;

/**
 *
 *
 */
public class DctmExportTargetIterator extends CloseableIterator<ExportTarget> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String idAttribute;
	private final String typeAttribute;
	private final IDfCollection collection;
	private int index = 0;

	public DctmExportTargetIterator(IDfCollection collection) {
		this(collection, null, null);
	}

	public DctmExportTargetIterator(IDfCollection collection, String idAttribute) {
		this(collection, idAttribute, null);
	}

	public DctmExportTargetIterator(IDfCollection collection, String idAttribute, String typeAttribute) {
		this.collection = collection;
		this.typeAttribute = typeAttribute;
		this.idAttribute = idAttribute;
	}

	@Override
	protected CloseableIterator<ExportTarget>.Result findNext() throws Exception {
		if (!this.collection.next()) { return null; }

		this.index++;
		try {
			return found(DctmExportTools.getExportTarget(this.collection, this.idAttribute, this.typeAttribute));
		} catch (DfException e) {
			throw new ExportException(String.format("DfException caught constructing export target # %d", this.index),
				e);
		} catch (UnsupportedDctmObjectTypeException e) {
			String dump = " (dump failed)";
			try {
				dump = String.format(":%n%s%n", this.collection.dump());
			} catch (DfException e2) {
				if (this.log.isTraceEnabled()) {
					this.log.error("Failed to generate the debug dump for object # {}", this.index, e2);
				}
			}
			throw new ExportException(String.format("Item # %d is not a supported export target: %s", this.index, dump),
				e);
		}
	}

	@Override
	protected void doClose() {
		DfcUtils.closeQuietly(this.collection);
	}
}