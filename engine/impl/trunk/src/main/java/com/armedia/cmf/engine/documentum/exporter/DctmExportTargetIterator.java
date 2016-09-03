/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.UnsupportedDctmObjectTypeException;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
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
	private int current = 0;

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
	public boolean checkNext() {
		try {
			return this.collection.next();
		} catch (DfException e) {
			throw new RuntimeException(String.format("Failed to get element # %d", this.current), e);
		}
	}

	private ExportTarget newTarget() throws DfException, UnsupportedDctmObjectTypeException {
		return DfUtils.getExportTarget(this.collection, this.idAttribute, this.typeAttribute);
	}

	@Override
	public ExportTarget getNext() throws Exception {
		this.current++;
		try {
			return newTarget();
		} catch (DfException e) {
			throw new ExportException(String.format("DfException caught constructing export target # %d", this.current),
				e);
		} catch (UnsupportedDctmObjectTypeException e) {
			String dump = " (dump failed)";
			try {
				dump = String.format(":%n%s%n", this.collection.dump());
			} catch (DfException e2) {
				if (this.log.isTraceEnabled()) {
					this.log.error(String.format("Failed to generate the debug dump for object # %d", this.current),
						e2);
				}
			}
			throw new ExportException(
				String.format("Item # %d is not a supported export target: %s", this.current, dump), e);
		}
	}

	@Override
	protected void doClose() {
		DfUtils.closeQuietly(this.collection);
	}
}