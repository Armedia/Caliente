package com.delta.cmsmf.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Enumeration;

import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfContentCollection;
import com.documentum.fc.client.IDfEnumeration;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfRelation;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfValidator;
import com.documentum.fc.client.IDfVersionLabels;
import com.documentum.fc.client.IDfVersionPolicy;
import com.documentum.fc.client.IDfVirtualDocument;
import com.documentum.fc.client.acs.IDfAcsTransferPreferences;
import com.documentum.fc.client.acs.IDfContentTransferCapability;
import com.documentum.fc.client.content.IDfContentAvailability;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfList;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

public abstract class DfVersionTreePatch<T extends IDfSysObject> implements IDfSysObject {
	protected final T base;
	protected final DfVersionNumber patchNumber;

	protected DfVersionTreePatch(Class<T> klass, T base, DfVersionNumber patchNumber) {
		if (klass == null) { throw new IllegalArgumentException("Must provide an expected base class"); }
		if (base == null) { throw new IllegalArgumentException("Must provide a valid base object"); }
		if (patchNumber == null) { throw new IllegalArgumentException("Must provide a valid patch number"); }
		if (base instanceof DfVersionTreePatch) {
			@SuppressWarnings("unchecked")
			DfVersionTreePatch<T> patch = (DfVersionTreePatch<T>) base;
			if (!klass.isInstance(patch.base)) { throw new ClassCastException(String.format(
				"Wrong type of version tree patch - expected a %s, but got a %s", klass.getCanonicalName(), patch.base
					.getClass().getCanonicalName())); }
			this.base = patch.getBase();
		} else {
			this.base = base;
		}
		this.patchNumber = patchNumber;
	}

	public final T getBase() {
		return this.base;
	}

	public final DfVersionNumber getPatchNumber() {
		return this.patchNumber;
	}

	@Override
	public final IDfRelation addChildRelative(String relationTypeName, IDfId childId, String childLabel,
		boolean isPermanent, String description) throws DfException {
		return this.base.addChildRelative(relationTypeName, childId, childLabel, isPermanent, description);
	}

	@Override
	public final IDfId addDigitalSignature(String userName, String reason) throws DfException {
		return this.base.addDigitalSignature(userName, reason);
	}

	@Override
	public final IDfId addESignature(String userName, String password, String signatureJustification,
		String formatToSign, String hashAlgorithm, String preSignatureHash, String signatureMethodName,
		String applicationProperties, String passThroughArgument1, String passThroughArgument2) throws DfException {
		return this.base.addESignature(userName, password, signatureJustification, formatToSign, hashAlgorithm,
			preSignatureHash, signatureMethodName, applicationProperties, passThroughArgument1, passThroughArgument2);
	}

	@Override
	public final void addNote(IDfId targetId, boolean keepPermanent) throws DfException {
		this.base.addNote(targetId, keepPermanent);
	}

	@Override
	public final IDfRelation addParentRelative(String relationTypeName, IDfId parentId, String childLabel,
		boolean isPermanent, String description) throws DfException {
		return this.base.addParentRelative(relationTypeName, parentId, childLabel, isPermanent, description);
	}

	@Override
	public final IDfId addReference(IDfId folderId, String bindingCondition, String bindingLabel) throws DfException {
		return this.base.addReference(folderId, bindingCondition, bindingLabel);
	}

	@Override
	public final void addRendition(String fileName, String formatName) throws DfException {
		this.base.addRendition(fileName, formatName);
	}

	@Override
	public final void addRenditionEx(String fileName, String formatName, int pageNumber, String storageName,
		boolean atomic) throws DfException {
		this.base.addRenditionEx(fileName, formatName, pageNumber, storageName, atomic);
	}

	@Override
	public final void addRenditionEx2(String fileName, String formatName, int pageNumber, String pageModifier,
		String storageName, boolean atomic, boolean keep, boolean batch) throws DfException {
		this.base.addRenditionEx2(fileName, formatName, pageNumber, pageModifier, storageName, atomic, keep, batch);
	}

	@Override
	public final void addRenditionEx3(String fileName, String formatName, int pageNumber, String pageModifier,
		String storageName, boolean atomic, boolean keep, boolean batch, String otherFileName) throws DfException {
		this.base.addRenditionEx3(fileName, formatName, pageNumber, pageModifier, storageName, atomic, keep, batch,
			otherFileName);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final boolean apiExec(String cmd, String args) throws DfException {
		return this.base.apiExec(cmd, args);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final String apiGet(String cmd, String args) throws DfException {
		return this.base.apiGet(cmd, args);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final boolean apiSet(String cmd, String args, String value) throws DfException {
		return this.base.apiSet(cmd, args, value);
	}

	@Override
	public final void appendBoolean(String attributeName, boolean value) throws DfException {
		this.base.appendBoolean(attributeName, value);
	}

	@Override
	public final void appendContent(ByteArrayOutputStream content) throws DfException {
		this.base.appendContent(content);
	}

	@Override
	public final void appendContentEx(ByteArrayOutputStream content, boolean other) throws DfException {
		this.base.appendContentEx(content, other);
	}

	@Override
	public final void appendDouble(String attributeName, double value) throws DfException {
		this.base.appendDouble(attributeName, value);
	}

	@Override
	public final void appendFile(String fileName) throws DfException {
		this.base.appendFile(fileName);
	}

	@Override
	public final void appendFileEx(String fileName, String otherFileName) throws DfException {
		this.base.appendFileEx(fileName, otherFileName);
	}

	@Override
	public final void appendId(String attributeName, IDfId value) throws DfException {
		this.base.appendId(attributeName, value);
	}

	@Override
	public final void appendInt(String attributeName, int value) throws DfException {
		this.base.appendInt(attributeName, value);
	}

	@Override
	public final IDfId appendPart(IDfId componentId, String versionLabel, boolean useNodeVerLabel,
		boolean followAssembly, int copyChild) throws DfException {
		return this.base.appendPart(componentId, versionLabel, useNodeVerLabel, followAssembly, copyChild);
	}

	@Override
	public final void appendString(String attributeName, String value) throws DfException {
		this.base.appendString(attributeName, value);
	}

	@Override
	public final void appendTime(String attributeName, IDfTime value) throws DfException {
		this.base.appendTime(attributeName, value);
	}

	@Override
	public final void appendValue(String attributeName, IDfValue value) throws DfException {
		this.base.appendValue(attributeName, value);
	}

	@Override
	public final boolean areAttributesModifiable() throws DfException {
		return this.base.areAttributesModifiable();
	}

	@Override
	public final IDfVirtualDocument asVirtualDocument(String lateBindingValue, boolean followRootAssembly)
		throws DfException {
		return this.base.asVirtualDocument(lateBindingValue, followRootAssembly);
	}

	@Override
	public final IDfCollection assemble(IDfId virtualDocId, int interruptFreq, String qualification, String nodesortList)
		throws DfException {
		return this.base.assemble(virtualDocId, interruptFreq, qualification, nodesortList);
	}

	@Override
	public final void attachPolicy(IDfId policyId, String state, String scope) throws DfException {
		this.base.attachPolicy(policyId, state, scope);
	}

	@Override
	public final void bindFile(int pageNumber, IDfId srcId, int srcPageNumber) throws DfException {
		this.base.bindFile(pageNumber, srcId, srcPageNumber);
	}

	@Override
	public final IDfId branch(String versionLabel) throws DfException {
		return this.base.branch(versionLabel);
	}

	@Override
	public final boolean canDemote() throws DfException {
		return this.base.canDemote();
	}

	@Override
	public final boolean canPromote() throws DfException {
		return this.base.canPromote();
	}

	@Override
	public final boolean canResume() throws DfException {
		return this.base.canResume();
	}

	@Override
	public final boolean canSuspend() throws DfException {
		return this.base.canSuspend();
	}

	@Override
	public final void cancelCheckout() throws DfException {
		this.base.cancelCheckout();
	}

	@Override
	public final void cancelCheckoutEx(boolean sendMail, String compoundValue, String specialValue) throws DfException {
		this.base.cancelCheckoutEx(sendMail, compoundValue, specialValue);
	}

	@Override
	public final void cancelScheduledDemote(IDfTime scheduleDate) throws DfException {
		this.base.cancelScheduledDemote(scheduleDate);
	}

	@Override
	public final void cancelScheduledPromote(IDfTime scheduleDate) throws DfException {
		this.base.cancelScheduledPromote(scheduleDate);
	}

	@Override
	public final void cancelScheduledResume(IDfTime schedule) throws DfException {
		this.base.cancelScheduledResume(schedule);
	}

	@Override
	public final void cancelScheduledSuspend(IDfTime scheduleDate) throws DfException {
		this.base.cancelScheduledSuspend(scheduleDate);
	}

	@Override
	public final IDfId checkin(boolean keepLock, String versionLabels) throws DfException {
		return this.base.checkin(keepLock, versionLabels);
	}

	@Override
	public final IDfId checkinEx(boolean keepLock, String versionLabels, String oldCompoundArchValue,
		String oldSpecialAppValue, String newCompoundArchValue, String newSpecialAppValue) throws DfException {
		return this.base.checkinEx(keepLock, versionLabels, oldCompoundArchValue, oldSpecialAppValue,
			newCompoundArchValue, newSpecialAppValue);
	}

	@Override
	public final void checkout() throws DfException {
		this.base.checkout();
	}

	@Override
	public final IDfId checkoutEx(String versionLabel, String compoundArchValue, String specialAppValue)
		throws DfException {
		return this.base.checkoutEx(versionLabel, compoundArchValue, specialAppValue);
	}

	@Override
	public final void demote(String state, boolean toBase) throws DfException {
		this.base.demote(state, toBase);
	}

	@Override
	public final void destroy() throws DfException {
		this.base.destroy();
	}

	@Override
	public final void destroyAllVersions() throws DfException {
		this.base.destroyAllVersions();
	}

	@Override
	public final void detachPolicy() throws DfException {
		this.base.detachPolicy();
	}

	@Override
	public final void disassemble() throws DfException {
		this.base.disassemble();
	}

	@Override
	public final String dump() throws DfException {
		return this.base.dump();
	}

	@Override
	public final Enumeration<?> enumAttrs() throws DfException {
		return this.base.enumAttrs();
	}

	@Override
	public final boolean fetch(String typeNameIgnored) throws DfException {
		return this.base.fetch(typeNameIgnored);
	}

	@Override
	public final boolean fetchWithCaching(String currencyCheckValue, boolean usePersistentCache, boolean useSharedCache)
		throws DfException {
		return this.base.fetchWithCaching(currencyCheckValue, usePersistentCache, useSharedCache);
	}

	@Override
	public final int findAttrIndex(String attributeName) throws DfException {
		return this.base.findAttrIndex(attributeName);
	}

	@Override
	public final int findBoolean(String attributeName, boolean value) throws DfException {
		return this.base.findBoolean(attributeName, value);
	}

	@Override
	public final int findDouble(String attributeName, double value) throws DfException {
		return this.base.findDouble(attributeName, value);
	}

	@Override
	public final int findId(String attributeName, IDfId value) throws DfException {
		return this.base.findId(attributeName, value);
	}

	@Override
	public final int findInt(String attributeName, int value) throws DfException {
		return this.base.findInt(attributeName, value);
	}

	@Override
	public final int findString(String attributeName, String value) throws DfException {
		return this.base.findString(attributeName, value);
	}

	@Override
	public final int findTime(String attributeName, IDfTime value) throws DfException {
		return this.base.findTime(attributeName, value);
	}

	@Override
	public final int findValue(String attributeName, IDfValue value) throws DfException {
		return this.base.findValue(attributeName, value);
	}

	@Override
	public final void freeze(boolean freezeComponents) throws DfException {
		this.base.freeze(freezeComponents);
	}

	@Override
	public final IDfACL getACL() throws DfException {
		return this.base.getACL();
	}

	@Override
	public final String getACLDomain() throws DfException {
		return this.base.getACLDomain();
	}

	@Override
	public final String getACLName() throws DfException {
		return this.base.getACLName();
	}

	@Override
	public final IDfTime getAccessDate() throws DfException {
		return this.base.getAccessDate();
	}

	@Override
	public final String getAccessorApplicationPermit(int index) throws DfException {
		return this.base.getAccessorApplicationPermit(index);
	}

	@Override
	public final int getAccessorCount() throws DfException {
		return this.base.getAccessorCount();
	}

	@Override
	public final String getAccessorName(int index) throws DfException {
		return this.base.getAccessorName(index);
	}

	@Override
	public final String getAccessorPermit(int index) throws DfException {
		return this.base.getAccessorPermit(index);
	}

	@Override
	public final int getAccessorPermitType(int index) throws DfException {
		return this.base.getAccessorPermitType(index);
	}

	@Override
	public final int getAccessorXPermit(int index) throws DfException {
		return this.base.getAccessorXPermit(index);
	}

	@Override
	public final String getAccessorXPermitNames(int index) throws DfException {
		return this.base.getAccessorXPermitNames(index);
	}

	@Override
	public final boolean getAclRefValid() throws DfException {
		return this.base.getAclRefValid();
	}

	@Override
	public final IDfEnumeration getAcsRequests(String formatName, int pageNumber, String pageModifier,
		IDfAcsTransferPreferences transferPreferences) throws DfException {
		return this.base.getAcsRequests(formatName, pageNumber, pageModifier, transferPreferences);
	}

	@Override
	public final String getAliasSet() throws DfException {
		return this.base.getAliasSet();
	}

	@Override
	public final IDfId getAliasSetId() throws DfException {
		return this.base.getAliasSetId();
	}

	@Override
	public final String getAllRepeatingStrings(String attributeName, String separator) throws DfException {
		return this.base.getAllRepeatingStrings(attributeName, separator);
	}

	@Override
	public final IDfId getAntecedentId() throws DfException {
		return this.base.getAntecedentId();
	}

	@Override
	public final String getApplicationType() throws DfException {
		return this.base.getApplicationType();
	}

	@Override
	public final IDfId getAssembledFromId() throws DfException {
		return this.base.getAssembledFromId();
	}

	@Override
	public final IDfAttr getAttr(int attrIndex) throws DfException {
		return this.base.getAttr(attrIndex);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final IDfList getAttrAssistance(String attrName) throws DfException {
		return this.base.getAttrAssistance(attrName);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final IDfList getAttrAssistanceWithValues(String attrName, IDfList depAttrNameList,
		IDfList depAttrValueListList) throws DfException {
		return this.base.getAttrAssistanceWithValues(attrName, depAttrNameList, depAttrValueListList);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final IDfList getAttrAsstDependencies(String attrName) throws DfException {
		return this.base.getAttrAsstDependencies(attrName);
	}

	@Override
	public final int getAttrCount() throws DfException {
		return this.base.getAttrCount();
	}

	@Override
	public final int getAttrDataType(String attributeName) throws DfException {
		return this.base.getAttrDataType(attributeName);
	}

	@Override
	public final String getAuthors(int index) throws DfException {
		return this.base.getAuthors(index);
	}

	@Override
	public final int getAuthorsCount() throws DfException {
		return this.base.getAuthorsCount();
	}

	@Override
	public final boolean getBoolean(String attributeName) throws DfException {
		return this.base.getBoolean(attributeName);
	}

	@Override
	public final int getBranchCount() throws DfException {
		return this.base.getBranchCount();
	}

	@Override
	public final IDfId getCabinetId() throws DfException {
		return this.base.getCabinetId();
	}

	@Override
	public final IDfCollection getChildRelatives(String relationTypeName) throws DfException {
		return this.base.getChildRelatives(relationTypeName);
	}

	@Override
	public final IDfId getChronicleId() throws DfException {
		return this.base.getChronicleId();
	}

	@Override
	public final IDfCollection getCollectionForContent(String format, int pageNumber) throws DfException {
		return this.base.getCollectionForContent(format, pageNumber);
	}

	@Override
	public final IDfCollection getCollectionForContentEx2(String format, int pageNumber, String pageModifier)
		throws DfException {
		return this.base.getCollectionForContentEx2(format, pageNumber, pageModifier);
	}

	@Override
	public final IDfCollection getCollectionForContentEx3(String format, int pageNumber, String pageModifier,
		boolean other) throws DfException {
		return this.base.getCollectionForContentEx3(format, pageNumber, pageModifier, other);
	}

	@Override
	public final IDfContentCollection getCollectionForContentEx4(String format, int pageNumber, String pageModifier,
		boolean other) throws DfException {
		return this.base.getCollectionForContentEx4(format, pageNumber, pageModifier, other);
	}

	@Override
	public final IDfId getComponentId(int index) throws DfException {
		return this.base.getComponentId(index);
	}

	@Override
	public final int getComponentIdCount() throws DfException {
		return this.base.getComponentIdCount();
	}

	@Override
	public final String getCompoundArchitecture() throws DfException {
		return this.base.getCompoundArchitecture();
	}

	@Override
	public final IDfId getContainId(int index) throws DfException {
		return this.base.getContainId(index);
	}

	@Override
	public final int getContainIdCount() throws DfException {
		return this.base.getContainIdCount();
	}

	@Override
	public final ByteArrayInputStream getContent() throws DfException {
		return this.base.getContent();
	}

	@Override
	public final IDfContentAvailability getContentAvailability(String formatName, int pageNumber, String pageModifier,
		String networkLocationIdentifier) throws DfException {
		return this.base.getContentAvailability(formatName, pageNumber, pageModifier, networkLocationIdentifier);
	}

	@Override
	public final ByteArrayInputStream getContentEx(String format, int pageNumber) throws DfException {
		return this.base.getContentEx(format, pageNumber);
	}

	@Override
	public final ByteArrayInputStream getContentEx2(String format, int pageNumber, String pageModifier)
		throws DfException {
		return this.base.getContentEx2(format, pageNumber, pageModifier);
	}

	@Override
	public final ByteArrayInputStream getContentEx3(String format, int pageNumber, String pageModifier, boolean other)
		throws DfException {
		return this.base.getContentEx3(format, pageNumber, pageModifier, other);
	}

	@Override
	public final long getContentSize() throws DfException {
		return this.base.getContentSize();
	}

	@Override
	public final long getContentSize(int page, String formatName, String pageModifier) throws DfException {
		return this.base.getContentSize(page, formatName, pageModifier);
	}

	@Override
	public final int getContentState(int index) throws DfException {
		return this.base.getContentState(index);
	}

	@Override
	public final int getContentStateCount() throws DfException {
		return this.base.getContentStateCount();
	}

	@Override
	public final String getContentType() throws DfException {
		return this.base.getContentType();
	}

	@Override
	public final IDfId getContentsId() throws DfException {
		return this.base.getContentsId();
	}

	@Override
	public final IDfTime getCreationDate() throws DfException {
		return this.base.getCreationDate();
	}

	@Override
	public final String getCreatorName() throws DfException {
		return this.base.getCreatorName();
	}

	@Override
	public final int getCurrentState() throws DfException {
		return this.base.getCurrentState();
	}

	@Override
	public final String getCurrentStateName() throws DfException {
		return this.base.getCurrentStateName();
	}

	@Override
	public final String getDirectDescendant() throws DfException {
		return this.base.getDirectDescendant();
	}

	@Override
	public final double getDouble(String attributeName) throws DfException {
		return this.base.getDouble(attributeName);
	}

	@Override
	public final Double getDoubleContentAttr(String name, String formatName, int page, String pageModifier)
		throws DfException {
		return this.base.getDoubleContentAttr(name, formatName, page, pageModifier);
	}

	@Override
	public final String getExceptionStateName() throws DfException {
		return this.base.getExceptionStateName();
	}

	@Override
	public final String getFile(String fileName) throws DfException {
		return this.base.getFile(fileName);
	}

	@Override
	public final String getFileEx(String fileName, String formatName, int pageNumber, boolean other) throws DfException {
		return this.base.getFileEx(fileName, formatName, pageNumber, other);
	}

	@Override
	public final String getFileEx2(String fileName, String formatName, int pageNumber, String pageModifier,
		boolean other) throws DfException {
		return this.base.getFileEx2(fileName, formatName, pageNumber, pageModifier, other);
	}

	@Override
	public final IDfId getFolderId(int index) throws DfException {
		return this.base.getFolderId(index);
	}

	@Override
	public final int getFolderIdCount() throws DfException {
		return this.base.getFolderIdCount();
	}

	@Override
	public final IDfFormat getFormat() throws DfException {
		return this.base.getFormat();
	}

	@Override
	public final int getFrozenAssemblyCount() throws DfException {
		return this.base.getFrozenAssemblyCount();
	}

	@Override
	public final boolean getFullText() throws DfException {
		return this.base.getFullText();
	}

	@Override
	public final String getGroupName() throws DfException {
		return this.base.getGroupName();
	}

	@Override
	public final int getGroupPermit() throws DfException {
		return this.base.getGroupPermit();
	}

	@Override
	public final boolean getHasEvents() throws DfException {
		return this.base.getHasEvents();
	}

	@Override
	public final boolean getHasFolder() throws DfException {
		return this.base.getHasFolder();
	}

	@Override
	public final boolean getHasFrozenAssembly() throws DfException {
		return this.base.getHasFrozenAssembly();
	}

	@Override
	public final IDfId getId(String attributeName) throws DfException {
		return this.base.getId(attributeName);
	}

	@Override
	public final String getImplicitVersionLabel() throws DfException {
		return this.base.getImplicitVersionLabel();
	}

	@Override
	public final int getInt(String attributeName) throws DfException {
		return this.base.getInt(attributeName);
	}

	@Override
	public final String getKeywords(int index) throws DfException {
		return this.base.getKeywords(index);
	}

	@Override
	public final int getKeywordsCount() throws DfException {
		return this.base.getKeywordsCount();
	}

	@Override
	public final boolean getLatestFlag() throws DfException {
		return this.base.getLatestFlag();
	}

	@Override
	public final int getLinkCount() throws DfException {
		return this.base.getLinkCount();
	}

	@Override
	public final int getLinkHighCount() throws DfException {
		return this.base.getLinkHighCount();
	}

	@Override
	public final IDfCollection getLocations(String attrNames) throws DfException {
		return this.base.getLocations(attrNames);
	}

	@Override
	public final IDfTime getLockDate() throws DfException {
		return this.base.getLockDate();
	}

	@Override
	public final String getLockMachine() throws DfException {
		return this.base.getLockMachine();
	}

	@Override
	public final String getLockOwner() throws DfException {
		return this.base.getLockOwner();
	}

	@Override
	public final String getLogEntry() throws DfException {
		return this.base.getLogEntry();
	}

	@Override
	public final long getLong(String attributeName) throws DfException {
		return this.base.getLong(attributeName);
	}

	@Override
	public final String getMasterDocbase() throws DfException {
		return this.base.getMasterDocbase();
	}

	@Override
	public final String getModifier() throws DfException {
		return this.base.getModifier();
	}

	@Override
	public final IDfTime getModifyDate() throws DfException {
		return this.base.getModifyDate();
	}

	@Override
	public final String getNextStateName() throws DfException {
		return this.base.getNextStateName();
	}

	@Override
	public final IDfId getObjectId() {
		return DfId.DF_NULLID;
	}

	@Override
	public final String getObjectName() throws DfException {
		return this.base.getObjectName();
	}

	@Override
	public final IDfSession getObjectSession() {
		return this.base.getObjectSession();
	}

	@Override
	public final IDfSession getOriginalSession() {
		return this.base.getOriginalSession();
	}

	@Override
	public final long getOtherFileSize(int page, String formatName, String pageModifier) throws DfException {
		return this.base.getOtherFileSize(page, formatName, pageModifier);
	}

	@Override
	public final String getOwnerName() throws DfException {
		return this.base.getOwnerName();
	}

	@Override
	public final int getOwnerPermit() throws DfException {
		return this.base.getOwnerPermit();
	}

	@Override
	public final int getPageCount() throws DfException {
		return this.base.getPageCount();
	}

	@Override
	public final IDfCollection getParentRelatives(String relationTypeName) throws DfException {
		return this.base.getParentRelatives(relationTypeName);
	}

	@Override
	public final int getPartition() throws DfException {
		return this.base.getPartition();
	}

	@Override
	public final String getPath(int pageNumber) throws DfException {
		return this.base.getPath(pageNumber);
	}

	@Override
	public final String getPathEx(int pageNumber, String pageModifier) throws DfException {
		return this.base.getPathEx(pageNumber, pageModifier);
	}

	@Override
	public final String getPathEx2(String formatName, int pageNumber, String pageModifier, boolean other)
		throws DfException {
		return this.base.getPathEx2(formatName, pageNumber, pageModifier, other);
	}

	@Override
	public final IDfList getPermissions() throws DfException {
		return this.base.getPermissions();
	}

	@Override
	public final int getPermit() throws DfException {
		return this.base.getPermit();
	}

	@Override
	public final int getPermitEx(String accessorName) throws DfException {
		return this.base.getPermitEx(accessorName);
	}

	@Override
	public final IDfId getPolicyId() throws DfException {
		return this.base.getPolicyId();
	}

	@Override
	public final String getPolicyName() throws DfException {
		return this.base.getPolicyName();
	}

	@Override
	public final String getPreviousStateName() throws DfException {
		return this.base.getPreviousStateName();
	}

	@Override
	public final int getReferenceCount() throws DfException {
		return this.base.getReferenceCount();
	}

	@Override
	public final IDfId getRemoteId() throws DfException {
		return this.base.getRemoteId();
	}

	@Override
	public final IDfCollection getRenditions(String attrNames) throws DfException {
		return this.base.getRenditions(attrNames);
	}

	@Override
	public final boolean getRepeatingBoolean(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingBoolean(attributeName, valueIndex);
	}

	@Override
	public final double getRepeatingDouble(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingDouble(attributeName, valueIndex);
	}

	@Override
	public final IDfId getRepeatingId(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingId(attributeName, valueIndex);
	}

	@Override
	public final int getRepeatingInt(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingInt(attributeName, valueIndex);
	}

	@Override
	public final long getRepeatingLong(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingLong(attributeName, valueIndex);
	}

	@Override
	public final String getRepeatingString(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingString(attributeName, valueIndex);
	}

	@Override
	public final IDfTime getRepeatingTime(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingTime(attributeName, valueIndex);
	}

	@Override
	public final IDfValue getRepeatingValue(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingValue(attributeName, valueIndex);
	}

	@Override
	public final String getResolutionLabel() throws DfException {
		return this.base.getResolutionLabel();
	}

	@Override
	public final int getResumeState() throws DfException {
		return this.base.getResumeState();
	}

	@Override
	public final String getResumeStateName() throws DfException {
		return this.base.getResumeStateName();
	}

	@Override
	public final IDfTime getRetainUntilDate() throws DfException {
		return this.base.getRetainUntilDate();
	}

	@Override
	public final int getRetainerCount() throws DfException {
		return this.base.getRetainerCount();
	}

	@Override
	public final IDfId getRetainerId(int index) throws DfException {
		return this.base.getRetainerId(index);
	}

	@Override
	public final IDfTime getRetentionDate() throws DfException {
		return this.base.getRetentionDate();
	}

	@Override
	public final IDfCollection getRouters(String additionalAttributes, String orderBy) throws DfException {
		return this.base.getRouters(additionalAttributes, orderBy);
	}

	@Override
	public final IDfSession getSession() {
		return this.base.getSession();
	}

	@Override
	public final IDfSessionManager getSessionManager() {
		return this.base.getSessionManager();
	}

	@Override
	public final String getSpecialApp() throws DfException {
		return this.base.getSpecialApp();
	}

	@Override
	public final String getStatus() throws DfException {
		return this.base.getStatus();
	}

	@Override
	public final String getStorageType() throws DfException {
		return this.base.getStorageType();
	}

	@Override
	public final String getString(String attributeName) throws DfException {
		return this.base.getString(attributeName);
	}

	@Override
	public final String getStringContentAttr(String name, String formatName, int page, String pageModifier)
		throws DfException {
		return this.base.getStringContentAttr(name, formatName, page, pageModifier);
	}

	@Override
	public final String getSubject() throws DfException {
		return this.base.getSubject();
	}

	@Override
	public final IDfTime getTime(String attributeName) throws DfException {
		final IDfValue v = getValue(attributeName);
		return (v != null ? v.asTime() : null);
	}

	@Override
	public final IDfTime getTimeContentAttr(String name, String formatName, int page, String pageModifier)
		throws DfException {
		return this.base.getTimeContentAttr(name, formatName, page, pageModifier);
	}

	@Override
	public final String getTitle() throws DfException {
		return this.base.getTitle();
	}

	@Override
	public final IDfType getType() throws DfException {
		return this.base.getType();
	}

	@Override
	public final String getTypeName() throws DfException {
		return this.base.getTypeName();
	}

	@Override
	public final int getVStamp() throws DfException {
		return this.base.getVStamp();
	}

	@Override
	public final IDfValidator getValidator() throws DfException {
		return this.base.getValidator();
	}

	@Override
	public final IDfValue getValue(String attributeName) throws DfException {
		return this.base.getValue(attributeName);
	}

	@Override
	public final IDfValue getValueAt(int index) throws DfException {
		return this.base.getValueAt(index);
	}

	@Override
	public final int getValueCount(String attributeName) throws DfException {
		return this.base.getValueCount(attributeName);
	}

	@Override
	public final IDfCollection getVdmPath(IDfId rootId, boolean shortestPath, String versionList) throws DfException {
		return this.base.getVdmPath(rootId, shortestPath, versionList);
	}

	@Override
	public final IDfCollection getVdmPathDQL(IDfId rootId, boolean shortestPath, String parentType,
		String bindingLabel, String nodeSortBy) throws DfException {
		return this.base.getVdmPathDQL(rootId, shortestPath, parentType, bindingLabel, nodeSortBy);
	}

	@Override
	public final String getVersionLabel(int index) throws DfException {
		return this.base.getVersionLabel(index);
	}

	@Override
	public final int getVersionLabelCount() throws DfException {
		return this.base.getVersionLabelCount();
	}

	@Override
	public final IDfVersionLabels getVersionLabels() throws DfException {
		return this.base.getVersionLabels();
	}

	@Override
	public final IDfVersionPolicy getVersionPolicy() throws DfException {
		return this.base.getVersionPolicy();
	}

	@Override
	public final IDfCollection getVersions(String attrNames) throws DfException {
		return this.base.getVersions(attrNames);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final String getWidgetType(int environment, String attrName) throws DfException {
		return this.base.getWidgetType(environment, attrName);
	}

	@Override
	public final IDfCollection getWorkflows(String additionalAttributes, String orderBy) throws DfException {
		return this.base.getWorkflows(additionalAttributes, orderBy);
	}

	@Override
	public final int getWorldPermit() throws DfException {
		return this.base.getWorldPermit();
	}

	@Override
	public final int getXPermit(String accessorName) throws DfException {
		return this.base.getXPermit(accessorName);
	}

	@Override
	public final String getXPermitList() throws DfException {
		return this.base.getXPermitList();
	}

	@Override
	public final String getXPermitNames(String accessorName) throws DfException {
		return this.base.getXPermitNames(accessorName);
	}

	@Override
	public final void grant(String accessorName, int accessorPermit, String extendedPermission) throws DfException {
		this.base.grant(accessorName, accessorPermit, extendedPermission);
	}

	@Override
	public final void grantPermit(IDfPermit permit) throws DfException {
		this.base.grantPermit(permit);
	}

	@Override
	public final boolean hasAttr(String attributeName) throws DfException {
		return this.base.hasAttr(attributeName);
	}

	@Override
	public final boolean hasPermission(String permission, String accessorName) throws DfException {
		return this.base.hasPermission(permission, accessorName);
	}

	@Override
	public final void insertBoolean(String attributeName, int valueIndex, boolean value) throws DfException {
		this.base.insertBoolean(attributeName, valueIndex, value);
	}

	@Override
	public final void insertContent(ByteArrayOutputStream content, int pageNumber) throws DfException {
		this.base.insertContent(content, pageNumber);
	}

	@Override
	public final void insertContentEx(ByteArrayOutputStream content, int pageNumber, boolean other) throws DfException {
		this.base.insertContentEx(content, pageNumber, other);
	}

	@Override
	public final void insertDouble(String attributeName, int valueIndex, double value) throws DfException {
		this.base.insertDouble(attributeName, valueIndex, value);
	}

	@Override
	public final void insertFile(String fileName, int pageNumber) throws DfException {
		this.base.insertFile(fileName, pageNumber);
	}

	@Override
	public final void insertFileEx(String fileName, int pageNumber, String otherFileName) throws DfException {
		this.base.insertFileEx(fileName, pageNumber, otherFileName);
	}

	@Override
	public final void insertId(String attributeName, int valueIndex, IDfId value) throws DfException {
		this.base.insertId(attributeName, valueIndex, value);
	}

	@Override
	public final void insertInt(String attributeName, int valueIndex, int value) throws DfException {
		this.base.insertInt(attributeName, valueIndex, value);
	}

	@Override
	public final IDfId insertPart(IDfId componentID, String versionLabel, IDfId beforeContainmentId, double orderNo,
		boolean orderNoFlag, boolean useNodeVerLabel, boolean followAssembly, int copyChild) throws DfException {
		return this.base.insertPart(componentID, versionLabel, beforeContainmentId, orderNo, orderNoFlag,
			useNodeVerLabel, followAssembly, copyChild);
	}

	@Override
	public final void insertString(String attributeName, int valueIndex, String value) throws DfException {
		this.base.insertString(attributeName, valueIndex, value);
	}

	@Override
	public final void insertTime(String attributeName, int valueIndex, IDfTime value) throws DfException {
		this.base.insertTime(attributeName, valueIndex, value);
	}

	@Override
	public final void insertValue(String attributeName, int valueIndex, IDfValue value) throws DfException {
		this.base.insertValue(attributeName, valueIndex, value);
	}

	@Override
	public final boolean isArchived() throws DfException {
		return this.base.isArchived();
	}

	@Override
	public final boolean isAttrRepeating(String attributeName) throws DfException {
		return this.base.isAttrRepeating(attributeName);
	}

	@Override
	public final boolean isCheckedOut() throws DfException {
		return this.base.isCheckedOut();
	}

	@Override
	public final boolean isCheckedOutBy(String userName) throws DfException {
		return this.base.isCheckedOutBy(userName);
	}

	@Override
	public final boolean isContentTransferCapabilityEnabled(String networkLocationIdentifier,
		IDfContentTransferCapability capability) throws DfException {
		return this.base.isContentTransferCapabilityEnabled(networkLocationIdentifier, capability);
	}

	@Override
	public final boolean isDeleted() throws DfException {
		return this.base.isDeleted();
	}

	@Override
	public final boolean isDirty() throws DfException {
		return this.base.isDirty();
	}

	@Override
	public final boolean isFrozen() throws DfException {
		return this.base.isFrozen();
	}

	@Override
	public final boolean isHidden() throws DfException {
		return this.base.isHidden();
	}

	@Override
	public final boolean isImmutable() throws DfException {
		return this.base.isImmutable();
	}

	@Override
	public final boolean isInstanceOf(String typeName) throws DfException {
		return this.base.isInstanceOf(typeName);
	}

	@Override
	public final boolean isLinkResolved() throws DfException {
		return this.base.isLinkResolved();
	}

	@Override
	public final boolean isNew() throws DfException {
		return this.base.isNew();
	}

	@Override
	public final boolean isNull(String attributeName) throws DfException {
		return this.base.isNull(attributeName);
	}

	@Override
	public final boolean isPublic() throws DfException {
		return this.base.isPublic();
	}

	@Override
	public final boolean isReference() throws DfException {
		return this.base.isReference();
	}

	@Override
	public final boolean isReplica() throws DfException {
		return this.base.isReplica();
	}

	@Override
	public final boolean isSuspended() throws DfException {
		return this.base.isSuspended();
	}

	@Override
	public final boolean isVirtualDocument() throws DfException {
		return this.base.isVirtualDocument();
	}

	@Override
	public final void link(String folderSpec) throws DfException {
		this.base.link(folderSpec);
	}

	@Override
	public final void lock() throws DfException {
		this.base.lock();
	}

	@Override
	public final void lockEx(boolean validateStamp) throws DfException {
		this.base.lockEx(validateStamp);
	}

	@Override
	public final void mark(String versionLabels) throws DfException {
		this.base.mark(versionLabels);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final void mount(String path) throws DfException {
		this.base.mount(path);
	}

	@Override
	public final String print(String printer, boolean printCover, boolean saveOutput, int numCopies,
		int startingContentPage, int endingContentPage) throws DfException {
		return this.base.print(printer, printCover, saveOutput, numCopies, startingContentPage, endingContentPage);
	}

	@Override
	public final void promote(String state, boolean override, boolean fTestOnly) throws DfException {
		this.base.promote(state, override, fTestOnly);
	}

	@Override
	public final void prune(boolean keepLabels) throws DfException {
		this.base.prune(keepLabels);
	}

	@Override
	public final IDfId queue(String queueOwner, String event, int priority, boolean sendMail, IDfTime dueDate,
		String message) throws DfException {
		return this.base.queue(queueOwner, event, priority, sendMail, dueDate, message);
	}

	@Override
	public final void refreshReference() throws DfException {
		this.base.refreshReference();
	}

	@Override
	public final void registerEvent(String message, String event, int priority, boolean sendMail) throws DfException {
		this.base.registerEvent(message, event, priority, sendMail);
	}

	@Override
	public final void remove(String attributeName, int valueIndex) throws DfException {
		this.base.remove(attributeName, valueIndex);
	}

	@Override
	public final void removeAll(String attributeName) throws DfException {
		this.base.removeAll(attributeName);
	}

	@Override
	public final void removeChildRelative(String relationTypeName, IDfId childId, String childLabel) throws DfException {
		this.base.removeChildRelative(relationTypeName, childId, childLabel);
	}

	@Override
	public final void removeContent(int pageNumber) throws DfException {
		this.base.removeContent(pageNumber);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final void removeNote(IDfId annotationId) throws DfException {
		this.base.removeNote(annotationId);
	}

	@Override
	public final void removeParentRelative(String relationTypeName, IDfId parentId, String childLabel)
		throws DfException {
		this.base.removeParentRelative(relationTypeName, parentId, childLabel);
	}

	@Override
	public final void removePart(IDfId containmentId, double orderNo, boolean orderNoFlag) throws DfException {
		this.base.removePart(containmentId, orderNo, orderNoFlag);
	}

	@Override
	public final void removeRendition(String formatName) throws DfException {
		this.base.removeRendition(formatName);
	}

	@Override
	public final void removeRenditionEx(String formatName, int pageNumber, boolean atomic) throws DfException {
		this.base.removeRenditionEx(formatName, pageNumber, atomic);
	}

	@Override
	public final void removeRenditionEx2(String formatName, int pageNumber, String pageModifier, boolean atomic)
		throws DfException {
		this.base.removeRenditionEx2(formatName, pageNumber, pageModifier, atomic);
	}

	@Override
	public final String resolveAlias(String scopeAlias) throws DfException {
		return this.base.resolveAlias(scopeAlias);
	}

	@Override
	public final void resume(String state, boolean toBase, boolean override, boolean fTestOnly) throws DfException {
		this.base.resume(state, toBase, override, fTestOnly);
	}

	@Override
	public final void revert() throws DfException {
		this.base.revert();
	}

	@Override
	public final void revertACL() throws DfException {
		this.base.revertACL();
	}

	@Override
	public final void revoke(String accessorName, String extendedPermission) throws DfException {
		this.base.revoke(accessorName, extendedPermission);
	}

	@Override
	public final void revokePermit(IDfPermit permit) throws DfException {
		this.base.revokePermit(permit);
	}

	@Override
	public final void save() throws DfException {
		this.base.save();
	}

	@Override
	public final IDfId saveAsNew(boolean shareContent) throws DfException {
		return this.base.saveAsNew(shareContent);
	}

	@Override
	public final void saveLock() throws DfException {
		this.base.saveLock();
	}

	@Override
	public final void scheduleDemote(String state, IDfTime schedule_date) throws DfException {
		this.base.scheduleDemote(state, schedule_date);
	}

	@Override
	public final void schedulePromote(String state, IDfTime scheduleDate, boolean override) throws DfException {
		this.base.schedulePromote(state, scheduleDate, override);
	}

	@Override
	public final void scheduleResume(String state, IDfTime scheduleDate, boolean toBase, boolean override)
		throws DfException {
		this.base.scheduleResume(state, scheduleDate, toBase, override);
	}

	@Override
	public final void scheduleSuspend(String state, IDfTime scheduleDate, boolean override) throws DfException {
		this.base.scheduleSuspend(state, scheduleDate, override);
	}

	@Override
	public final void setACL(IDfACL acl) throws DfException {
		this.base.setACL(acl);
	}

	@Override
	public final void setACLDomain(String aclDomain) throws DfException {
		this.base.setACLDomain(aclDomain);
	}

	@Override
	public final void setACLName(String ACLName) throws DfException {
		this.base.setACLName(ACLName);
	}

	@Override
	public final void setApplicationType(String type) throws DfException {
		this.base.setApplicationType(type);
	}

	@Override
	public final void setArchived(boolean archived) throws DfException {
		this.base.setArchived(archived);
	}

	@Override
	public final void setAuthors(int index, String author) throws DfException {
		this.base.setAuthors(index, author);
	}

	@Override
	public final void setBoolean(String attributeName, boolean value) throws DfException {
		this.base.setBoolean(attributeName, value);
	}

	@Override
	public final void setCompoundArchitecture(String compoundArchitecture) throws DfException {
		this.base.setCompoundArchitecture(compoundArchitecture);
	}

	@Override
	public final boolean setContent(ByteArrayOutputStream content) throws DfException {
		return this.base.setContent(content);
	}

	@Override
	public final boolean setContentEx(ByteArrayOutputStream content, String format, int pageNumber) throws DfException {
		return this.base.setContentEx(content, format, pageNumber);
	}

	@Override
	public final boolean setContentEx2(ByteArrayOutputStream content, String format, int pageNumber, boolean other)
		throws DfException {
		return this.base.setContentEx2(content, format, pageNumber, other);
	}

	@Override
	public final void setContentType(String contentType) throws DfException {
		this.base.setContentType(contentType);
	}

	@Override
	public final void setDouble(String attributeName, double value) throws DfException {
		this.base.setDouble(attributeName, value);
	}

	@Override
	public final void setDoubleContentAttribute(String name, double value, String formatName, int page,
		String pageModifier) throws DfException {
		this.base.setDoubleContentAttribute(name, value, formatName, page, pageModifier);
	}

	@Override
	public final void setFile(String fileName) throws DfException {
		this.base.setFile(fileName);
	}

	@Override
	public final void setFileEx(String fileName, String formatName, int pageNumber, String otherFile)
		throws DfException {
		this.base.setFileEx(fileName, formatName, pageNumber, otherFile);
	}

	@Override
	public final void setFullText(boolean fullText) throws DfException {
		this.base.setFullText(fullText);
	}

	@Override
	public final void setGroupName(String name) throws DfException {
		this.base.setGroupName(name);
	}

	@Override
	public final void setGroupPermit(int permit) throws DfException {
		this.base.setGroupPermit(permit);
	}

	@Override
	public final void setHidden(boolean isHidden) throws DfException {
		this.base.setHidden(isHidden);
	}

	@Override
	public final void setId(String attributeName, IDfId value) throws DfException {
		this.base.setId(attributeName, value);
	}

	@Override
	public final void setInt(String attributeName, int value) throws DfException {
		this.base.setInt(attributeName, value);
	}

	@Override
	public final void setIsVirtualDocument(boolean is_virtual_doc) throws DfException {
		this.base.setIsVirtualDocument(is_virtual_doc);
	}

	@Override
	public final void setKeywords(int index, String keyword) throws DfException {
		this.base.setKeywords(index, keyword);
	}

	@Override
	public final void setLinkResolved(boolean linkResolved) throws DfException {
		this.base.setLinkResolved(linkResolved);
	}

	@Override
	public final void setLogEntry(String logEntry) throws DfException {
		this.base.setLogEntry(logEntry);
	}

	@Override
	public final void setNull(String attributeName) throws DfException {
		this.base.setNull(attributeName);
	}

	@Override
	public final void setObjectName(String objectName) throws DfException {
		this.base.setObjectName(objectName);
	}

	@Override
	public final void setOwnerName(String ownerName) throws DfException {
		this.base.setOwnerName(ownerName);
	}

	@Override
	public final void setOwnerPermit(int permit) throws DfException {
		this.base.setOwnerPermit(permit);
	}

	@Override
	public final void setPartition(int partition) throws DfException {
		this.base.setPartition(partition);
	}

	@Override
	public final void setPath(String fileName, String formatName, int pageNumber, String otherFile) throws DfException {
		this.base.setPath(fileName, formatName, pageNumber, otherFile);
	}

	@Override
	public final void setRepeatingBoolean(String attributeName, int valueIndex, boolean value) throws DfException {
		this.base.setRepeatingBoolean(attributeName, valueIndex, value);
	}

	@Override
	public final void setRepeatingDouble(String attributeName, int valueIndex, double value) throws DfException {
		this.base.setRepeatingDouble(attributeName, valueIndex, value);
	}

	@Override
	public final void setRepeatingId(String attributeName, int valueIndex, IDfId value) throws DfException {
		this.base.setRepeatingId(attributeName, valueIndex, value);
	}

	@Override
	public final void setRepeatingInt(String attributeName, int valueIndex, int value) throws DfException {
		this.base.setRepeatingInt(attributeName, valueIndex, value);
	}

	@Override
	public final void setRepeatingString(String attributeName, int valueIndex, String value) throws DfException {
		this.base.setRepeatingString(attributeName, valueIndex, value);
	}

	@Override
	public final void setRepeatingTime(String attributeName, int valueIndex, IDfTime value) throws DfException {
		this.base.setRepeatingTime(attributeName, valueIndex, value);
	}

	@Override
	public final void setRepeatingValue(String attributeName, int valueIndex, IDfValue value) throws DfException {
		this.base.setRepeatingValue(attributeName, valueIndex, value);
	}

	@Override
	public final void setResolutionLabel(String label) throws DfException {
		this.base.setResolutionLabel(label);
	}

	@Override
	public final void setSessionManager(IDfSessionManager sessMgr) throws DfException {
		this.base.setSessionManager(sessMgr);
	}

	@Override
	public final void setSpecialApp(String specialApp) throws DfException {
		this.base.setSpecialApp(specialApp);
	}

	@Override
	public final void setStatus(String status) throws DfException {
		this.base.setStatus(status);
	}

	@Override
	public final void setStorageType(String type) throws DfException {
		this.base.setStorageType(type);
	}

	@Override
	public final void setString(String attributeName, String value) throws DfException {
		this.base.setString(attributeName, value);
	}

	@Override
	public final void setStringContentAttribute(String name, String value, String formatName, int page,
		String pageModifier) throws DfException {
		this.base.setStringContentAttribute(name, value, formatName, page, pageModifier);
	}

	@Override
	public final void setSubject(String subject) throws DfException {
		this.base.setSubject(subject);
	}

	@Override
	public final void setTime(String attributeName, IDfTime value) throws DfException {
		this.base.setTime(attributeName, value);
	}

	@Override
	public final void setTimeContentAttribute(String name, IDfTime value, String formatName, int page,
		String pageModifier) throws DfException {
		this.base.setTimeContentAttribute(name, value, formatName, page, pageModifier);
	}

	@Override
	public final void setTitle(String title) throws DfException {
		this.base.setTitle(title);
	}

	@Override
	public final void setValue(String attributeName, IDfValue value) throws DfException {
		this.base.setValue(attributeName, value);
	}

	@Override
	public final void setWorldPermit(int permit) throws DfException {
		this.base.setWorldPermit(permit);
	}

	@Override
	public final void signoff(String user, String os_password, String reason) throws DfException {
		this.base.signoff(user, os_password, reason);
	}

	@Override
	public final void suspend(String state, boolean override, boolean fTestOnly) throws DfException {
		this.base.suspend(state, override, fTestOnly);
	}

	@Override
	public final void truncate(String attributeName, int valueIndex) throws DfException {
		this.base.truncate(attributeName, valueIndex);
	}

	@Override
	public final void unRegisterEvent(String event) throws DfException {
		this.base.unRegisterEvent(event);
	}

	@Override
	public final void unRegisterEventEx(String event, String userName) throws DfException {
		this.base.unRegisterEventEx(event, userName);
	}

	@Override
	public final void unfreeze(boolean thawComponents) throws DfException {
		this.base.unfreeze(thawComponents);
	}

	@Override
	public final void unlink(String folderSpec) throws DfException {
		this.base.unlink(folderSpec);
	}

	@Override
	public final void unmark(String versionLabels) throws DfException {
		this.base.unmark(versionLabels);
	}

	@Override
	public final void updatePart(IDfId containmentId, String versionLabel, double orderNumber, boolean useNodeVerLabel,
		boolean followAssembly, int copyChild) throws DfException {
		this.base.updatePart(containmentId, versionLabel, orderNumber, useNodeVerLabel, followAssembly, copyChild);
	}

	@Override
	public final void updatePartEx(IDfId containmentId, String versionLabel, double orderNumber,
		boolean useNodeVerLabel, boolean followAssembly, int copyChild, String containType, String containDesc)
		throws DfException {
		this.base.updatePartEx(containmentId, versionLabel, orderNumber, useNodeVerLabel, followAssembly, copyChild,
			containType, containDesc);
	}

	@Override
	public final void useACL(String aclType) throws DfException {
		this.base.useACL(aclType);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final void validateAllRules(int stopAfterNumOfErrors) throws DfException {
		this.base.validateAllRules(stopAfterNumOfErrors);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final void validateAttrRules(String attrName, int stopAfterNumOfErrors) throws DfException {
		this.base.validateAttrRules(attrName, stopAfterNumOfErrors);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final void validateAttrRulesWithValue(String attrName, String value, int stopAfterNumOfErrors)
		throws DfException {
		this.base.validateAttrRulesWithValue(attrName, value, stopAfterNumOfErrors);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final void validateAttrRulesWithValues(String attrName, IDfList valueList, int stopAfterNumOfErrors)
		throws DfException {
		this.base.validateAttrRulesWithValues(attrName, valueList, stopAfterNumOfErrors);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final void validateObjRules(int stopAfterNumOfErrors) throws DfException {
		this.base.validateObjRules(stopAfterNumOfErrors);
	}

	@SuppressWarnings("deprecation")
	@Override
	public final void validateObjRulesWithValues(IDfList attrNameList, IDfList valueListList, int stopAfterNumOfErrors)
		throws DfException {
		this.base.validateObjRulesWithValues(attrNameList, valueListList, stopAfterNumOfErrors);
	}

	@Override
	public final void verifyESignature() throws DfException {
		this.base.verifyESignature();
	}
}