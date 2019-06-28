/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.activation.MimeType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmDataType;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.DctmVdocMember;
import com.armedia.caliente.engine.dfc.common.DctmDocument;
import com.armedia.caliente.engine.dfc.common.DctmSysObject;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper.Mapping;
import com.armedia.caliente.tools.dfc.DfValueFactory;
import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.caliente.tools.dfc.DfcVersionNumber;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfVirtualDocument;
import com.documentum.fc.client.IDfVirtualDocumentNode;
import com.documentum.fc.client.impl.ISysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

/**
 *
 *
 */
public class DctmImportDocument extends DctmImportSysObject<IDfSysObject> implements DctmDocument {

	private static final String DEFAULT_BINARY_MIME = "application/octet-stream";
	private static final String DEFAULT_BINARY_FORMAT = "binary";

	private TemporaryPermission antecedentTemporaryPermission = null;
	private TemporaryPermission branchTemporaryPermission = null;

	protected DctmImportDocument(DctmImportDelegateFactory factory, CmfObject<IDfValue> storedObject) throws Exception {
		super(factory, IDfSysObject.class, DctmObjectType.DOCUMENT, storedObject);
	}

	@Override
	protected boolean skipImport(DctmImportContext ctx) throws ImportException, DfException {
		// We can't import this document if the destination directory is "/"
		ctx.printf("HISTORY COUNTER: [%08x] from history [%s]", ctx.getHistoryPosition(),
			this.cmfObject.getHistoryId());
		CmfProperty<IDfValue> path = this.cmfObject.getProperty(IntermediateProperty.PATH);
		if (path == null) { return true; }
		for (Iterator<IDfValue> it = path.iterator(); it.hasNext();) {
			IDfValue v = it.next();
			final String tgt = ctx.getTargetPath(v.asString());
			if (Tools.equals("", tgt)) {
				it.remove();
			}
			if (Tools.equals("/", tgt)) {
				it.remove();
			}
		}
		if (!path.hasValues()) {
			this.log.warn("Can't import {}  without a parent FOLDER or CABINET - skipping",
				this.cmfObject.getDescription());
			return true;
		}
		return super.skipImport(ctx);
	}

	private String calculateVersionString(IDfSysObject document, boolean full) throws DfException {
		if (!full) {
			return String.format("%s%s", document.getImplicitVersionLabel(),
				document.getHasFolder() ? String.format(",%s", ISysObject.CURRENT_VERSION_LABEL) : "");
		}
		int labelCount = document.getVersionLabelCount();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < labelCount; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(document.getVersionLabel(i));
		}
		return sb.toString();
	}

	@Override
	protected String calculateLabel(IDfSysObject document) throws DfException, ImportException {
		final int folderCount = document.getFolderIdCount();
		for (int i = 0; i < folderCount; i++) {
			IDfId id = document.getFolderId(i);
			IDfFolder f = IDfFolder.class.cast(document.getSession().getFolderBySpecification(id.getId()));
			if (f != null) {
				String path = (f.getFolderPathCount() > 0 ? f.getFolderPath(0)
					: String.format("(unknown-folder:[%s])", id.getId()));
				return String.format("%s/%s [%s]", path, document.getObjectName(),
					calculateVersionString(document, true));
			}
		}
		throw new ImportException(
			String.format("None of the parent paths for object [%s] were found", document.getObjectId().getId()));
	}

	@Override
	protected IDfSysObject locateInCms(DctmImportContext ctx) throws ImportException, DfException {
		final IDfSession session = ctx.getSession();

		if (isReference()) { return locateExistingByPath(ctx); }

		// First things first: are we the root of the version hierarchy?
		CmfAttribute<IDfValue> chronicleAtt = this.cmfObject.getAttribute(DctmAttributes.I_CHRONICLE_ID);
		final Mapping chronicleMapping;
		CmfAttribute<IDfValue> implicitLabelAtt = this.cmfObject.getAttribute(DctmAttributes.R_VERSION_LABEL);
		final String implicitLabel = (implicitLabelAtt != null ? implicitLabelAtt.getValue().asString() : null);
		if (chronicleAtt != null) {
			String sourceChronicleId = chronicleAtt.getValue().asId().getId();
			// Map to the new chronicle ID, from the old one...try for the quick win
			chronicleMapping = ctx.getValueMapper().getTargetMapping(this.cmfObject.getType(),
				DctmAttributes.I_CHRONICLE_ID, sourceChronicleId);
		} else {
			chronicleMapping = null;
		}

		// If we don't have a chronicle mapping, we're likely the root document and thus
		// will have to search by path...
		final String chronicleId;
		IDfSysObject existing = null;
		if (chronicleMapping != null) {
			chronicleId = chronicleMapping.getTargetValue();
		} else {
			// We don't know the chronicle, so look by path. We look at all the paths this
			// object is expected to take up on the target, and they must refer to either
			// no existing object, or exactly one existing object. If there is an existing
			// object, it's replaced by this one.
			existing = locateExistingByPath(ctx);

			// If we found no match via path, then we can't locate a match at all and must assume
			// that this object is a new object
			if (implicitLabel == null) { return existing; }
			if (existing == null) { return null; }

			// We have a match, but it may not be the version we seek, so
			// track the chronicle so the code below can find the right version.
			chronicleId = existing.getChronicleId().getId();
		}

		// Using the chronicle ID and the implicit version ID, we will seek out
		// the exact existing version.
		IDfPersistentObject obj = session.getObjectByQualification(
			String.format("dm_sysobject (all) where i_chronicle_id = '%s' and any r_version_label = %s", chronicleId,
				DfcUtils.quoteString(implicitLabel)));

		// Return whatever we found...if we found nothing, then this is a new version
		// and must be handled as such
		return castObject(obj);
	}

	@Override
	protected boolean isVersionable(IDfSysObject object) throws DfException {
		return true;
	}

	@Override
	protected IDfId persistChanges(IDfSysObject document, DctmImportContext context)
		throws DfException, ImportException {
		// Apparently, references require no saving
		if (isReference()) { return document.getObjectId(); }
		return super.persistChanges(document, context);
	}

	@Override
	protected boolean isSameObject(IDfSysObject object, DctmImportContext ctx) throws DfException, ImportException {
		// If we're a reference, and there's something there already, we don't import...
		if (isReference()) { return true; }
		// We only care about the "standard criteria" if we're the root version...
		if (ctx.getHistoryPosition() == 0) { return super.isSameObject(object, ctx); }
		// If we're not the root version, then we assume this is the same object since it has the
		// same version number and is associated to the same chronicle (this has already been
		// established during the import process)...
		return true;
	}

	protected IDfSysObject newDocument(DctmImportContext context) throws DfException, ImportException {
		IDfSysObject doc = super.newObject(context);
		setVirtualDocumentFlag(doc);
		return doc;
	}

	@Override
	protected IDfSysObject newObject(DctmImportContext context) throws DfException, ImportException {

		if (isReference()) { return newReference(context); }

		final CmfAttribute<IDfValue> sourceChronicleAtt = this.cmfObject.getAttribute(DctmAttributes.I_CHRONICLE_ID);
		final String sourceChronicleId = (sourceChronicleAtt != null ? sourceChronicleAtt.getValue().asString() : null);

		// If we have no chronicle info to look for, we don't try to...
		final CmfProperty<IDfValue> rootProp = this.cmfObject.getProperty(IntermediateProperty.VERSION_TREE_ROOT);
		final boolean root = ((rootProp != null) && rootProp.hasValues() && rootProp.getValue().asBoolean());
		if (root) {
			// This is the start of a new chronicle
			final IDfSysObject newDoc = newDocument(context);
			context.getValueMapper().setMapping(this.cmfObject.getType(), DctmAttributes.I_CHRONICLE_ID,
				sourceChronicleId, newDoc.getChronicleId().getId());
			return newDoc;
		}

		final IDfSession session = context.getSession();

		final CmfAttribute<IDfValue> antecedentAtt = this.cmfObject.getAttribute(DctmAttributes.I_ANTECEDENT_ID);
		// First things first: if the exact antecedent we're looking for exists, then we use that
		// and continue normally
		if ((antecedentAtt != null) && antecedentAtt.hasValues()) {
			IDfId aid = antecedentAtt.getValue().asId();
			Mapping mapping = context.getValueMapper().getTargetMapping(this.cmfObject.getType(),
				DctmAttributes.R_OBJECT_ID, aid.getId());
			// This mapping can only exist (i.e. be non-null) if we actually processed the
			// antecedent during this run
			if (mapping != null) {
				return createSuccessorVersion(castObject(session.getObject(new DfId(mapping.getTargetValue()))), null,
					context);
			}
		}

		// Its exact antecedent isn't there, so we try for the patched one...
		final IDfId antecedentId;
		IDfSysObject antecedentVersion = null;
		final CmfProperty<IDfValue> antecedentProperty = this.cmfObject.getProperty(DctmSysObject.PATCH_ANTECEDENT);
		if (antecedentProperty == null) {
			antecedentId = (antecedentAtt != null ? antecedentAtt.getValue().asId() : DfId.DF_NULLID);
		} else {
			IDfId aid = antecedentProperty.getValue().asId();
			if (aid.isObjectId()) {
				antecedentId = aid;
			} else {
				antecedentId = null;
				Mapping mapping = context.getValueMapper().getTargetMapping(this.cmfObject.getType(),
					DctmAttributes.I_CHRONICLE_ID, sourceChronicleId);
				if (mapping != null) {
					// Find the antecedent using the expected antecedent version number, which
					// by now *should* exist as part of the normal import...
					String dql = String.format(
						"dm_sysobject (ALL) where i_chronicle_id = '%s' and any r_version_label = '%s'",
						mapping.getTargetValue(), antecedentProperty.getValue().asString());
					antecedentVersion = castObject(session.getObjectByQualification(dql));
				}
			}
		}

		boolean substituteRoot = false;
		if (antecedentVersion == null) {
			Mapping mapping = null;
			if (antecedentId != null) {
				mapping = context.getValueMapper().getTargetMapping(this.cmfObject.getType(),
					DctmAttributes.R_OBJECT_ID, antecedentId.getId());
			}
			if (mapping == null) {
				// The root of the trunk is missing...we'll need to create a new, contentless
				// object
				antecedentVersion = newDocument(context);

				// Set the name
				antecedentVersion
					.setObjectName(this.cmfObject.getAttribute(DctmAttributes.OBJECT_NAME).getValue().asString());

				// And...finally...use this object moving forward
				substituteRoot = true;
			} else {
				IDfId id = new DfId(mapping.getTargetValue());
				session.flushObject(id);
				antecedentVersion = castObject(session.getObject(id));
			}
		}

		CmfProperty<IDfValue> patches = this.cmfObject.getProperty(DctmSysObject.VERSION_PATCHES);
		if ((patches != null) && (patches.getValueCount() > 0)) {
			// Patches are required...so let's carry them out!
			// At the end of patching, antecedentVersion should point to the actual
			// version that will be checked out/branched (i.e. the LAST patch version added)
			// If there is no object, and a root must be created, then do so as well
			IDfSysObject lastAntecedent = null;
			IDfValue lastAntecedentVersion = null;
			for (IDfValue p : patches) {
				// Now we checkout and checkin and branch and whatnot as necessary until we can
				// actually proceed with the rest of the algorithm...
				final CmfProperty<IDfValue> prop = new CmfProperty<>(DctmAttributes.R_VERSION_LABEL,
					CmfValue.Type.STRING, false, DfValueFactory.of(p.toString()));
				if (substituteRoot) {
					antecedentVersion.save();
					// Don't need to do this again...
					substituteRoot = false;
				}
				IDfSysObject patchDocument = createSuccessorVersion(antecedentVersion, prop, context);
				IDfId checkinId = persistNewVersion(patchDocument, p.asString(), context);
				cleanUpTemporaryPermissions(session);
				resetMutabilityFlags();

				// If we branched, we don't change antecedents
				lastAntecedent = castObject(session.getObject(checkinId));
				lastAntecedentVersion = p;
				if (!context.getValue(DctmImportSysObject.BRANCH_MARKER).asBoolean()) {
					antecedentVersion = lastAntecedent;
				}
			}

			// If this version is to be a successor of the last antecedent (all components are the
			// same except the last number), then we go ahead and assign it to antecedentVersion. If
			// it's a sibling (same-level branch), or a descendant (sub-branch), then we leave the
			// antecedent where it is
			DfcVersionNumber newVersion = new DfcVersionNumber(
				this.cmfObject.getAttribute(DctmAttributes.R_VERSION_LABEL).getValue().asString());
			DfcVersionNumber lastVersion = new DfcVersionNumber(lastAntecedentVersion.asString());
			if (newVersion.isSuccessorOf(lastVersion)) {
				antecedentVersion = lastAntecedent;
			}
		} else if (substituteRoot) {
			// If there were no gaps to patch up, then this is the valid starting point
			// for the history hierarchy.
			return antecedentVersion;
		}

		return createSuccessorVersion(antecedentVersion, null, context);
	}

	private IDfSysObject createSuccessorVersion(IDfSysObject antecedentVersion, CmfProperty<IDfValue> rVersionLabel,
		DctmImportContext context) throws ImportException, DfException {
		final IDfSession session = context.getSession();
		antecedentVersion.fetch(null);
		this.antecedentTemporaryPermission = new TemporaryPermission(context.getSession(), antecedentVersion,
			IDfACL.DF_PERMIT_DELETE);
		if (this.antecedentTemporaryPermission.grant(antecedentVersion)) {
			antecedentVersion.save();
		}

		// This is so later on we can decide whether we should set or clear the immutable flag,
		// but only if we're a leaf node
		CmfAttribute<IDfValue> descendantCount = this.cmfObject.getAttribute(DctmAttributes.I_DIRECT_DSC);
		if ((descendantCount != null) && (descendantCount.getValue().asInteger() == 0)) {
			detectIncomingMutability();
		}

		if (rVersionLabel == null) {
			rVersionLabel = this.cmfObject.getAttribute(DctmAttributes.R_VERSION_LABEL);
		}
		String antecedentVersionImplicitVersionLabel = antecedentVersion.getImplicitVersionLabel();
		String documentImplicitVersionLabel = rVersionLabel.getValue().asString();

		int antecedentDots = StringUtils.countMatches(antecedentVersionImplicitVersionLabel, ".");
		int documentDots = StringUtils.countMatches(documentImplicitVersionLabel, ".");

		final boolean shouldBranch = (documentDots == (antecedentDots + 2));
		context.setValue(DctmImportSysObject.BRANCH_MARKER, DfValueFactory.of(shouldBranch));
		if (shouldBranch) {
			// branch
			IDfId branchID = antecedentVersion.branch(antecedentVersionImplicitVersionLabel);
			antecedentVersion = castObject(session.getObject(branchID));
			setVirtualDocumentFlag(antecedentVersion);
			this.branchTemporaryPermission = new TemporaryPermission(context.getSession(), antecedentVersion,
				IDfACL.DF_PERMIT_DELETE);
			this.branchTemporaryPermission.grant(antecedentVersion);
		} else {
			// checkout
			this.branchTemporaryPermission = null;
			antecedentVersion.checkout();
			setVirtualDocumentFlag(antecedentVersion);
			handleVirtualDocumentMembers(antecedentVersion, context);
			antecedentVersion.fetch(null);
		}
		setOwnerGroupACLData(antecedentVersion, context);
		return antecedentVersion;
	}

	private void cleanUpTemporaryPermissions(IDfSession session) throws DfException {
		if (this.antecedentTemporaryPermission != null) {
			IDfId antecedentId = new DfId(this.antecedentTemporaryPermission.getObjectId());
			IDfSysObject antecedent = castObject(session.getObject(antecedentId));
			session.flushObject(antecedentId);
			antecedent.fetch(null);
			if (this.antecedentTemporaryPermission.revoke(antecedent)) {
				antecedent.save();
			}
			this.antecedentTemporaryPermission = null;
		}

		if (this.branchTemporaryPermission != null) {
			IDfId branchId = new DfId(this.branchTemporaryPermission.getObjectId());
			IDfSysObject branch = castObject(session.getObject(branchId));
			session.flushObject(branchId);
			branch.fetch(null);
			if (this.branchTemporaryPermission.revoke(branch)) {
				branch.save();
			}
			this.branchTemporaryPermission = null;
		}
	}

	@Override
	protected void prepareOperation(IDfSysObject sysObject, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
		super.prepareOperation(sysObject, newObject, context);
		if (!newObject) {
			// We only do this for old objects because new objects will have been
			// "fixed" in this respect in newObject()
			detectAndClearMutability(sysObject);
		}
	}

	@Override
	protected void prepareForConstruction(IDfSysObject document, boolean newObject, DctmImportContext context)
		throws DfException {

		// Is root?
		CmfProperty<IDfValue> p = this.cmfObject.getProperty(IntermediateProperty.VERSION_TREE_ROOT);
		CmfAttribute<IDfValue> att = this.cmfObject.getAttribute(DctmAttributes.I_CHRONICLE_ID);
		String sourceChronicleId = (att != null ? att.getValue().asId().getId() : null);
		final boolean root = (((p != null) && p.hasValues() && p.getValue().asBoolean()) || (sourceChronicleId == null)
			|| Tools.equals(this.cmfObject.getId(), sourceChronicleId));
		if (!root && !newObject) {
			this.antecedentTemporaryPermission = new TemporaryPermission(context.getSession(), document,
				IDfACL.DF_PERMIT_VERSION);
			if (this.antecedentTemporaryPermission.grant(document)) {
				// Not sure this is OK...
				if (!document.isCheckedOut()) {
					document.save();
				}
			}
		}
	}

	protected void saveContentStream(DctmImportContext context, IDfSysObject document, CmfContentStream info,
		CmfContentStore<?, ?>.Handle contentHandle, String contentType, String fullFormat, int pageNumber,
		int renditionNumber, String pageModifier, int currentContent, int totalContentCount)
		throws DfException, ImportException {
		// Step one: what's the content's path in the filesystem?
		final IDfSession session = context.getSession();
		File path;
		try {
			path = contentHandle.getFile();
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to get the content file for %s, qualifier [%s]",
				this.cmfObject.getDescription(), contentHandle.getInfo()), e);
		}

		if (path == null) {
			// If the content store doesn't support this, then we dump to a temp file and go from
			// there
			// TODO: Look into IDfSysObjectInternal for use of streams vs. using filesystem staging
			try {
				path = File.createTempFile("content", null);
				contentHandle.writeFile(path);
				path.deleteOnExit();
			} catch (IOException e) {
				if (path != null) {
					path.delete();
				}
				throw new ImportException(
					String.format("Failed to create and write the temporary content file for %s, qualifier [%s]",
						this.cmfObject.getDescription(), contentHandle.getInfo()),
					e);
			} catch (CmfStorageException e) {
				if (path != null) {
					path.delete();
				}
				throw new ImportException(String.format("Failed to get the content stream for %s, qualifier [%s]",
					this.cmfObject.getDescription(), contentHandle.getInfo()), e);
			}
		}

		final String absolutePath = path.getAbsolutePath();

		fullFormat = Tools.coalesce(fullFormat, contentType);

		if (renditionNumber == 0) {
			if (!Tools.equals(fullFormat, contentType)) {
				fullFormat = contentType;
			}
			try {
				document.setFileEx(absolutePath, fullFormat, pageNumber, null);
				final String msg = String.format("Added the primary content to document [%s](%s) -> {%s/%s/%s}",
					this.cmfObject.getLabel(), this.cmfObject.getId(), absolutePath, fullFormat, pageNumber);
				DctmImportDocument.this.log.info(msg);
				context.printf("\t%s (item %d of %d)", msg, currentContent, totalContentCount);
			} catch (DfException e) {
				final String msg = String.format("Failed to add the primary content to document [%s](%s) -> {%s/%s/%s}",
					this.cmfObject.getLabel(), this.cmfObject.getId(), absolutePath, fullFormat, pageNumber);
				context.printf("\t%s (item %d of %d): %s [%s]", msg, currentContent, totalContentCount,
					e.getClass().getCanonicalName(), e.getMessage());
				throw new ImportException(msg, e);
			}
		} else if (Tools.equals(contentType, fullFormat) && StringUtils.isEmpty(pageModifier)) {
			// If the rendition is of the same format as the main content, then we MUST skip it
			final String msg = String.format("Skipped a rendition for document [%s](%s) -> {%s/%s/%s/%s}",
				this.cmfObject.getLabel(), this.cmfObject.getId(), absolutePath, fullFormat, pageNumber, pageModifier);
			context.printf("\t%s (item %d of %d)", msg, currentContent, totalContentCount);
			return;
		} else {
			try {
				document.addRenditionEx2(absolutePath, fullFormat, pageNumber, pageModifier, null, false, false, false);
				final String msg = String.format("Added rendition content to document [%s](%s) -> {%s/%s/%s/%s}",
					this.cmfObject.getLabel(), this.cmfObject.getId(), absolutePath, fullFormat, pageNumber,
					pageModifier);
				DctmImportDocument.this.log.info(msg);
				context.printf("\t%s (item %d of %d)", msg, currentContent, totalContentCount);
			} catch (DfException e) {
				final String msg = String.format(
					"Failed to add rendition content to document [%s](%s) -> {%s/%s/%s/%s}", this.cmfObject.getLabel(),
					this.cmfObject.getId(), absolutePath, fullFormat, pageNumber, pageModifier);
				context.printf("\t%s (item %d of %d): %s [%s]", msg, currentContent, totalContentCount,
					e.getClass().getCanonicalName(), e.getMessage());
				throw new ImportException(msg, e);
			}
		}

		String setFile = info.getProperty(DctmAttributes.SET_FILE);
		if (StringUtils.isBlank(setFile)) {
			setFile = " ";
		}

		String setClient = info.getProperty(DctmAttributes.SET_CLIENT);
		if (StringUtils.isBlank(setClient)) {
			setClient = " ";
		}

		String setTimeStr = info.getProperty(DctmAttributes.SET_TIME);
		IDfTime setTime = DfTime.DF_NULLDATE;
		if (setTimeStr != null) {
			setTime = new DfTime(setTimeStr, DctmDocument.CONTENT_SET_TIME_PATTERN);
		}

		if (!StringUtils.isBlank(pageModifier)) {
			pageModifier = DfcUtils.quoteStringForSql(pageModifier);
		} else {
			pageModifier = "dcr.page_modifier";
		}

		// Prepare the sql to be executed
		String sql = "" //
			+ "UPDATE dmr_content_s SET " //
			+ "       set_file = %s, " //
			+ "       set_client = %s, " //
			+ "       set_time = %s " //
			+ " WHERE r_object_id = (" //
			+ "           select dcs.r_object_id " //
			+ "             from dmr_content_s dcs, dmr_content_r dcr " //
			+ "            where dcr.parent_id = %s " //
			+ "              and dcs.r_object_id = dcr.r_object_id " //
			+ "              and dcs.rendition = %d " //
			+ "              and dcr.page_modifier = %s " //
			+ "              and dcr.page = %d " //
			+ "              and dcs.full_format = %s" //
			+ "       )";

		if (setTime.isNullDate() || !setTime.isValid()) {
			setTimeStr = "set_time";
		} else {
			setTimeStr = DfcUtils.generateSqlDateClause(setTime.getDate(), session);
		}

		final String documentId = document.getObjectId().getId();
		try {
			// Run the exec sql
			sql = String.format(sql, DfcUtils.quoteStringForSql(setFile), DfcUtils.quoteStringForSql(setClient),
				setTimeStr, DfcUtils.quoteStringForSql(documentId), renditionNumber, pageModifier, pageNumber,
				DfcUtils.quoteStringForSql(fullFormat));
			if (!runExecSQL(session, sql)) {
				final String msg = String.format(
					"SQL Execution failed for updating the content's system attributes for document [%s](%s) -> {%s/%s/%s/%s}:%n%s%n",
					this.cmfObject.getLabel(), this.cmfObject.getId(), absolutePath, fullFormat, pageNumber,
					pageModifier, sql);
				context.printf("\t%s (item %d of %d)", msg, currentContent, totalContentCount);
				throw new ImportException(msg);
			}
			return;
		} catch (DfException e) {
			final String msg = String.format(
				"Exception caught generating updating the content's system attributes for document [%s](%s) -> {%s/%s/%s/%s}",
				this.cmfObject.getLabel(), this.cmfObject.getId(), absolutePath, fullFormat, pageNumber, pageModifier);
			context.printf("\t%s (item %d of %d): %s [%s]", msg, currentContent, totalContentCount,
				e.getClass().getCanonicalName(), e.getMessage());
			throw new ImportException(msg, e);
		}
	}

	protected String identifyFormat(IDfSession session, String aContentType, String extension) throws DfException {
		// We have a mime type or format name ... try a format name first
		// Shortcut - avoid checking if it's the default binary type (application/octet-stream)
		if (Tools.equals(aContentType, DctmImportDocument.DEFAULT_BINARY_MIME)) { return null; }

		// Not a format...must be a mime type

		if (extension != null) {
			// We have an extension, so try to identify it based on mime type +
			// extension...though this may hardly be unique...
			String dql = "select distinct name from dm_format where mime_type = %s and dos_extension = %s";
			try (DfcQuery query = new DfcQuery(session,
				String.format(dql, DfcUtils.quoteString(aContentType), DfcUtils.quoteString(extension)),
				DfcQuery.Type.DF_EXECREAD_QUERY)) {
				if (query.hasNext()) { return query.next().getString("name"); }
			}
		}

		String dql = "select distinct name from dm_format where mime_type = %s";
		try (DfcQuery query = new DfcQuery(session, String.format(dql, DfcUtils.quoteString(aContentType)),
			DfcQuery.Type.DF_EXECREAD_QUERY)) {
			if (query.hasNext()) { return query.next().getString("name"); }
		}

		return null;
	}

	protected String determineFormat(IDfSession session, MimeType fallbackType) throws DfException {
		String extension = FilenameUtils
			.getExtension(this.cmfObject.getAttribute(DctmAttributes.OBJECT_NAME).getValue().asString()).toLowerCase();
		if (StringUtils.isBlank(extension)) {
			extension = null;
		}

		CmfAttribute<IDfValue> att = this.cmfObject.getAttribute(DctmAttributes.A_CONTENT_TYPE);
		String aContentType = ((att != null) && att.hasValues() ? att.getValue().asString() : null);

		// Is this a format?
		if (aContentType != null) {
			IDfFormat format = session.getFormat(aContentType);
			if (format != null) { return format.getName(); }
		}

		// Not a format, so it must be a mime type... so we use its declared type, and the
		// type identified from the stream
		List<String> mimeTypes = new ArrayList<>();
		if (aContentType != null) {
			mimeTypes.add(aContentType);
		}
		if (fallbackType != null) {
			mimeTypes.add(fallbackType.getBaseType());
		}
		for (String t : mimeTypes) {
			String ret = identifyFormat(session, t, extension);
			if (ret != null) { return ret; }
		}

		// If we got this far, it means we didn't get hits with the mime types with or without
		// extension, so now we fall back to the extension if that's all we have
		if (extension != null) {
			String dql = "select distinct name from dm_format where dos_extension = %s";
			try (DfcQuery query = new DfcQuery(session, String.format(dql, DfcUtils.quoteString(extension)),
				DfcQuery.Type.DF_EXECREAD_QUERY)) {
				if (query.hasNext()) { return query.next().getString("name"); }
			}
		}

		// No hits...nothing to be done...
		return DctmImportDocument.DEFAULT_BINARY_FORMAT;
	}

	protected boolean loadContent(final IDfSysObject document, boolean newObject, final DctmImportContext context)
		throws DfException, ImportException {
		List<CmfContentStream> infoList;
		try {
			infoList = context.getContentStreams(this.cmfObject);
		} catch (Exception e) {
			throw new ImportException(String.format("Failed to load the content info for %s [%s](%s)",
				this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}
		CmfContentStore<?, ?> contentStore = context.getContentStore();
		int i = 0;
		final CmfAttributeTranslator<IDfValue> translator = this.factory.getEngine().getTranslator();
		String contentType = null;
		Boolean fromDctm = null;
		final boolean skipRenditions = this.factory.isSkipRenditions();
		for (CmfContentStream info : infoList) {
			if (skipRenditions && !info.isDefaultRendition()) {
				// Skip the non-default rendition
				continue;
			}
			CmfContentStore<?, ?>.Handle h = contentStore.getHandle(translator, this.cmfObject, info);
			CfgTools cfg = info.getCfgTools();

			if (fromDctm == null) {
				// This is the canary in my coal mine - if this is from Documentum, there will be a
				// set_file property we can retrieve. If not, then there won't. We only check once.
				fromDctm = cfg.hasValue(DctmAttributes.SET_FILE);
			}

			if (contentType == null) {
				// We only perform this determination for the first stream on the list, as
				// this will determine the object's type
				contentType = determineFormat(context.getSession(), info.getMimeType());
			}
			String fullFormat = cfg.getString(DctmAttributes.FULL_FORMAT);
			int rendition = i;
			int page = 0;
			String pageModifier = info.getModifier();
			if (StringUtils.isEmpty(pageModifier)) {
				pageModifier = cfg.getString(DctmAttributes.PAGE_MODIFIER,
					DctmDataType.DF_STRING.getNullValue().asString());
			}

			if (fromDctm) {
				// Parse out the rendition number for this one...
				info.getRenditionIdentifier();
				rendition = (info.isDefaultRendition() ? 0
					: Integer.valueOf(info.getProperty(DctmAttributes.RENDITION)));
				if ((rendition == 0) && !info.isDefaultRendition()) {
					this.log.warn("{} {} has a non-default rendition with an ID of 0, which is not allowed ({})",
						this.cmfObject.getSubtype(), this.cmfObject.getDescription(), info);
					continue;
				}
				page = info.getRenditionPage();
			} else {
				// If this isn't a documentum stream, then the rendition number must be the current
				// iteration, so that the main rendition is always the primary stream
				this.log.info("Storing rendition #{} (r=[{}], p={}, i={}) for {} [{}]({})", i + 1,
					info.getRenditionIdentifier(), info.getRenditionPage(), i, this.cmfObject.getType(),
					this.cmfObject.getLabel(), this.cmfObject.getId());
			}

			saveContentStream(context, document, info, h, contentType, fullFormat, page, rendition, pageModifier, ++i,
				infoList.size());
		}

		return false;
	}

	protected boolean setVirtualDocumentFlag(final IDfSysObject document) throws DfException {
		final CmfAttribute<IDfValue> a = this.cmfObject.getAttribute(DctmAttributes.R_IS_VIRTUAL_DOC);
		final boolean vdocFlag = ((a != null) && a.hasValues() && a.getValue().asBoolean());
		if (vdocFlag != document.isVirtualDocument()) {
			document.setIsVirtualDocument(vdocFlag);
		}
		return vdocFlag;
	}

	protected void handleVirtualDocumentMembers(final IDfSysObject document, final DctmImportContext context)
		throws DfException, ImportException {
		final boolean addVdocMembers;
		if (document.isVirtualDocument() || (document.getLinkCount() > 0)) {
			addVdocMembers = true;
		} else {
			CmfAttribute<IDfValue> b = this.cmfObject.getAttribute(DctmAttributes.R_LINK_CNT);
			addVdocMembers = ((b != null) && b.hasValues() && (b.getValue().asInteger() > 0));
		}

		if (addVdocMembers && document.isCheckedOut()) {
			CmfProperty<IDfValue> p = this.cmfObject.getProperty(IntermediateProperty.VDOC_MEMBER);
			IDfVirtualDocument vdoc = document.asVirtualDocument(ISysObject.CURRENT_VERSION_LABEL, false);
			IDfVirtualDocumentNode root = vdoc.getRootNode();

			// First, remove all the existing nodes
			int childCount = root.getChildCount();
			if (childCount > 0) {
				for (int i = 0; i < childCount; i++) {
					// Always fetch the first node
					vdoc.removeNode(root.getChild(0));
				}
			}

			// Now, add all the "new" nodes. The alternative is to run a complex comparison
			// algorithm that checks to see which nodes are new, where they should be inserted,
			// reordered, etc. That's too much work, and we expect it would achieve the same
			// result, but with less effort and complexity
			if ((p != null) && p.hasValues()) {
				IDfVirtualDocumentNode prev = null;
				childCount = p.getValueCount();
				int i = 1;
				context.printf("\tAdding %d Virtual Document child nodes for [%s](%s)", childCount,
					this.cmfObject.getLabel(), this.cmfObject.getId());
				for (IDfValue v : p) {
					DctmVdocMember member = new DctmVdocMember(v.asString());
					Mapping m = context.getValueMapper().getTargetMapping(CmfObject.Archetype.DOCUMENT,
						DctmAttributes.I_CHRONICLE_ID, member.getChronicleId().getId());
					if (m == null) {
						throw new ImportException(String.format(
							"Virtual Document [%s](%s) references a component [%s] which could not be located (maybe it hasn't been imported yet?)",
							this.cmfObject.getLabel(), this.cmfObject.getId(), v.asString()));
					}
					IDfSysObject so = IDfSysObject.class.cast(context.getSession().getObjectByQualification(String
						.format("dm_sysobject where i_chronicle_id = %s", DfcUtils.quoteString(m.getTargetValue()))));
					if (so == null) {
						throw new ImportException(String.format(
							"Virtual Document [%s](%s) references a component [%s] which could not be located, but may have failed during import",
							this.cmfObject.getLabel(), this.cmfObject.getId(), m.getTargetValue()));
					}

					final String childBinding = (StringUtils.isBlank(member.getBinding())
						? ISysObject.CURRENT_VERSION_LABEL
						: member.getBinding());
					final boolean childIsVirtualDoc = (so.isVirtualDocument() || (so.getLinkCount() > 0));
					final boolean followAssembly = childIsVirtualDoc ? member.isFollowAssembly() : false;
					final boolean overrideLateBinding = childIsVirtualDoc ? member.isOverrideLateBinding() : false;

					context.printf("\t\tAdding child node #%d (of %d) to VDoc [%s](%s) (%s|%s|%s|%s)", i, childCount,
						this.cmfObject.getLabel(), this.cmfObject.getId(), so.getChronicleId().getId(), childBinding,
						followAssembly, overrideLateBinding);
					prev = vdoc.addNode(root, prev, so.getChronicleId(), childBinding, followAssembly,
						overrideLateBinding);
					i++;
				}
			}

			if (root.getChildCount() == 0) {
				// Revert the virtual document flag
				document.setIsVirtualDocument(false);
			}
		}
	}

	@Override
	protected void doFinalizeConstruction(final IDfSysObject document, boolean newObject,
		final DctmImportContext context) throws DfException, ImportException {

		// References don't require any of this being done
		if (isReference()) { return; }

		if (!context.getSettings().getBoolean(TransferSetting.IGNORE_CONTENT)) {
			loadContent(document, newObject, context);
		}
	}

	@Override
	protected boolean cleanupAfterSave(IDfSysObject document, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
		final IDfSession session = context.getSession();

		cleanUpParents(session);
		cleanUpTemporaryPermissions(session);

		return super.cleanupAfterSave(document, newObject, context);
	}
}