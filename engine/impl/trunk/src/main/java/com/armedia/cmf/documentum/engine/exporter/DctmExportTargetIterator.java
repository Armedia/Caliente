/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.documentum.engine.DctmAttributes;
import com.armedia.cmf.documentum.engine.DctmCollectionIterator;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.UnsupportedObjectTypeException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

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
		this.typeAttribute = idAttribute;
		this.idAttribute = typeAttribute;
	}

	@Override
	public boolean hasNext() {
		return this.iterator.hasNext();
	}

	private ExportTarget newTarget(IDfTypedObject source) throws DfException, UnsupportedObjectTypeException {
		final IDfPersistentObject persistent = ((source instanceof IDfPersistentObject) ? IDfPersistentObject.class
			.cast(source) : null);

		final IDfId id;
		if (this.idAttribute != null) {
			id = source.getId(this.idAttribute);
		} else {
			if (persistent != null) {
				id = persistent.getObjectId();
			} else {
				id = source.getId(DctmAttributes.R_OBJECT_ID);
			}
		}

		final String typeStr;
		if (this.typeAttribute != null) {
			typeStr = source.getValue(this.typeAttribute).asString();
		} else {
			if (persistent != null) {
				typeStr = persistent.getType().getName();
			} else {
				throw new UnsupportedOperationException(
					"If the results aren't persistent objects, you must specify the name of the attribute that contains the object type value");
			}
		}

		return new ExportTarget(DctmObjectType.decodeType(typeStr).getStoredObjectType(), id.getId());
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
		} catch (UnsupportedObjectTypeException e) {
			String dump = " (dump failed)";
			try {
				dump = String.format(":%n%s%n", next.dump());
			} catch (DfException e2) {
				if (this.log.isTraceEnabled()) {
					this.log
					.error(String.format("Failed to generate the debug dump for object # %d", this.current), e2);
				}
			}
			throw new RuntimeException(String.format("Item # %d is not a supported export target", this.current, dump),
				e);
		}
	}

	@Override
	public void remove() {
		this.iterator.remove();
	}
}