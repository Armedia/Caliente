package com.armedia.caliente.tools;

import java.util.ArrayList;
import java.util.List;

import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.operations.IDfDeleteOperation;
import com.documentum.operations.IDfOperation;
import com.documentum.operations.IDfOperationError;
import com.documentum.operations.IDfOperationMonitor;
import com.documentum.operations.IDfOperationNode;
import com.documentum.operations.IDfOperationStep;

public class DctmRecursiveDelete {

	public void test() throws DfException {
		final DfcSessionPool pool = new DfcSessionPool("dctmvm01", "dctmadmin", "123");
		final IDfSession session = pool.acquireSession();
		try {
			// final IDfLocalTransaction tx = DfUtils.openTransaction(session);
			String[] targets = {
				"/JSAP-2017-03-05", "/JSAP-2017-03-06-01"
			};
			boolean ok = false;
			try {
				IDfClientX client = new DfClientX();
				IDfDeleteOperation op = client.getDeleteOperation();
				List<String> added = new ArrayList<>();
				for (String path : targets) {
					IDfFolder target = session.getFolderByPath(path);
					if (target != null) {
						System.out.printf("Adding folder [%s](%s) for deletion%n", path, target.getObjectId().getId());
						op.add(target);
						added.add(path);
					}
				}
				System.out.printf("%d Target folders added: %s%n", added.size(), added);
				op.setDeepFolders(ok);
				op.setSession(session);
				op.setOperationMonitor(new IDfOperationMonitor() {

					@Override
					public int reportError(IDfOperationError error) throws DfException {
						IDfOperationNode node = error.getNode();
						if (node != null) {
							System.err.printf("Error deleting folder [%s]: %s%n", node.getId(), error.getMessage());
						} else {
							System.err.printf("Operation error detected: %s%n", error.getMessage());
						}
						return IDfOperationMonitor.CONTINUE;
					}

					@Override
					public int progressReport(IDfOperation operation, int operationPercentDone, IDfOperationStep step,
						int stepPercentDone, IDfOperationNode node) throws DfException {
						System.out.printf("%s: %3d%% done (%s: %3d%% done)%n", operation.getName(),
							operationPercentDone, step.getName(), stepPercentDone);
						return IDfOperationMonitor.CONTINUE;
					}

					@Override
					public int getYesNoAnswer(IDfOperationError question) throws DfException {
						boolean ret = true;
						"".hashCode();
						return (ret ? IDfOperationMonitor.YES : IDfOperationMonitor.NO);
					}
				});
				if (op.execute()) {
					System.out.printf("Deletion successful!%n");
				} else {
					System.out.printf("Deletion FAILED!%n");
				}

				// DfUtils.commitTransaction(session, tx);
				ok = true;
			} finally {
				if (!ok) {
					// DfUtils.abortTransaction(session, tx);
				}
			}
		} finally {
			pool.releaseSession(session);
		}
	}
}