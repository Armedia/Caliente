package com.armedia.caliente.cli.bulkdel;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.tools.dfc.DctmCrypto;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.operations.IDfDeleteOperation;
import com.documentum.operations.IDfOperation;
import com.documentum.operations.IDfOperationError;
import com.documentum.operations.IDfOperationMonitor;
import com.documentum.operations.IDfOperationNode;
import com.documentum.operations.IDfOperationStep;

public class BulkDel {
	private static final String ALL_VERSIONS = "all";
	private static final String SELECTED_VERSIONS = "selected";
	private static final String UNUSED_VERSIONS = "unused";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final DfcLaunchHelper dfcLaunchHelper;

	public BulkDel(DfcLaunchHelper dfcLaunchHelper) {
		this.dfcLaunchHelper = dfcLaunchHelper;
	}

	private List<String> addPredicateTargets(String predicate, IDfSession session, IDfDeleteOperation op)
		throws DfException {
		return null;
	}

	private List<String> addExplicitTargets(CommandLineValues cli, IDfSession session, IDfDeleteOperation op)
		throws DfException {
		List<String> targets = new ArrayList<>();
		for (String spec : cli.getAllStrings(CLIParam.target)) {
			// Spec can either be an object ID or a path... let's first find out
			// if it's an object ID...

			final IDfPersistentObject target;
			if (DfId.isObjectId(spec)) {
				IDfPersistentObject t = null;
				try {
					t = session.getObject(new DfId(spec));
				} catch (DfIdNotFoundException e) {
					// Do nothing...
					t = null;
				} finally {
					target = t;
				}
			} else {
				target = session.getObjectByPath(spec);
			}

			if (target == null) {
				this.log.warn(String.format("Object spec [%s] did not return an object", spec));
				continue;
			}

			if (!IDfSysObject.class.isInstance(target)) {
				this.log.warn(String.format("Object spec [%s] does not refer to an IDfSysObject", spec));
				continue;
			}

			final IDfSysObject obj = IDfSysObject.class.cast(target);
			String type = target.getString("r_object_type");

			final String path;
			if (IDfFolder.class.isInstance(obj)) {
				path = IDfFolder.class.cast(obj).getFolderPath(0);
			} else {
				if (obj.getFolderIdCount() < 1) {
					path = "unknown";
				} else {
					final IDfId parentId = obj.getFolderId(0);
					final String parentPath;
					IDfFolder parent = session.getFolderBySpecification(parentId.getId());
					if (parent == null) {
						this.log.warn("Failed to find the parent folder for %s [%s](%s) - folder [%s] not found", type,
							obj.getObjectName(), obj.getObjectId(), parentId);
						parentPath = "(unknown)";
					} else {
						// TODO: here we once again assume that item 0 will be the "default" path
						// but we already know this assumption is false. In our particular case, we
						// don't care...
						parentPath = parent.getFolderPath(0);
					}
					path = String.format("%s/%s", parentPath, obj.getObjectName());
				}
			}

			this.log.info("Adding %s [%s](%s) for deletion (from spec [%s])%n", type, path,
				target.getObjectId().getId(), spec);
			op.add(obj);
			targets.add(path);
		}
		return targets;
	}

	protected int run(CommandLineValues cli) throws Exception {
		final boolean debug = cli.isPresent(CLIParam.debug);

		final boolean hasPredicate = cli.isPresent(CLIParam.predicate);
		final boolean hasTarget = cli.isPresent(CLIParam.target);
		if (hasPredicate == hasTarget) {
			if (hasPredicate) {
				this.log.error("May only supply either a predicate or a list of targets, but not both");
			} else {
				this.log.error("You must supply one of either a predicate or a list of targets");
			}
			return 1;
		}

		try {
			final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
			final String user = this.dfcLaunchHelper.getDfcUser(cli);
			final String password = this.dfcLaunchHelper.getDfcPassword(cli);

			final DfcSessionPool pool = new DfcSessionPool(docbase, user, new DctmCrypto().decrypt(password));

			final boolean recursive = cli.isPresent(CLIParam.recursive);
			final boolean deleteAllChildren = cli.isPresent(CLIParam.delete_all_children);
			final boolean deleteVdocChildren = cli.isPresent(CLIParam.delete_vdoc_children);
			final boolean noDeref = cli.isPresent(CLIParam.delete_referenced);
			final int versionPolicy;
			if (cli.isPresent(CLIParam.delete_versions)) {
				String str = cli.getString(CLIParam.delete_versions);
				switch (str.toLowerCase()) {
					case SELECTED_VERSIONS:
						versionPolicy = IDfDeleteOperation.SELECTED_VERSIONS;
						break;
					case UNUSED_VERSIONS:
						versionPolicy = IDfDeleteOperation.UNUSED_VERSIONS;
						break;
					case ALL_VERSIONS:
						versionPolicy = IDfDeleteOperation.ALL_VERSIONS;
						break;
					default:
						this.log.error("Unknown version policy value [{}]", str);
						return 1;
				}
			} else {
				versionPolicy = IDfDeleteOperation.ALL_VERSIONS;
			}

			final int onErrorResult = (cli.isPresent(CLIParam.abort_on_error) ? IDfOperationMonitor.ABORT
				: IDfOperationMonitor.CONTINUE);

			try {
				final IDfSession mainSession;
				try {
					mainSession = pool.acquireSession();
				} catch (Exception e) {
					String msg = String.format("Failed to open a session to docbase [%s] as user [%s]", docbase, user);
					if (debug) {
						this.log.error(msg, e);
					} else {
						this.log.error("{}: {}", msg, e.getMessage());
					}
					return 1;
				}

				try {
					IDfClientX client = new DfClientX();
					IDfDeleteOperation op = client.getDeleteOperation();

					if (recursive) {
						op.setDeepFolders(true);
					}
					op.setVersionDeletionPolicy(versionPolicy);
					op.enableDeepDeleteFolderChildren(deleteAllChildren);
					op.enableDeepDeleteVirtualDocumentsInFolders(deleteVdocChildren);
					op.enablePopulateWithReferences(noDeref);

					op.setSession(mainSession);

					op.setOperationMonitor(new IDfOperationMonitor() {
						@Override
						public int reportError(IDfOperationError error) throws DfException {
							IDfOperationNode node = error.getNode();
							if (node != null) {
								BulkDel.this.log.error(String.format("Error deleting object [%s]", node.getId()),
									error.getException());
							} else {
								BulkDel.this.log.error("Operation error detected", error.getException());
							}
							return onErrorResult;
						}

						@Override
						public int progressReport(IDfOperation operation, int operationPercentDone,
							IDfOperationStep step, int stepPercentDone, IDfOperationNode node) throws DfException {
							BulkDel.this.log.info(String.format("%s: %3d%% done (%s: %3d%% done)%n",
								operation.getName(), operationPercentDone, step.getName(), stepPercentDone));
							return IDfOperationMonitor.CONTINUE;
						}

						@Override
						public int getYesNoAnswer(IDfOperationError question) throws DfException {
							boolean ret = true;
							"".hashCode();
							return (ret ? IDfOperationMonitor.YES : IDfOperationMonitor.NO);
						}
					});

					final List<String> targets;
					if (hasPredicate) {
						targets = addPredicateTargets(cli.getString(CLIParam.predicate), mainSession, op);
					} else {
						targets = addExplicitTargets(cli, mainSession, op);
					}
					this.log.info("%d target objects added: %s%n", targets.size(), targets);

					if (op.execute()) {
						System.out.printf("Deletion successful!%n");
						return 0;
					} else {
						System.out.printf("Deletion FAILED!%n");
						return 1;
					}
				} finally {
					pool.releaseSession(mainSession);
				}
			} finally {
				pool.close();
			}
		} catch (DfException e) {
			this.log.error("Documentum exception caught", e);
			return 1;
		}
	}
}