/**
 *
 */

package com.armedia.cmf.engine.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.TransferContext;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class ExportContext<S, T, V> extends TransferContext<S, T, V> {
	/**
	 * @param rootId
	 * @param session
	 * @param objectStore
	 * @param fileSystem
	 * @param output
	 */
	ExportContext(String rootId, S session, Logger output) {
		super(rootId, session, output);
	}
}