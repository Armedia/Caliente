/**
 *
 */

package com.delta.cmsmf.runtime;

import com.delta.cmsmf.cms.pool.DctmSessionManager;
import com.delta.cmsmf.mainEngine.CLIParam;
import com.delta.cmsmf.mainEngine.CMSMFLauncher;
import com.documentum.fc.client.IDfSession;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class DctmConnectionPool {

	private static final DctmSessionManager SESSION_MANAGER;

	static {
		final String docbase = CMSMFLauncher.getParameter(CLIParam.docbase);
		final String username = CMSMFLauncher.getParameter(CLIParam.user);
		final String password = CMSMFLauncher.getParameter(CLIParam.password);
		SESSION_MANAGER = new DctmSessionManager(docbase, username, password);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				DctmConnectionPool.SESSION_MANAGER.close();
			}
		});
	}

	private DctmConnectionPool() {
	}

	public static IDfSession acquireSession() {
		return DctmConnectionPool.SESSION_MANAGER.acquireSession();
	}

	public static void releaseSession(IDfSession session) {
		DctmConnectionPool.SESSION_MANAGER.releaseSession(session);
	}
}