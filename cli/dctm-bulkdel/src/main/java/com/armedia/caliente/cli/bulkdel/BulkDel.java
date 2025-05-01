/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.bulkdel;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.dfc.DfcCrypto;
import com.armedia.caliente.tools.dfc.cli.DfcLaunchHelper;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.LazyFormatter;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.cli.OptionValues;
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

	private List<String> addExplicitTargets(OptionValues cli, IDfSession session, IDfDeleteOperation op)
		throws DfException {
		List<String> targets = new ArrayList<>();
		for (String spec : cli.getStrings(CLIParam.target)) {
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
				this.log.warn("Object spec [{}] did not return an object", spec);
				continue;
			}

			if (!IDfSysObject.class.isInstance(target)) {
				this.log.warn("Object spec [{}] does not refer to an IDfSysObject", spec);
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
						this.log.warn("Failed to find the parent folder for - folder [%s] not found", type,
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

	protected int run(OptionValues cli) throws Exception {
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

			final DfcSessionPool pool = new DfcSessionPool(docbase, user, DfcCrypto.INSTANCE.decrypt(password));

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
				} catch (DfException e) {
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
								BulkDel.this.log.error("Error deleting object [{}]", node.getId(),
									error.getException());
							} else {
								BulkDel.this.log.error("Operation error detected", error.getException());
							}
							return onErrorResult;
						}

						@Override
						public int progressReport(IDfOperation operation, int operationPercentDone,
							IDfOperationStep step, int stepPercentDone, IDfOperationNode node) throws DfException {
							BulkDel.this.log.info("{}: {}% done ({}: {}% done){}", operation.getName(),
								LazyFormatter.of("%3d", operationPercentDone), step.getName(),
								LazyFormatter.of("%3d", stepPercentDone), Tools.NL);
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
					this.log.info("{} target objects added: {}", targets.size(), targets);

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