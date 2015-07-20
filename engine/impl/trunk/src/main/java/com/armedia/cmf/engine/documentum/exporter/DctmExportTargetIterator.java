/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.documentum.DctmCollectionIterator;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.UnsupportedDctmObjectTypeException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmExportTargetIterator implements Iterator<ExportTarget> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String idAttribute;
	private final String typeAttribute;
	private final DctmCollectionIterator iterator;
	private int current = 0;

	public DctmExportTargetIterator(IDfCollection collection) {
		this(collection, null, null);
	}

	public DctmExportTargetIterator(IDfCollection collection, String idAttribute) {
		this(collection, idAttribute, null);
	}

	public DctmExportTargetIterator(IDfCollection collection, String idAttribute, String typeAttribute) {
		this.iterator = new DctmCollectionIterator(collection);
		this.typeAttribute = typeAttribute;
		this.idAttribute = idAttribute;
	}

	@Override
	public boolean hasNext() {
		return this.iterator.hasNext();
	}

	private ExportTarget newTarget(IDfTypedObject source) throws DfException, UnsupportedDctmObjectTypeException {
		return DfUtils.getExportTarget(source, this.idAttribute, this.typeAttribute);
	}

	@Override
	public ExportTarget next() {
		IDfTypedObject next = this.iterator.next();
		this.current++;
		try {
			return newTarget(next);
		} catch (DfException e) {
			throw new RuntimeException(
				String.format("DfException caught constructing export target # %d", this.current), e);
		} catch (UnsupportedDctmObjectTypeException e) {
			String dump = " (dump failed)";
			try {
				dump = String.format(":%n%s%n", next.dump());
			} catch (DfException e2) {
				if (this.log.isTraceEnabled()) {
					this.log
						.error(String.format("Failed to generate the debug dump for object # %d", this.current), e2);
				}
			}
			throw new RuntimeException(String.format("Item # %d is not a supported export target: %s", this.current,
				dump), e);
		}
	}

	@Override
	public void remove() {
		this.iterator.remove();
	}
}