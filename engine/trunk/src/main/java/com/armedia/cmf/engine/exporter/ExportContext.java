/**
 *
 */

package com.armedia.cmf.engine.exporter;

import java.util.Stack;

import org.slf4j.Logger;

import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferContext;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class ExportContext<S, T, V> extends TransferContext<S, T, V> {

	private final Stack<ExportTarget> referrents = new Stack<ExportTarget>();

	/**
	 * @param rootId
	 * @param rootType
	 * @param session
	 * @param output
	 */
	public <C extends ExportContext<S, T, V>, W extends SessionWrapper<S>, E extends ExportEngine<S, W, T, V, C>, F extends ExportContextFactory<S, W, T, V, C, E>> ExportContext(
		F factory, CfgTools settings, String rootId, StoredObjectType rootType, S session, Logger output) {
		super(factory, settings, rootId, rootType, session, output);
	}

	final void pushReferrent(ExportTarget referrent) {
		if (referrent == null) { throw new IllegalArgumentException("Must provide a referrent object to track"); }
		this.referrents.push(referrent);
	}

	public final ExportTarget getReferrent() {
		if (this.referrents.isEmpty()) { return null; }
		return this.referrents.peek();
	}

	final ExportTarget popReferrent() {
		if (this.referrents.isEmpty()) { return null; }
		return this.referrents.pop();
	}

}