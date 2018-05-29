/**
 *
 */

package com.armedia.caliente.engine.exporter;

import java.util.Stack;

import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferContext;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class ExportContext< //
	SESSION, //
	VALUE, //
	EXPORT_CONTEXT_FACTORY extends ExportContextFactory<SESSION, ?, VALUE, ?, ?> //
> extends TransferContext<SESSION, VALUE, EXPORT_CONTEXT_FACTORY> {

	private final Stack<ExportTarget> referrents = new Stack<>();

	/**
	 * @param rootId
	 * @param rootType
	 * @param session
	 * @param output
	 */
	public <C extends ExportContext<SESSION, VALUE, EXPORT_CONTEXT_FACTORY>, W extends SessionWrapper<SESSION>, E extends ExportEngine<SESSION, W, VALUE, C, ?, ?>> ExportContext(
		EXPORT_CONTEXT_FACTORY factory, CfgTools settings, String rootId, CmfType rootType, SESSION session, Logger output,
		WarningTracker tracker) {
		super(factory, settings, rootId, rootType, session, output, tracker);
	}

	final void pushReferrent(ExportTarget referrent) {
		if (referrent == null) { throw new IllegalArgumentException("Must provide a referrent object to track"); }
		this.referrents.push(referrent);
	}

	public boolean shouldWaitForRequirement(CmfType referrent, CmfType referenced) {
		return false;
	}

	public final boolean isReferrentLoop(ExportTarget referrent) {
		return this.referrents.contains(referrent);
	}

	public final ExportTarget getReferrent() {
		if (this.referrents.isEmpty()) { return null; }
		return this.referrents.peek();
	}

	ExportTarget popReferrent() {
		if (this.referrents.isEmpty()) { return null; }
		return this.referrents.pop();
	}

}