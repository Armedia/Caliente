/**
 *
 */

package com.armedia.cmf.engine.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.TransferContext;
import com.armedia.cmf.storage.StoredObjectType;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class ExportContext<S, T, V> extends TransferContext<S, T, V> {
	/**
	 * @param rootId
	 * @param rootType
	 * @param session
	 * @param output
	 */
	public ExportContext(ExportEngine<S, ?, T, V, ?> engine, String rootId, StoredObjectType rootType, S session,
		Logger output) {
		super(engine, rootId, rootType, session, output);
	}
}