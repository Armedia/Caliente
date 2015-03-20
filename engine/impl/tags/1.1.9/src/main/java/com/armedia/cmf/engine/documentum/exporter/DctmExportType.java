/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.Collection;

import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.storage.StoredObject;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportType extends DctmExportAbstract<IDfType> {

	protected DctmExportType(DctmExportEngine engine) {
		super(engine, DctmObjectType.TYPE);
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfType type) throws DfException {
		String superName = type.getSuperName();
		if ((superName != null) && (superName.length() > 0)) {
			superName = String.format(" (extends %s)", superName);
		} else {
			superName = "";
		}
		return String.format("%s%s", type.getName(), superName);
	}

	@Override
	protected String calculateBatchId(IDfSession session, IDfType type) throws DfException {
		// Calculate the maximum depth that this folder resides in, from its parents.
		// Keep track of visited nodes, and explode on a loop.
		// We return it in zero-padded hex to allow for large numbers (up to 2^64
		// depth), and also maintain consistent sorting
		return String.format("%016x", calculateDepth(session, type));
	}

	private int calculateDepth(IDfSession session, IDfType type) throws DfException {
		if (type == null) { return -1; }
		return calculateDepth(session, type.getSuperType()) + 1;
	}

	@Override
	protected Collection<IDfPersistentObject> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfType type, DctmExportContext ctx) throws Exception {
		Collection<IDfPersistentObject> ret = super.findRequirements(session, marshaled, type, ctx);
		IDfType superType = type.getSuperType();
		if (superType != null) {
			if (ctx.isSpecialType(superType.getName())) {
				this.log.warn(String.format("Will not export special type [%s] (supertype of [%s])",
					superType.getName(), type.getName()));
			} else {
				ret.add(superType);
			}
		}
		return ret;
	}
}