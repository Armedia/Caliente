/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dfc.UnsupportedDctmObjectTypeException;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.tools.dfc.DfUtils;
import com.armedia.commons.utilities.CloseableIterator;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.common.DfException;

/**
 * @author diego
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
		DfUtils.closeQuietly(this.collection);
	}
}